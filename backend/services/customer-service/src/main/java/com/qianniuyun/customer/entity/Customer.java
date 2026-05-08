package com.qianniuyun.customer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String phone;
    private String name;
    private String email;
    private String address;
    private String vipLevel;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
