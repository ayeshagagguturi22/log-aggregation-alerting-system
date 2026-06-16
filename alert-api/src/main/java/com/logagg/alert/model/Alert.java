package com.logagg.alert.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String service;
    private int errorCount;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private LocalDateTime createdAt;
}
