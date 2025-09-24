package com.example.shoozy_shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.shoozy_shop.enums.ReturnStatus;
import com.example.shoozy_shop.model.ReturnItem;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

        List<ReturnItem> findByReturnRequestId(Long returnRequestId);

        List<ReturnItem> findByOrderDetailId(Long orderDetailId);

        @Query("""
                            SELECT ri
                            FROM ReturnItem ri
                            WHERE ri.orderDetail.order.id = :orderId
                        """)
        List<ReturnItem> findAllByOrderId(@Param("orderId") Long orderId);

        @Query("""
                            SELECT COALESCE(SUM(ri.quantity), 0)
                            FROM ReturnItem ri
                            WHERE ri.orderDetail.id = :orderDetailId
                              AND ri.returnRequest.status IN (
                                  com.example.shoozy_shop.enums.ReturnStatus.PENDING,
                                  com.example.shoozy_shop.enums.ReturnStatus.APPROVED,
                                  com.example.shoozy_shop.enums.ReturnStatus.WAIT_FOR_PICKUP,
                                  com.example.shoozy_shop.enums.ReturnStatus.RETURNED,
                                  com.example.shoozy_shop.enums.ReturnStatus.REFUNDED
                              )
                        """)
        Integer sumRequestedQtyActiveByOrderDetailId(@Param("orderDetailId") Long orderDetailId);

        @Query("""
                            SELECT COALESCE(SUM(ri.quantity), 0)
                            FROM ReturnItem ri
                            WHERE ri.orderDetail.id = :orderDetailId
                              AND ri.returnRequest.status = :status
                        """)
        int sumReturnedQuantityByOrderDetailIdAndStatus(@Param("orderDetailId") Long orderDetailId,
                        @Param("status") ReturnStatus status);
}
