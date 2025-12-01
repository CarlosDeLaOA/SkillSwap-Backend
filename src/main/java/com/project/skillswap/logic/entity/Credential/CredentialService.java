package com.project.skillswap.logic.entity.Credential;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Certification.CertificationService;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar las credenciales
 */
@Service
public class CredentialService {

    //#region Dependencies
    private final CredentialRepository credentialRepository;
    private final CertificationService certificationService;
    //#endregion

    //#region Constructor
    @Autowired
    public CredentialService(
            CredentialRepository credentialRepository,
            @Lazy CertificationService certificationService
    ) {
        this.credentialRepository = credentialRepository;
        this.certificationService = certificationService;
    }
    //#endregion

    //#region Public Methods
    /**
     * Obtiene todas las credenciales de los miembros de una comunidad
     *
     * @param communityId ID de la comunidad
     * @return lista de credenciales en formato DTO
     */
    public List<CredentialDTO> getCommunityCredentials(Long communityId) {
        List<Credential> credentials = credentialRepository.findCredentialsByCommunityId(communityId);

        return credentials.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Registra una credencial cuando un learner aprueba un quiz
     *
     * @param quiz el cuestionario aprobado
     * @throws Exception si hay error al generar el certificado
     */
    @Transactional
    public void registerCredentialFromQuiz(Quiz quiz) throws Exception {
        if (!Boolean.TRUE.equals(quiz.getPassed())) {
            return;
        }

        boolean alreadyExists = credentialRepository.existsByQuiz(quiz);
        if (alreadyExists) {
            return;
        }

        Credential credential = new Credential();
        credential.setLearner(quiz.getLearner());
        credential.setSkill(quiz.getSkill());
        credential.setLearningSession(quiz.getLearningSession());
        credential.setQuiz(quiz);

        double percentage = quiz.getPercentageScore();
        credential.setPercentageAchieved(BigDecimal.valueOf(percentage));

        credentialRepository.save(credential);

        certificationService.checkAndGenerateCertificate(quiz.getLearner(), quiz.getSkill());
    }
    //#endregion

    //#region Private Methods
    /**
     * Convierte una Credential a DTO
     *
     * @param credential la credencial a convertir
     * @return el DTO de la credencial
     */
    private CredentialDTO convertToDTO(Credential credential) {
        CredentialDTO dto = new CredentialDTO();
        dto.setId(credential.getId());
        dto.setPercentageAchieved(credential.getPercentageAchieved());
        dto.setBadgeUrl(credential.getBadgeUrl());
        dto.setObtainedDate(credential.getObtainedDate());

        CredentialDTO.LearnerDTO learnerDTO = new CredentialDTO.LearnerDTO();
        learnerDTO.setId(credential.getLearner().getId());

        CredentialDTO.PersonDTO personDTO = new CredentialDTO.PersonDTO();
        personDTO.setId(credential.getLearner().getPerson().getId());
        personDTO.setFullName(credential.getLearner().getPerson().getFullName());
        learnerDTO.setPerson(personDTO);
        dto.setLearner(learnerDTO);

        CredentialDTO.SkillDTO skillDTO = new CredentialDTO.SkillDTO();
        skillDTO.setId(credential.getSkill().getId());
        skillDTO.setName(credential.getSkill().getName());
        skillDTO.setDescription(credential.getSkill().getDescription());
        skillDTO.setActive(credential.getSkill().getActive());

        CredentialDTO.KnowledgeAreaDTO kaDTO = new CredentialDTO.KnowledgeAreaDTO();
        kaDTO.setId(credential.getSkill().getKnowledgeArea().getId());
        kaDTO.setName(credential.getSkill().getKnowledgeArea().getName());
        kaDTO.setDescription(credential.getSkill().getKnowledgeArea().getDescription());
        kaDTO.setIconUrl(credential.getSkill().getKnowledgeArea().getIconUrl());
        kaDTO.setActive(credential.getSkill().getKnowledgeArea().getActive());
        skillDTO.setKnowledgeArea(kaDTO);

        dto.setSkill(skillDTO);

        return dto;
    }
    //#endregion
}