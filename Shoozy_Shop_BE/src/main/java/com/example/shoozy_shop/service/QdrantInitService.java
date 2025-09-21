package com.example.shoozy_shop.service;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class QdrantInitService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(QdrantInitService.class);

    @Autowired
    private QdrantClient qdrantClient;

    @Value("${qdrant.collection-name:shoozy-collection}")
    private String collectionName;

    @Override
    public void run(String... args) throws Exception {
        try {
            initializeCollection();
        } catch (Exception e) {
            logger.warn("Could not initialize Qdrant collection: {}", e.getMessage());
        }
    }

    private void initializeCollection() {
        try {
            logger.info("Checking if Qdrant collection exists: {}", collectionName);

            // Thử tạo collection, nếu đã tồn tại sẽ báo lỗi và ta bỏ qua
            try {
                Collections.CreateCollection createCollection = Collections.CreateCollection.newBuilder()
                        .setCollectionName(collectionName)
                        .setVectorsConfig(Collections.VectorsConfig.newBuilder()
                                .setParams(Collections.VectorParams.newBuilder()
                                        .setSize(768) // nomic-embed-text embedding size
                                        .setDistance(Collections.Distance.Cosine)
                                        .build())
                                .build())
                        .build();

                qdrantClient.createCollectionAsync(createCollection).get();
                logger.info("Successfully created Qdrant collection: {}", collectionName);

            } catch (Exception createEx) {
                // Collection có thể đã tồn tại, kiểm tra thông báo lỗi
                if (createEx.getMessage().contains("already exists") ||
                        createEx.getMessage().contains("Collection") ||
                        createEx.getCause().toString().contains("ALREADY_EXISTS")) {
                    logger.info("Qdrant collection already exists: {}", collectionName);
                } else {
                    logger.error("Failed to create collection: {}", createEx.getMessage());
                    throw createEx;
                }
            }

        } catch (Exception e) {
            logger.error("Failed to initialize Qdrant collection: {}", e.getMessage());
            // Không throw exception để app có thể tiếp tục chạy
            logger.warn("Application will continue without Qdrant collection initialization");
        }
    }
}