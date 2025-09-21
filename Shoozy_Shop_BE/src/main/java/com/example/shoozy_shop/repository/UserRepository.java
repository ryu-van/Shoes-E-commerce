package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    // Phân trang theo role
    Page<User> findByRole_Name(String roleName, Pageable pageable);

    Page<User> findByRole_NameAndIsActive(String roleName, Boolean isActive, Pageable pageable);

    Page<User> findByRole_NameAndFullnameContainingIgnoreCase(String roleName, String keyword, Pageable pageable);

    Page<User> findByRole_NameAndIsActiveAndFullnameContainingIgnoreCase(String roleName, Boolean isActive,
            String keyword, Pageable pageable);
}
