package com.eatzy.communication.controller;

import com.eatzy.communication.dto.ChatMessageDTO;
import com.eatzy.communication.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatRestController {

    private static final Logger log = LoggerFactory.getLogger(ChatRestController.class);
    private final ChatMessageService chatMessageService;

    public ChatRestController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    /**
     * Get chat message history for an order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ChatMessageDTO>> getOrderChatHistory(
            @PathVariable("orderId") Long orderId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        
        // Assume security filter allows the correct users
        List<ChatMessageDTO> messages = chatMessageService.getMessageHistory(orderId, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get message count for an order
     */
    @GetMapping("/order/{orderId}/count")
    public ResponseEntity<Long> getMessageCount(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(chatMessageService.getMessageCount(orderId));
    }

    /**
     * Mark all current messages as read for a specific user.
     * Call this when the user opens the chat window.
     */
    @PutMapping("/order/{orderId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("orderId") Long orderId,
            @RequestParam("userId") Long userId) {
        chatMessageService.markAsRead(orderId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get the number of unread messages for a specific user in an order.
     */
    @GetMapping("/order/{orderId}/unread")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable("orderId") Long orderId,
            @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(chatMessageService.getUnreadCount(orderId, userId));
    }
}
