package com.example.moviebooking.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfig {

    // Short-term memory: remembers only the last "maxMessages" messages of each conversation.
    // It lives in the app's memory, so it is lost when the app stops.
    @Bean
    public ChatMemory chatMemory(@Value("${memory.max-messages}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .maxMessages(maxMessages)
                .build();
    }
}
