package com.trojanmarket.repository;

import com.trojanmarket.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {

    Optional<ChatSession> findByPostIDAndBuyerID(Integer postID, Integer buyerID);

    List<ChatSession> findByBuyerIDOrSellerID(Integer buyerID, Integer sellerID);
}
