package com.trojanmarket.repository;

import com.trojanmarket.entity.Posting;
import com.trojanmarket.entity.PostingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostingRepository extends JpaRepository<Posting, Integer> {

    List<Posting> findBySellerIDAndIsActiveTrue(Integer sellerID);

    List<Posting> findByStatusAndIsActiveTrue(PostingStatus status);
}
