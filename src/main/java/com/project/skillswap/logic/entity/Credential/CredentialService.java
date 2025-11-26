package com.project.skillswap.logic.entity.Credential;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar las credenciales
 */
@Service
public class CredentialService {

    //<editor-fold desc="Dependencies">
    @Autowired
    private CredentialRepository credentialRepository;
    //</editor-fold>

    //<editor-fold desc="Public Methods">

    /**
     * Obtiene todas las credenciales de los miembros de una comunidad
     */

    public List<CredentialDTO> getCommunityCredentials(Long communityId) {
        List<Credential> credentials = credentialRepository.findCredentialsByCommunityId(communityId);

        return credentials.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private CredentialDTO convertToDTO(Credential credential) {
        CredentialDTO dto = new CredentialDTO();
        dto.setId(credential.getId());
        dto.setPercentageAchieved(credential.getPercentageAchieved());
        dto.setBadgeUrl(credential.getBadgeUrl());
        dto.setObtainedDate(credential.getObtainedDate());

        // Learner
        CredentialDTO.LearnerDTO learnerDTO = new CredentialDTO.LearnerDTO();
        learnerDTO.setId(credential.getLearner().getId());

        CredentialDTO.PersonDTO personDTO = new CredentialDTO.PersonDTO();
        personDTO.setId(credential.getLearner().getPerson().getId());
        personDTO.setFullName(credential.getLearner().getPerson().getFullName());
        learnerDTO.setPerson(personDTO);
        dto.setLearner(learnerDTO);

        // Skill
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

    //</editor-fold>
}