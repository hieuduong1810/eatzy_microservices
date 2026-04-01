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
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        // Assume security filter allows the correct users
        List<ChatMessageDTO> messages = chatMessageService.getMessageHistory(orderId, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get message count for an order
     */
    @GetMapping("/order/{orderId}/count")
    public ResponseEntity<Long> getMessageCount(@PathVariable Long orderId) {
        return ResponseEntity.ok(chatMessageService.getMessageCount(orderId));
    }
}
