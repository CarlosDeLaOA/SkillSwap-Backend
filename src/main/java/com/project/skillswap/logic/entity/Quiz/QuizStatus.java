package com.project.skillswap.logic.entity.Quiz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estados posibles de un cuestionario
 */
public enum QuizStatus {
    IN_PROGRESS,
    SUBMITTED,
    GRADED
}