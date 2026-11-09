package com.khushi.stemquest.model;

import jakarta.persistence.*;

@Entity
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Integer grade;
    public String subject;
    public String topic;
    @Column(length = 500) public String textEn;
    @Column(length = 500) public String textHi;
    public String optionA, optionB, optionC, optionD;
    public int answer; // 0=A, 1=B, 2=C, 3=D

    public Question() {}

    public Question(int grade, String subject, String topic, String textEn, String textHi,
                    String a, String b, String c, String d, int answer) {
        this.grade = grade; this.subject = subject; this.topic = topic;
        this.textEn = textEn; this.textHi = textHi;
        this.optionA = a; this.optionB = b; this.optionC = c; this.optionD = d;
        this.answer = answer;
    }
}
