package com.logagg.alert.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "log_events")
public class LogEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String service;
    private String level;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime createdAt;
}
