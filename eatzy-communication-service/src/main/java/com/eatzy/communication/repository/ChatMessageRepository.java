package com.eatzy.communication.repository;

import com.eatzy.communication.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all chat messages for a specific order, ordered by sentAt descending.
     */
    Page<ChatMessage> findByOrderIdOrderBySentAtDesc(Long orderId, Pageable pageable);

    /**
     * Count total messages for a specific order
     */
    Long countByOrderId(Long orderId);
}
