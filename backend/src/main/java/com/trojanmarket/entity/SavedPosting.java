package com.trojanmarket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "SavedPostings",
       uniqueConstraints = @UniqueConstraint(name = "uk_saved_user_post", columnNames = {"userID", "postID"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "savedID")
    private Integer savedID;

    @Column(name = "userID", nullable = false)
    private Integer userID;

    @Column(name = "postID", nullable = false)
    private Integer postID;

    @CreationTimestamp
    @Column(name = "savedTime", nullable = false, updatable = false)
    private LocalDateTime savedTime;
}
