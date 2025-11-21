package com.project.skillswap.rest.credential;

import com.project.skillswap.logic.entity.Credential.Credential;
import com.project.skillswap.logic.entity.Credential.CredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar las credenciales de la comunidad
 *
 * @author Byte&Bite Team
 * @version 1.0
 */
@RestController
@RequestMapping("/communities")
@CrossOrigin(origins = "*")
public class CredentialRestController {

    //<editor-fold desc="Dependencies">
    @Autowired
    private CredentialService credentialService;
    //</editor-fold>

    //<editor-fold desc="Endpoints">

    /**
     * Obtiene todas las credenciales de los miembros de una comunidad
     *
     * GET /communities/{communityId}/credentials
     *
     * @param communityId ID de la comunidad
     * @return ResponseEntity con la lista de credenciales
     */
    @GetMapping("/{communityId}/credentials")
    public ResponseEntity<List<Credential>> getCommunityCredentials(
            @PathVariable Long communityId) {

        List<Credential> credentials = credentialService.getCommunityCredentials(communityId);
        return ResponseEntity.ok(credentials);
    }

    //</editor-fold>
}
