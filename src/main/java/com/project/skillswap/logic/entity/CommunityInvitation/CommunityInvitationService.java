package com.project.skillswap.logic.entity.CommunityInvitation;

import com.project.skillswap.logic.entity.CommunityMember.CommunityMember;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMemberRepository;
import com.project.skillswap.logic.entity.CommunityMember.MemberRole;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio que gestiona la lógica de invitaciones a comunidades de aprendizaje.
 */
@Service
public class CommunityInvitationService {

    //#region Constants
    private static final int TOKEN_EXPIRATION_HOURS = 48;
    private static final int MAX_COMMUNITY_MEMBERS = 10;
    //#endregion

    //#region Dependencies
    private final CommunityInvitationTokenRepository tokenRepository;
    private final LearningCommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final PersonRepository personRepository;
    private final LearnerRepository learnerRepository;
    private final EmailCommunityService emailCommunityService;

    public CommunityInvitationService(CommunityInvitationTokenRepository tokenRepository,
                                      LearningCommunityRepository communityRepository,
                                      CommunityMemberRepository memberRepository,
                                      PersonRepository personRepository,
                                      LearnerRepository learnerRepository,
                                      EmailCommunityService emailCommunityService) {
        this.tokenRepository = tokenRepository;
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
        this.personRepository = personRepository;
        this.learnerRepository = learnerRepository;
        this.emailCommunityService = emailCommunityService;
    }
    //#endregion

    //#region Public Methods
    /**
     * Crea una nueva comunidad y envía invitaciones a los miembros.
     *
     * @param name nombre de la comunidad
     * @param description descripción de la comunidad
     * @param creatorId ID del creador (debe ser un learner)
     * @param memberEmails lista de emails de los miembros a invitar
     * @return resultado de la creación
     */
    @Transactional
    public CreateCommunityResult createCommunityWithInvitations(String name, String description,
                                                                Long creatorId, List<String> memberEmails) {
        Optional<Learner> optionalCreator = learnerRepository.findById(creatorId);
        if (optionalCreator.isEmpty()) {
            return new CreateCommunityResult(false, "El creador debe ser un aprendiz (Learner)", null, null);
        }

        Learner creator = optionalCreator.get();

        // Validar que el creador no esté ya en otra comunidad activa
        List<CommunityMember> creatorMemberships = memberRepository.findByLearnerIdAndActiveTrue(creator.getId());
        if (!creatorMemberships.isEmpty()) {
            return new CreateCommunityResult(false,
                    "Ya eres miembro de una comunidad. Solo puedes estar en una comunidad a la vez.",
                    null, null);
        }

        if (memberEmails == null || memberEmails.isEmpty()) {
            return new CreateCommunityResult(false, "Debes invitar al menos un miembro", null, null);
        }

        if (memberEmails.size() > MAX_COMMUNITY_MEMBERS - 1) {
            return new CreateCommunityResult(false,
                    "Máximo " + (MAX_COMMUNITY_MEMBERS - 1) + " invitaciones permitidas", null, null);
        }

        Set<String> uniqueEmails = new HashSet<>(memberEmails);
        uniqueEmails.remove(creator.getPerson().getEmail());

        if (uniqueEmails.isEmpty()) {
            return new CreateCommunityResult(false, "Debes invitar a otros miembros además de ti mismo", null, null);
        }

        LearningCommunity community = new LearningCommunity();
        community.setName(name);
        community.setDescription(description);
        community.setCreator(creator);
        community.setMaxMembers(MAX_COMMUNITY_MEMBERS);
        community.setInvitationCode(generateUniqueInvitationCode());
        community.setActive(true);
        community = communityRepository.save(community);

        CommunityMember creatorMember = new CommunityMember();
        creatorMember.setLearningCommunity(community);
        creatorMember.setLearner(creator);
        creatorMember.setRole(MemberRole.CREATOR);
        creatorMember.setActive(true);
        memberRepository.save(creatorMember);

        List<String> successfulInvitations = new ArrayList<>();
        List<String> failedInvitations = new ArrayList<>();

        for (String email : uniqueEmails) {
            try {
                InvitationResult result = createAndSendInvitation(community, email, creator.getPerson().getFullName());
                if (result.isSuccess()) {
                    successfulInvitations.add(email);
                } else {
                    failedInvitations.add(email + " (" + result.getMessage() + ")");
                }
            } catch (Exception e) {
                failedInvitations.add(email + " (Error al enviar)");
            }
        }

        String message = "Comunidad creada exitosamente. ";
        if (!successfulInvitations.isEmpty()) {
            message += successfulInvitations.size() + " invitaciones enviadas. ";
        }
        if (!failedInvitations.isEmpty()) {
            message += failedInvitations.size() + " invitaciones fallaron.";
        }

        return new CreateCommunityResult(true, message, community.getId(),
                new InvitationsSummary(successfulInvitations, failedInvitations));
    }

    /**
     * Acepta una invitación a una comunidad.
     *
     * @param token el token de invitación
     * @param personId ID de la persona que acepta
     * @return resultado de la aceptación
     */
    @Transactional
    public AcceptInvitationResult acceptInvitation(String token, Long personId) {
        Optional<CommunityInvitationToken> optionalToken = tokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            return new AcceptInvitationResult(false, "Invitación inválida o no encontrada",
                    InvitationStatus.INVALID_TOKEN);
        }

        CommunityInvitationToken invitationToken = optionalToken.get();

        if (!invitationToken.getActive()) {
            return new AcceptInvitationResult(false, "Esta invitación ya no es válida",
                    InvitationStatus.INVALID_TOKEN);
        }

        if (invitationToken.isAccepted()) {
            return new AcceptInvitationResult(false, "Esta invitación ya fue aceptada",
                    InvitationStatus.ALREADY_ACCEPTED);
        }

        if (invitationToken.isExpired()) {
            return new AcceptInvitationResult(false,
                    "Esta invitación ha expirado. Solicita al administrador que te envíe una nueva invitación.",
                    InvitationStatus.EXPIRED_TOKEN);
        }

        Optional<Person> optionalPerson = personRepository.findById(personId);
        if (optionalPerson.isEmpty()) {
            return new AcceptInvitationResult(false, "Usuario no encontrado", InvitationStatus.USER_NOT_FOUND);
        }

        Person person = optionalPerson.get();

        if (!person.getEmail().equalsIgnoreCase(invitationToken.getInviteeEmail())) {
            return new AcceptInvitationResult(false,
                    "Esta invitación fue enviada a otro correo electrónico", InvitationStatus.EMAIL_MISMATCH);
        }

        if (person.getLearner() == null) {
            return new AcceptInvitationResult(false,
                    "Debes ser un aprendiz (Learner) para unirte a una comunidad", InvitationStatus.NOT_LEARNER);
        }

        LearningCommunity community = invitationToken.getCommunity();
        Learner learner = person.getLearner();

        // Validar que el usuario no esté ya en otra comunidad activa
        List<CommunityMember> existingMemberships = memberRepository.findByLearnerIdAndActiveTrue(learner.getId());
        if (!existingMemberships.isEmpty()) {
            return new AcceptInvitationResult(false,
                    "Ya eres miembro de otra comunidad. Solo puedes estar en una comunidad a la vez.",
                    InvitationStatus.ALREADY_IN_COMMUNITY);
        }

        long currentMembers = memberRepository.countActiveMembersByCommunityId(community.getId());
        if (currentMembers >= community.getMaxMembers()) {
            return new AcceptInvitationResult(false, "La comunidad ha alcanzado el límite de miembros",
                    InvitationStatus.COMMUNITY_FULL);
        }

        Optional<CommunityMember> existingMember = memberRepository
                .findActiveMembersByCommunityId(community.getId())
                .stream()
                .filter(m -> m.getLearner().getId().equals(learner.getId()))
                .findFirst();

        if (existingMember.isPresent()) {
            return new AcceptInvitationResult(false, "Ya eres miembro de esta comunidad",
                    InvitationStatus.ALREADY_MEMBER);
        }

        CommunityMember newMember = new CommunityMember();
        newMember.setLearningCommunity(community);
        newMember.setLearner(learner);
        newMember.setRole(MemberRole.MEMBER);
        newMember.setActive(true);
        memberRepository.save(newMember);

        invitationToken.setAcceptedAt(LocalDateTime.now());
        invitationToken.setActive(false);
        tokenRepository.save(invitationToken);

        // Enviar correo de bienvenida
        try {
            emailCommunityService.sendWelcomeToCommunityEmail(
                    person.getEmail(),
                    person.getFullName(),
                    community.getName()
            );
        } catch (MessagingException e) {
            System.err.println("Error enviando correo de bienvenida: " + e.getMessage());
        }

        return new AcceptInvitationResult(true,
                "Te has unido exitosamente a " + community.getName(), InvitationStatus.SUCCESS);
    }

    /**
     * Limpia tokens expirados de la base de datos.
     */
    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
    //#endregion

    //#region Private Methods
    /**
     * Crea y envía una invitación a un miembro.
     *
     * @param community la comunidad
     * @param email email del invitado
     * @param creatorName nombre del creador
     * @return resultado de la invitación
     */
    private InvitationResult createAndSendInvitation(LearningCommunity community, String email,
                                                     String creatorName) {
        Optional<Person> optionalPerson = personRepository.findByEmail(email);

        String inviteeName = email;
        if (optionalPerson.isPresent()) {
            Person person = optionalPerson.get();
            inviteeName = person.getFullName();

            if (person.getLearner() == null) {
                return new InvitationResult(false, "El usuario no es un aprendiz (Learner)");
            }

            // Verificar si el usuario ya está en otra comunidad
            List<CommunityMember> existingMemberships = memberRepository.findByLearnerIdAndActiveTrue(
                    person.getLearner().getId());
            if (!existingMemberships.isEmpty()) {
                return new InvitationResult(false, "Ya es miembro de otra comunidad");
            }

            boolean isAlreadyMember = memberRepository.findActiveMembersByCommunityId(community.getId())
                    .stream()
                    .anyMatch(m -> m.getLearner().getPerson().getEmail().equalsIgnoreCase(email));

            if (isAlreadyMember) {
                return new InvitationResult(false, "Ya es miembro de la comunidad");
            }
        }

        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

        CommunityInvitationToken invitationToken = new CommunityInvitationToken(
                token, community, email, inviteeName, expiresAt
        );
        tokenRepository.save(invitationToken);

        try {
            emailCommunityService.sendCommunityInvitation(
                    email, inviteeName, community.getName(), creatorName, token
            );
            return new InvitationResult(true, "Invitación enviada");
        } catch (MessagingException e) {
            return new InvitationResult(false, "Error al enviar el correo");
        }
    }

    /**
     * Genera un token seguro y único.
     *
     * @return token generado
     */
    private String generateSecureToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    /**
     * Genera un código único de invitación para la comunidad.
     *
     * @return código generado
     */
    private String generateUniqueInvitationCode() {
        String code;
        int attempts = 0;
        final int MAX_ATTEMPTS = 10;

        do {
            code = "COM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            attempts++;
        } while (communityRepository.existsByInvitationCode(code) && attempts < MAX_ATTEMPTS);

        if (attempts >= MAX_ATTEMPTS) {
            // Si después de 10 intentos no se genera uno único, usar timestamp adicional
            code = "COM-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }

        return code;
    }
    //#endregion

    //#region Inner Classes
    /**
     * Clase que representa el resultado de crear una comunidad.
     */
    public static class CreateCommunityResult {
        private final boolean success;
        private final String message;
        private final Long communityId;
        private final InvitationsSummary invitationsSummary;

        public CreateCommunityResult(boolean success, String message, Long communityId,
                                     InvitationsSummary invitationsSummary) {
            this.success = success;
            this.message = message;
            this.communityId = communityId;
            this.invitationsSummary = invitationsSummary;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Long getCommunityId() {
            return communityId;
        }

        public InvitationsSummary getInvitationsSummary() {
            return invitationsSummary;
        }
    }

    /**
     * Clase que representa el resumen de invitaciones.
     */
    public static class InvitationsSummary {
        private final List<String> successfulInvitations;
        private final List<String> failedInvitations;

        public InvitationsSummary(List<String> successfulInvitations, List<String> failedInvitations) {
            this.successfulInvitations = successfulInvitations;
            this.failedInvitations = failedInvitations;
        }

        public List<String> getSuccessfulInvitations() {
            return successfulInvitations;
        }

        public List<String> getFailedInvitations() {
            return failedInvitations;
        }
    }

    /**
     * Clase que representa el resultado de una invitación individual.
     */
    private static class InvitationResult {
        private final boolean success;
        private final String message;

        public InvitationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Clase que representa el resultado de aceptar una invitación.
     */
    public static class AcceptInvitationResult {
        private final boolean success;
        private final String message;
        private final InvitationStatus status;

        public AcceptInvitationResult(boolean success, String message, InvitationStatus status) {
            this.success = success;
            this.message = message;
            this.status = status;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public InvitationStatus getStatus() {
            return status;
        }
    }

    /**
     * Enum con los estados posibles de invitación.
     */
    public enum InvitationStatus {
        SUCCESS,
        INVALID_TOKEN,
        EXPIRED_TOKEN,
        ALREADY_ACCEPTED,
        ALREADY_MEMBER,
        ALREADY_IN_COMMUNITY,
        COMMUNITY_FULL,
        USER_NOT_FOUND,
        EMAIL_MISMATCH,
        NOT_LEARNER
    }
    //#endregion
}