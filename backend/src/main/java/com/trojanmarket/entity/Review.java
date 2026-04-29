package com.trojanmarket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewID")
    private Integer reviewID;

    @Column(name = "reviewerID", nullable = false)
    private Integer reviewerID;

    @Column(name = "sellerID", nullable = false)
    private Integer sellerID;

    @Column(name = "transactionID", nullable = false, unique = true)
    private Integer transactionID;

    @Column(name = "rating", nullable = false, columnDefinition = "TINYINT")
    private Short rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "reviewTime", nullable = false, updatable = false)
    private LocalDateTime reviewTime;
}
