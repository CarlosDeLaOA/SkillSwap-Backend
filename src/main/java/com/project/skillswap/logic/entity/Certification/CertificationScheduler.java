package com.project.skillswap.logic.entity.Certification;

import com.project.skillswap.logic.entity.Credential.CredentialRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Skill.Skill;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler que revisa cada minuto si hay learners que han alcanzado 10 credenciales
 * y genera automáticamente sus certificados
 */
@Component
public class CertificationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(CertificationScheduler.class);

    private static final int CREDENTIALS_REQUIRED = 10;

    @Autowired
    private LearnerRepository learnerRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private CertificationService certificationService;

    /**
     * Se ejecuta cada 1 minuto para verificar certificaciones pendientes
     * Cron: 0 * * * * * = cada minuto en el segundo 0
     */
    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void checkPendingCertifications() {
        logger.info("╔════════════════════════════════════════════════════════════╗");
        logger.info("║   VERIFICACIÓN DE CERTIFICACIONES PROGRAMADA            ║");
        logger.info("╚════════════════════════════════════════════════════════════╝");

        try {
            List<Learner> allLearners = learnerRepository.findAll();
            logger.info("→ Total de learners en el sistema: {}", allLearners.size());

            int certificatesGenerated = 0;
            int learnersProcessed = 0;

            for (Learner learner : allLearners) {
                try {
                    int generated = checkLearnerCertifications(learner);
                    certificatesGenerated += generated;
                    learnersProcessed++;
                } catch (Exception e) {
                    logger.error(" Error procesando learner {}: {}",
                            learner.getId(), e.getMessage());
                }
            }

            logger.info("");
            logger.info("╔════════════════════════════════════════════════════════════╗");
            logger.info("║   VERIFICACIÓN COMPLETADA                               ║");
            logger.info("╠════════════════════════════════════════════════════════════╣");
            logger.info("║  Learners procesados: {:<35} ║", learnersProcessed);
            logger.info("║  Certificados generados: {:<32} ║", certificatesGenerated);
            logger.info("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            logger.error("╔════════════════════════════════════════════════════════════╗");
            logger.error("║   ERROR EN VERIFICACIÓN                                 ║");
            logger.error("╠════════════════════════════════════════════════════════════╣");
            logger.error("║  Error: {}", e.getMessage());
            logger.error("╚════════════════════════════════════════════════════════════╝");
            logger.error("Stack trace:", e);
        }
    }

    /**
     * Verifica todas las skills del learner y genera certificados si corresponde
     */
    private int checkLearnerCertifications(Learner learner) {
        int certificatesGenerated = 0;

        try {
            // Obtener todas las skills únicas en las que el learner tiene credenciales
            List<Skill> skillsWithCredentials = credentialRepository.findDistinctSkillsByLearner(learner);

            logger.debug("  → Learner {} ({}) - Skills con credenciales: {}",
                    learner.getId(),
                    learner.getPerson().getEmail(),
                    skillsWithCredentials.size());

            for (Skill skill : skillsWithCredentials) {
                try {
                    // Contar credenciales para esta skill
                    long credentialCount = credentialRepository.countByLearnerAndSkill(learner, skill);

                    if (credentialCount >= CREDENTIALS_REQUIRED) {
                        logger.info(" Learner {} tiene {} credenciales en skill '{}' (ID: {})",
                                learner.getId(),
                                credentialCount,
                                skill.getName(),
                                skill.getId());

                        // Verificar si ya tiene certificado
                        boolean alreadyCertified = certificationService.existsByLearnerAndSkill(learner, skill);

                        if (!alreadyCertified) {
                            logger.info("     Generando certificado para Learner {} en skill '{}'...",
                                    learner.getId(),
                                    skill.getName());

                            certificationService.checkAndGenerateCertificate(learner, skill);
                            certificatesGenerated++;

                            logger.info("     Certificado generado exitosamente");
                            logger.info("     Email enviado a: {}", learner.getPerson().getEmail());
                        } else {
                            logger.debug(" Ya tiene certificado en skill '{}'", skill.getName());
                        }
                    }
                } catch (Exception e) {
                    logger.error("     Error procesando skill {}: {}", skill.getName(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("   Error obteniendo skills del learner {}: {}", learner.getId(), e.getMessage());
        }

        return certificatesGenerated;
    }
}