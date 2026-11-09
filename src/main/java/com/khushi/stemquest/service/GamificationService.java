package com.khushi.stemquest.service;

import com.khushi.stemquest.model.User;
import com.khushi.stemquest.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class GamificationService {
    private final UserRepository userRepo;

    public GamificationService(UserRepository userRepo) { this.userRepo = userRepo; }

    public static int xpFor(int score, int total) {
        return score * 10 + ((total > 0 && score == total) ? 20 : 0);
    }

    public static int levelFor(int xp) { return xp / 100 + 1; }

    public static List<String> badgeList(User u) {
        if (u.badges == null || u.badges.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(u.badges.split(",")));
    }

    private void addBadge(User u, String b) {
        Set<String> s = new LinkedHashSet<>(badgeList(u));
        s.add(b);
        u.badges = String.join(",", s);
    }

    public void record(User u, int score, int total) {
        u.xp += xpFor(score, total);

        LocalDate today = LocalDate.now();
        if (u.lastActiveDate == null || u.lastActiveDate.isBefore(today.minusDays(1))) {
            u.streak = 1;
        } else if (u.lastActiveDate.equals(today.minusDays(1))) {
            u.streak += 1;
        }
        u.lastActiveDate = today;

        addBadge(u, "FIRST_GAME");
        if (total > 0 && score == total) addBadge(u, "PERFECT");
        if (u.streak >= 3) addBadge(u, "STREAK3");
        if (levelFor(u.xp) >= 3) addBadge(u, "LEVEL3");
        if (u.streak >= 7) addBadge(u, "STREAK7");
        if (levelFor(u.xp) >= 5) addBadge(u, "LEVEL5");

        userRepo.save(u);
    }
}
