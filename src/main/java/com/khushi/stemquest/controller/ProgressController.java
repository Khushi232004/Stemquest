package com.khushi.stemquest.controller;

import com.khushi.stemquest.model.*;
import com.khushi.stemquest.repository.*;
import com.khushi.stemquest.service.GamificationService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {
    private final UserRepository userRepo;
    private final ProgressRepository progressRepo;
    private final GamificationService gamification;

    public ProgressController(UserRepository userRepo, ProgressRepository progressRepo,
                              GamificationService gamification) {
        this.userRepo = userRepo; this.progressRepo = progressRepo; this.gamification = gamification;
    }

    private int num(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return (o instanceof Number n) ? n.intValue() : 0;
    }

    @PostMapping("/sync")
    public Map<String, Object> sync(@RequestBody List<Map<String, Object>> items, Principal principal) {
        User u = userRepo.findByStudentCode(principal.getName()).orElseThrow();
        int saved = 0;
        for (Map<String, Object> m : items.stream().limit(50).toList()) {
            String cid = String.valueOf(m.get("clientId"));
            if (cid.length() > 60 || progressRepo.existsByClientId(cid)) continue;

            // Never trust the browser: keep the numbers inside sensible limits
            int total = Math.max(0, Math.min(num(m, "total"), 20));
            int score = Math.max(0, Math.min(num(m, "score"), total));
            int time = Math.max(0, Math.min(num(m, "timeTaken"), 3600));
            String topic = String.valueOf(m.get("topic"));
            if (topic.length() > 50) topic = topic.substring(0, 50);

            Progress p = new Progress();
            p.userId = u.id;
            p.clientId = cid;
            p.topic = topic;
            p.game = "MATCH".equals(m.get("game")) ? "MATCH" : "QUIZ";
            p.score = score;
            p.total = total;
            p.timeTaken = time;
            p.playedAt = LocalDateTime.now();
            progressRepo.save(p);

            gamification.record(u, score, total);
            saved++;
        }
        return Map.of("saved", saved, "xp", u.xp,
                "level", GamificationService.levelFor(u.xp), "streak", u.streak);
    }
}
