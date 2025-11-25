package com.project.skillswap.rest.LearningCommunity;

import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.auth.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller para gestión de comunidades de aprendizaje
 */
@RestController
@RequestMapping("/api/communities")
@CrossOrigin(origins = "http://localhost:4200")
public class LearningCommunityRestController {

    @Autowired
    private LearningCommunityRepository learningCommunityRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private JwtService jwtService;

    /**
     * GET /api/communities/my-communities
     * Obtiene todas las comunidades del usuario autenticado
     */

}