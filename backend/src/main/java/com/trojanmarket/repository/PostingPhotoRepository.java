package com.trojanmarket.repository;

import com.trojanmarket.entity.PostingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostingPhotoRepository extends JpaRepository<PostingPhoto, Integer> {

    List<PostingPhoto> findByPostIDOrderBySortOrderAsc(Integer postID);

    void deleteByPostID(Integer postID);
}
