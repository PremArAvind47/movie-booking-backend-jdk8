package com.example.moviebooking.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        vectorStore.add(List.of(
                new Document("Interstellar is a sci-fi movie about space travel and time dilation."),
                new Document("The Dark Knight is an action movie about Batman fighting the Joker in Gotham."),
                new Document("Inception is a sci-fi thriller about entering people's dreams to plant an idea."),
                new Document("Titanic is a romance movie about two lovers on a doomed ship."),
                new Document("The Godfather is a crime drama about a powerful mafia family.")
        ));

        return vectorStore;
    }
}
