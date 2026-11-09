package com.khushi.stemquest.config;

import com.khushi.stemquest.model.Question;
import com.khushi.stemquest.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
public class QuestionImporter implements CommandLineRunner {
    private final QuestionRepository qr;

    public QuestionImporter(QuestionRepository qr) { this.qr = qr; }

    @Override
    public void run(String... args) throws Exception {
        // remove old questions that have no grade
        qr.deleteAll(qr.findAll().stream().filter(q -> q.grade == null).toList());

        ClassPathResource res = new ClassPathResource("questions.csv");
        if (!res.exists()) return;
        int added = 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 10) continue;
                try {
                    int grade = Integer.parseInt(p[0].trim());
                    if (qr.existsByTextEnAndGrade(p[3].trim(), grade)) continue;
                    qr.save(new Question(grade, p[1].trim(), p[2].trim(), p[3].trim(), p[4].trim(),
                            p[5].trim(), p[6].trim(), p[7].trim(), p[8].trim(),
                            Integer.parseInt(p[9].trim())));
                    added++;
                } catch (Exception e) {
                    System.out.println("QuestionImporter: skipped bad line: " + line);
                }
            }
        }
        System.out.println("QuestionImporter: added " + added + " new questions");
    }
}
