package com.example.shoozy_shop.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.shoozy_shop.enums.ReturnStatus;
import com.example.shoozy_shop.model.ReturnRequest;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
  List<ReturnRequest> findByUserId(Long userId);

  @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM ReturnItem r WHERE r.orderDetail.id = :orderDetailId")
  int sumReturnedQuantityByOrderDetailId(@Param("orderDetailId") Long orderDetailId);

  Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

  // ReturnRequestRepository.java
  @Query("""
          select distinct rr
          from ReturnRequest rr
            left join rr.returnItems ri
            left join ri.orderDetail od
            left join od.productVariant pv
            left join pv.product p
          where rr.user.id = :userId
            and (:statusEnum is null or rr.status = :statusEnum)
            and (
                 :q is null
              or lower(rr.order.orderCode) like concat('%', lower(:q), '%')
              or lower(coalesce(p.name, '')) like concat('%', lower(:q), '%')
            )
          order by rr.updatedAt desc, rr.createdAt desc
      """)
  List<ReturnRequest> searchByUserNoPage(
      @Param("userId") Long userId,
      @Param("q") String q,
      @Param("statusEnum") ReturnStatus statusEnum);

}
