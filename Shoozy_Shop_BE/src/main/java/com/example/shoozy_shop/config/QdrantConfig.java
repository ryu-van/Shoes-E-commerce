package com.example.shoozy_shop.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    private static final Logger logger = LoggerFactory.getLogger(QdrantConfig.class);

    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.port:6334}")
    private int qdrantPort;

    @Value("${qdrant.collection-name:shoozy-collection}")
    private String collectionName;

    @Autowired
    private EmbeddingModel embeddingModel; // Spring Boot tự tạo

    @Bean
    public QdrantClient qdrantClient() {
        try {
            logger.info("Connecting to Qdrant at {}:{}", qdrantHost, qdrantPort);
            QdrantClient client = new QdrantClient(
                    QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false)
                            .build()
            );
            logger.info("Successfully connected to Qdrant");
            return client;
        } catch (Exception e) {
            logger.error("Failed to connect to Qdrant: {}", e.getMessage());
            throw new RuntimeException("Cannot connect to Qdrant. Please make sure Qdrant server is running on " + qdrantHost + ":" + qdrantPort, e);
        }
    }

    @Bean
    public QdrantVectorStore vectorStore(QdrantClient qdrantClient) {
        try {
            return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                    .collectionName(collectionName)
                    .initializeSchema(true) // Đặt lại true để tự động tạo schema
                    .build();
        } catch (Exception e) {
            logger.error("Failed to create QdrantVectorStore: {}", e.getMessage());
            throw new RuntimeException("Cannot create QdrantVectorStore", e);
        }
    }
}