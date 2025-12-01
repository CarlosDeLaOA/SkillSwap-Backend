
package com.project.skillswap.logic.entity.Question;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Quiz.Quiz;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Table(name = "question", indexes = {
        @Index(name = "idx_question_quiz", columnList = "quiz_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_question_quiz_number", columnNames = {"quiz_id", "number"})
})
@Entity
public class Question {
    private static final Logger logger = LoggerFactory.getLogger(Question.class);

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", referencedColumnName = "id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "correct_answer", length = 255)
    private String correctAnswer;

    @Column(name = "user_answer", length = 255)
    private String userAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Question() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    //</editor-fold>
}