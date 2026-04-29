package com.trojanmarket.repository;

import com.trojanmarket.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByReceiverIDOrderByCreatedAtDesc(Integer receiverID);

    long countByReceiverIDAndIsReadFalse(Integer receiverID);

    void deleteByReceiverID(Integer receiverID);
}
