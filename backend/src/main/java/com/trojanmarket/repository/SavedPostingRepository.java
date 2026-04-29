package com.trojanmarket.repository;

import com.trojanmarket.entity.SavedPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPostingRepository extends JpaRepository<SavedPosting, Integer> {

    List<SavedPosting> findByUserIDOrderBySavedTimeDesc(Integer userID);

    Optional<SavedPosting> findByUserIDAndPostID(Integer userID, Integer postID);

    void deleteByUserIDAndPostID(Integer userID, Integer postID);
}
