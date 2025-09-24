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

  // (có thể bỏ nếu không dùng nữa — đã có bản "active" ở ReturnItemRepo)
  @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM ReturnItem r WHERE r.orderDetail.id = :orderDetailId")
  int sumReturnedQuantityByOrderDetailId(@Param("orderDetailId") Long orderDetailId);

  Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

  // 🔍 Search theo user (đã OK)
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
  List<ReturnRequest> searchByUserNoPage(@Param("userId") Long userId,
      @Param("q") String q,
      @Param("statusEnum") ReturnStatus statusEnum);

  // ✅ Chi tiết cho admin/user: fetch-join đầy đủ để convert DTO không bị N+1
  @Query("""
          select rr from ReturnRequest rr
           join fetch rr.order o
           left join fetch rr.returnItems ri
           left join fetch ri.images imgs
           left join fetch ri.orderDetail od
           left join fetch od.productVariant pv
           left join fetch pv.product p
          where rr.id = :id
      """)
  java.util.Optional<ReturnRequest> findDetailById(@Param("id") Long id);
}
