
package com.project.skillswap.rest.credential;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Credential.CredentialDTO;
import com.project.skillswap.logic.entity.Credential.CredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar las credenciales de la comunidad
 */
@RestController
@RequestMapping("/communities")
@CrossOrigin(origins = "*")
public class CredentialRestController {
    private static final Logger logger = LoggerFactory.getLogger(CredentialRestController.class);

    @Autowired
    private CredentialService credentialService;

    /**
     * Obtiene todas las credenciales de los miembros de una comunidad
     * GET /communities/{id}/credentials
     */
    @GetMapping("/{id}/credentials")
    public ResponseEntity<List<CredentialDTO>> getCommunityCredentials(@PathVariable Long id) {
        List<CredentialDTO> credentials = credentialService.getCommunityCredentials(id);
        return ResponseEntity.ok(credentials);
    }

}