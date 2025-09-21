package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.AddressResponse;
import com.example.shoozy_shop.model.Address;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByUser_Id(Long userId);

    boolean existsByIdAndUser_Id(Long id, Long userId);

    @Query("""
      select new com.example.shoozy_shop.dto.response.AddressResponse(
        a.id, a.provinceName, a.districtName, a.wardName, a.addressDetail, a.isSelected
      )
      from Address a
      where a.isSelected = true and a.user.id = :userId
      order by a.updatedAt desc
    """)
    List<AddressResponse> findSelectedProjectionByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Address a set a.isSelected = false where a.user.id = :userId and a.isSelected = true")
    int unselectAllByUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Address a set a.isSelected = false where a.user.id = :userId and a.id <> :keepId and a.isSelected = true")
    int unselectOthers(@Param("userId") Long userId, @Param("keepId") Long keepId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Address a set a.isSelected = true where a.id = :id and a.user.id = :userId")
    int markSelected(@Param("id") Long id, @Param("userId") Long userId);
}
