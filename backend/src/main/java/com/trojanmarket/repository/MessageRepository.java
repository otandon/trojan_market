package com.trojanmarket.repository;

import com.trojanmarket.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findBySessionIDOrderByMessageTimeAsc(Integer sessionID);

    long countBySessionIDAndSenderIDNotAndIsReadFalse(Integer sessionID, Integer senderID);
}
