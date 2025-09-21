package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    List<Token> findAllByUserIdAndExpiredFalseAndRevokedFalse(Long userId);
}
