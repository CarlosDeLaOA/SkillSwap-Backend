package com.project.skillswap.logic.entity.Credential;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para gestionar las credenciales
 *
 * @author Byte&Bite Team
 * @version 1.0
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
     *
     * @param communityId ID de la comunidad
     * @return Lista de credenciales
     */
    @Transactional(readOnly = true)
    public List<Credential> getCommunityCredentials(Long communityId) {
        return credentialRepository.findCredentialsByCommunityId(communityId);
    }

    //</editor-fold>
}