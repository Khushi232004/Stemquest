package com.khushi.stemquest.repository;

import com.khushi.stemquest.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTopicAndGrade(String topic, Integer grade);
    List<Question> findByGrade(Integer grade);
    boolean existsByTextEnAndGrade(String textEn, Integer grade);
}
