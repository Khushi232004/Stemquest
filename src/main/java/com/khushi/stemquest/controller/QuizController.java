package com.khushi.stemquest.controller;

import com.khushi.stemquest.model.*;
import com.khushi.stemquest.repository.*;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class QuizController {
    private final QuestionRepository repo;
    private final UserRepository users;

    public QuizController(QuestionRepository repo, UserRepository users) {
        this.repo = repo; this.users = users;
    }

    // A student always gets their own grade. A teacher can choose with ?grade=
    private int gradeFor(Principal p, Integer param) {
        User u = users.findByStudentCode(p.getName()).orElse(null);
        if (u != null && "STUDENT".equals(u.role) && u.grade != null) return u.grade;
        return (param != null && (param == 6 || param == 7)) ? param : 6;
    }

    @GetMapping("/questions")
    public List<Question> questions(@RequestParam String topic,
                                    @RequestParam(required = false) Integer grade, Principal p) {
        return repo.findByTopicAndGrade(topic, gradeFor(p, grade));
    }

    @GetMapping("/topics")
    public List<Map<String, String>> topics(@RequestParam(required = false) Integer grade, Principal p) {
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, String>> out = new ArrayList<>();
        for (Question q : repo.findByGrade(gradeFor(p, grade))) {
            if (seen.add(q.topic)) out.add(Map.of("subject", q.subject, "topic", q.topic));
        }
        return out;
    }
}
