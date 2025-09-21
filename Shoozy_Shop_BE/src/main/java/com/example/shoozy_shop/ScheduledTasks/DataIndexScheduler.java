//package com.example.shoozy_shop.ScheduledTasks;
//
//import com.example.shoozy_shop.service.ProductDataService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//@Component
//public class DataIndexScheduler {
//
//    private static final Logger logger = LoggerFactory.getLogger(DataIndexScheduler.class);
//
//    @Autowired
//    private ProductDataService productDataService;
//
//    // Chạy mỗi 6 giờ để cập nhật dữ liệu
//    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
//    public void scheduleDataIndexing() {
//        try {
//            logger.info("Bắt đầu cập nhật dữ liệu vector store...");
//            productDataService.indexProductData();
//            productDataService.indexPromotionData();
//            logger.info("Hoàn thành cập nhật dữ liệu vector store");
//        } catch (Exception e) {
//            logger.error("Lỗi khi cập nhật dữ liệu vector store: ", e);
//        }
//    }
//}