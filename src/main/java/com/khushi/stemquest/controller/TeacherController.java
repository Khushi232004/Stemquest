package com.khushi.stemquest.controller;

import com.khushi.stemquest.model.*;
import com.khushi.stemquest.repository.*;
import com.khushi.stemquest.service.GamificationService;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private final UserRepository userRepo;
    private final ProgressRepository progressRepo;
    private final PasswordEncoder enc;

    public TeacherController(UserRepository userRepo, ProgressRepository progressRepo, PasswordEncoder enc) {
        this.userRepo = userRepo; this.progressRepo = progressRepo; this.enc = enc;
    }

    private List<User> studentsOf(Integer grade) {
        return userRepo.findByRole("STUDENT").stream()
                .filter(u -> grade == null || grade.equals(u.grade)).toList();
    }

    private Map<String, Object> row(User u) {
        List<Progress> ps = progressRepo.findByUserId(u.id);
        double avg = ps.stream().filter(p -> p.total > 0)
                .mapToDouble(p -> 100.0 * p.score / p.total).average().orElse(0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id);
        m.put("name", u.name);
        m.put("code", u.studentCode);
        m.put("grade", u.grade);
        m.put("className", u.className);
        m.put("xp", u.xp);
        m.put("level", GamificationService.levelFor(u.xp));
        m.put("streak", u.streak);
        m.put("games", ps.size());
        m.put("avg", Math.round(avg));
        m.put("last", u.lastActiveDate == null ? "" : u.lastActiveDate.toString());
        return m;
    }

    @GetMapping("/students")
    public List<Map<String, Object>> students(@RequestParam(required = false) Integer grade) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : studentsOf(grade)) out.add(row(u));
        return out;
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        User u = userRepo.findById(id).orElse(null);
        if (u == null || !"STUDENT".equals(u.role)) return ResponseEntity.status(404).build();
        Map<String, Object> m = row(u);
        m.put("badges", GamificationService.badgeList(u));
        List<Progress> ps = new ArrayList<>(progressRepo.findByUserId(u.id));
        ps.sort(Comparator.comparing((Progress p) -> String.valueOf(p.playedAt)).reversed());
        List<Map<String, Object>> hist = new ArrayList<>();
        for (Progress p : ps.stream().limit(15).toList()) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("topic", p.topic);
            h.put("game", p.game);
            h.put("score", p.score);
            h.put("total", p.total);
            h.put("playedAt", String.valueOf(p.playedAt));
            hist.add(h);
        }
        m.put("history", hist);
        return ResponseEntity.ok(m);
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Map<String, String>> remove(@PathVariable Long id) {
        User u = userRepo.findById(id).orElse(null);
        if (u == null || !"STUDENT".equals(u.role))
            return ResponseEntity.status(404).body(Map.of("message", "Not found"));
        progressRepo.deleteAll(progressRepo.findByUserId(u.id));
        userRepo.delete(u);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    @PostMapping("/students/{id}/password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id,
                                                             @RequestBody Map<String, String> b) {
        User u = userRepo.findById(id).orElse(null);
        if (u == null || !"STUDENT".equals(u.role))
            return ResponseEntity.status(404).body(Map.of("message", "Not found"));
        String pw = b.getOrDefault("password", "");
        if (pw.length() < 4 || pw.length() > 64)
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be 4 to 64 characters"));
        u.password = enc.encode(pw);
        userRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    @GetMapping("/topics")
    public List<Map<String, Object>> topics(@RequestParam(required = false) Integer grade) {
        Set<Long> ids = studentsOf(grade).stream().map(u -> u.id).collect(Collectors.toSet());
        Map<String, List<Progress>> byTopic = progressRepo.findAll().stream()
                .filter(p -> ids.contains(p.userId))
                .collect(Collectors.groupingBy(p -> p.topic));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<Progress>> e : byTopic.entrySet()) {
            double avg = e.getValue().stream().filter(p -> p.total > 0)
                    .mapToDouble(p -> 100.0 * p.score / p.total).average().orElse(0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("topic", e.getKey());
            m.put("avg", Math.round(avg));
            m.put("plays", e.getValue().size());
            out.add(m);
        }
        return out;
    }

    @GetMapping("/report.csv")
    public ResponseEntity<String> csv(@RequestParam(required = false) Integer grade) {
        StringBuilder sb = new StringBuilder("Name,Code,Grade,Class,XP,Level,Streak,Games,AvgScore%\n");
        for (User u : studentsOf(grade)) {
            Map<String, Object> m = row(u);
            sb.append('"').append(String.valueOf(m.get("name")).replace("\"", "\"\"")).append("\",")
              .append(m.get("code")).append(',')
              .append(m.get("grade")).append(',')
              .append(m.get("className")).append(',')
              .append(m.get("xp")).append(',')
              .append(m.get("level")).append(',')
              .append(m.get("streak")).append(',')
              .append(m.get("games")).append(',')
              .append(m.get("avg")).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(sb.toString());
    }
}
