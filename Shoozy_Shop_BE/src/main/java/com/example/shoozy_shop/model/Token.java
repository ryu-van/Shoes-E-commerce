package com.example.shoozy_shop.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String token;      

    @Column(name = "token_type")
    private String tokenType;   

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(nullable = false)
    private Boolean expired = false;

 
    public Token() { }

    public Token(User user, String token, String tokenType, LocalDate expirationDate) {
        this.user = user;
        this.token = token;
        this.tokenType = tokenType;
        this.expirationDate = expirationDate;
        this.revoked = false;
        this.expired = false;
    }

 
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Boolean getRevoked() {
        return revoked;
    }
    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public Boolean getExpired() {
        return expired;
    }
    public void setExpired(Boolean expired) {
        this.expired = expired;
    }
}
