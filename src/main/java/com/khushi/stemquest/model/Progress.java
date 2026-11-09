package com.khushi.stemquest.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Progress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long userId;
    public String topic;
    public String game;      // QUIZ or MATCH
    public int score;
    public int total;
    public int timeTaken;    // seconds
    public LocalDateTime playedAt;
    @Column(unique = true)
    public String clientId;  // prevents duplicate saving when syncing
}
