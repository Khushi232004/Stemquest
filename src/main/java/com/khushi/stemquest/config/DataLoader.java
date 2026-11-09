package com.khushi.stemquest.config;

import com.khushi.stemquest.model.User;
import com.khushi.stemquest.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {
    private final UserRepository ur;
    private final ProgressRepository pr;
    private final PasswordEncoder enc;

    @Value("${app.demo-accounts:true}")
    private boolean demo;

    public DataLoader(UserRepository ur, ProgressRepository pr, PasswordEncoder enc) {
        this.ur = ur; this.pr = pr; this.enc = enc;
    }

    @Override
    public void run(String... args) {
        // demo teacher only (no demo student)
        if (demo && ur.findByStudentCode("teacher1").isEmpty()) {
            User t = new User();
            t.name = "Demo Teacher"; t.studentCode = "teacher1";
            t.password = enc.encode("teacher123"); t.role = "TEACHER"; t.className = "-";
            ur.save(t);
        }
        // remove the old demo student and their results
        ur.findByStudentCode("student1").ifPresent(u -> {
            pr.deleteAll(pr.findByUserId(u.id));
            ur.delete(u);
        });
        // older accounts get Grade 6
        for (User u : ur.findByRole("STUDENT")) {
            if (u.grade == null) { u.grade = 6; ur.save(u); }
        }
    }
}
