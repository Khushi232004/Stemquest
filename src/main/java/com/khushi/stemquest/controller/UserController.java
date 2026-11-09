package com.khushi.stemquest.controller;

import com.khushi.stemquest.model.Progress;
import com.khushi.stemquest.model.User;
import com.khushi.stemquest.repository.ProgressRepository;
import com.khushi.stemquest.repository.UserRepository;
import com.khushi.stemquest.service.GamificationService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserRepository repo;
    private final ProgressRepository progressRepo;

    public UserController(UserRepository repo, ProgressRepository progressRepo) {
        this.repo = repo; this.progressRepo = progressRepo;
    }

    @GetMapping("/users/me")
    public Map<String, Object> me(Principal principal) {
        User u = repo.findByStudentCode(principal.getName()).orElseThrow();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id);
        m.put("name", u.name);
        m.put("studentCode", u.studentCode);
        m.put("role", u.role);
        m.put("grade", u.grade);
        m.put("avatar", u.avatar);
        m.put("xp", u.xp);
        m.put("level", GamificationService.levelFor(u.xp));
        m.put("streak", u.streak);
        m.put("badges", GamificationService.badgeList(u));
        return m;
    }

    private static final List<String> AVATARS = List.of("🌙", "⭐", "🌊", "🍃", "🔭", "🪐");

    @PostMapping("/users/me/avatar")
    public Map<String, String> setAvatar(@RequestBody Map<String, String> b, Principal principal) {
        User u = repo.findByStudentCode(principal.getName()).orElseThrow();
        String a = b.get("avatar");
        if (a != null && AVATARS.contains(a)) { u.avatar = a; repo.save(u); }
        return Map.of("message", "ok");
    }

    @GetMapping("/users/me/history")
    public List<Map<String, Object>> history(Principal principal) {
        User u = repo.findByStudentCode(principal.getName()).orElseThrow();
        List<Progress> ps = new ArrayList<>(progressRepo.findByUserId(u.id));
        ps.sort(Comparator.comparing((Progress p) -> String.valueOf(p.playedAt)).reversed());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Progress p : ps.stream().limit(100).toList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("topic", p.topic);
            m.put("game", p.game);
            m.put("score", p.score);
            m.put("total", p.total);
            m.put("timeTaken", p.timeTaken);
            m.put("playedAt", String.valueOf(p.playedAt));
            out.add(m);
        }
        return out;
    }

    // Students see only their own grade. Teachers see all, or ?grade=6 / 7
    @GetMapping("/leaderboard")
    public List<Map<String, Object>> leaderboard(@RequestParam(required = false) Integer grade,
                                                 Principal principal) {
        User me = repo.findByStudentCode(principal.getName()).orElseThrow();
        List<User> candidates;
        if ("STUDENT".equals(me.role) && me.grade != null)
            candidates = repo.findByRoleAndGradeOrderByXpDesc("STUDENT", me.grade);
        else if (grade != null)
            candidates = repo.findByRoleAndGradeOrderByXpDesc("STUDENT", grade);
        else
            candidates = repo.findByRoleOrderByXpDesc("STUDENT");
        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : candidates) {
            // Ignore teacher-code accounts even if a legacy row has the wrong role value.
            if (u.studentCode != null && u.studentCode.startsWith("TCH")) continue;
            // Some older rows used a teacher login code as the display name while the role was STUDENT.
            if (u.name != null && u.name.matches("(?i)^TCH\\d+$")) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.id);
            m.put("name", u.name);
            m.put("xp", u.xp);
            out.add(m);
            if (out.size() == 10) break;
        }
        return out;
    }
}
