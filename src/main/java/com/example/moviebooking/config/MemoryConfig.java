package com.example.moviebooking.config;

import org.springframework.context.annotation.Configuration;

// Our own memory bean is switched OFF (commented out, 11 Nov 2026), on purpose.
// Spring AI then makes its own default ChatMemory (ChatMemoryAutoConfiguration):
// a MessageWindowChatMemory in RAM that keeps the last 20 messages of each conversation.
// To use our own size again: remove the comments below and the "#" in application.properties.
@Configuration
public class MemoryConfig {

    // Short-term memory: remembers only the last "maxMessages" messages of each conversation.
    // It lives in the app's memory, so it is lost when the app stops.
    // @Bean
    // public ChatMemory chatMemory(@Value("${memory.max-messages}") int maxMessages) {
    //     return MessageWindowChatMemory.builder()
    //             .maxMessages(maxMessages)
    //             .build();
    // }
}
