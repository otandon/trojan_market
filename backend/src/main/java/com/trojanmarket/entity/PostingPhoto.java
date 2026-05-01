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
@Table(name = "PostingPhotos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostingPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photoID")
    private Integer photoID;

    @Column(name = "postID", nullable = false)
    private Integer postID;

    @Column(name = "photoData", nullable = false, columnDefinition = "LONGTEXT")
    private String photoData;

    @Column(name = "sortOrder", nullable = false)
    private Integer sortOrder;

    @CreationTimestamp
    @Column(name = "uploadedAt", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
