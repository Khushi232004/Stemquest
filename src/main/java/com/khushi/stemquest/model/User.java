package com.khushi.stemquest.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String name;
    @Column(unique = true)
    public String studentCode;
    public String password;
    public String role;       // STUDENT or TEACHER
    public String className;
    public Integer grade;
    public String avatar;
    public int xp = 0;
    public int streak = 0;
    public LocalDate lastActiveDate;
    public String badges = "";
}
