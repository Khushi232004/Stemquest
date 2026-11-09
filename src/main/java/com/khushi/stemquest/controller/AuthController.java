package com.khushi.stemquest.controller;

import com.khushi.stemquest.model.User;
import com.khushi.stemquest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.SecureRandom;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository repo;
    private final PasswordEncoder enc;
    private final SecureRandom rnd = new SecureRandom();

    @Value("${app.teacher-key:}")
    private String teacherKey;
    @Value("${app.student-join-code:}")
    private String joinCode;

    public AuthController(UserRepository repo, PasswordEncoder enc) {
        this.repo = repo; this.enc = enc;
    }

    private ResponseEntity<Map<String, String>> err(int status, String msg) {
        return ResponseEntity.status(status).body(Map.of("message", msg));
    }

    // Makes a unique login code like STU48213 or TCH20517
    private String newCode(String role) {
        String prefix = "TEACHER".equals(role) ? "TCH" : "STU";
        String code;
        do {
            code = prefix + (10000 + rnd.nextInt(90000));
        } while (repo.existsByStudentCode(code));
        return code;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> b) {
        String name = b.getOrDefault("name", "").trim();
        String pw = b.getOrDefault("password", "");
        String cls = b.getOrDefault("className", "").trim();

        if (name.isEmpty() || name.length() > 50) return err(400, "Enter a name (max 50 letters)");
        if (pw.length() < 4 || pw.length() > 64) return err(400, "Password must be 4 to 64 characters");
        if (cls.length() > 20) return err(400, "Class name is too long");
        int grade = 0;
        try { grade = Integer.parseInt(b.getOrDefault("grade", "0")); } catch (Exception e) { }

        String role = "STUDENT";
        if ("TEACHER".equals(b.get("role"))) {
            if (teacherKey.isBlank() || !teacherKey.equals(b.get("teacherKey")))
                return err(403, "Wrong teacher key");
            role = "TEACHER";
        } else if (!joinCode.isBlank() && !joinCode.equals(b.get("joinCode"))) {
            return err(403, "Wrong class join code");
        }
        if ("STUDENT".equals(role) && grade != 6 && grade != 7) return err(400, "Choose Grade 6 or 7");

        User u = new User();
        u.name = name;
        u.studentCode = newCode(role);
        u.password = enc.encode(pw);
        u.role = role;
        u.grade = "STUDENT".equals(role) ? Integer.valueOf(grade) : null;
        u.className = cls.isEmpty() ? "-" : cls;
        repo.save(u);
        return ResponseEntity.ok(Map.of("message", "ok", "studentCode", u.studentCode));
    }
}
