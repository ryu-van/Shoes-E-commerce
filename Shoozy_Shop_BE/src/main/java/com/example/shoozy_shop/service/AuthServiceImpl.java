package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.AuthRequest;
import com.example.shoozy_shop.dto.request.ForgotPasswordRequest;
import com.example.shoozy_shop.dto.request.RegisterRequest;
import com.example.shoozy_shop.dto.request.ResetPasswordRequest;
import com.example.shoozy_shop.dto.response.AuthResponse;
import com.example.shoozy_shop.exception.CustomException;
import com.example.shoozy_shop.model.PasswordResetToken;
import com.example.shoozy_shop.model.Role;
import com.example.shoozy_shop.model.Token;
import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.repository.PasswordResetTokenRepository;
import com.example.shoozy_shop.repository.RoleRepository;
import com.example.shoozy_shop.repository.TokenRepository;
import com.example.shoozy_shop.repository.UserRepository;
import com.example.shoozy_shop.security.JwtUtils;
import com.example.shoozy_shop.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetTokenRepository prTokenRepo;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private JavaMailSender mailSender;

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã tồn tại!");
        }
        if(userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại này đã tồn tại");
        }
        // Lấy vai trò Customer
        Role defaultRole = roleRepository.findByName("Customer")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Vai trò Customer không tồn tại"));

        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        // Tạo user mới
        User user = new User();
        user.setAvatar("http://localhost:9000/user-avatar/c2d8ed0529c4860703338647910e0b03.jpg");
        user.setFullname(request.getFullname());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCreatedAt(LocalDate.now());
        user.setUpdatedAt(LocalDate.now());
        user.setRole(defaultRole);
        user.setIsActive(true);
        // Lưu user
        User savedUser = userRepository.save(user);
        // Tạo JWT với email và vai trò
        String tokenStr = jwtUtils.generateJwtToken(savedUser.getEmail(), defaultRole.getName());
        // Lưu token vào DB
        Token token = new Token();
        token.setUser(savedUser);
        token.setToken(tokenStr);
        token.setTokenType("ACCESS_TOKEN");
        token.setExpirationDate(LocalDate.now().plusDays(1));
        token.setExpired(false);
        token.setRevoked(false);
        tokenRepository.save(token);
        // Trả về AuthResponse với thông tin bổ sung
        return new AuthResponse(tokenStr, defaultRole.getName());
    }
    @Override
    public AuthResponse login(AuthRequest request) {
        try {
            // Xác thực thông tin đăng nhập
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Tạo JWT với email và vai trò
            String tokenStr = jwtUtils.generateJwtToken(userDetails.getUsername(),
                    userDetails.getAuthorities().iterator().next().getAuthority());

            // Vô hiệu hóa các token cũ
            List<Token> oldTokens = tokenRepository
                    .findAllByUserIdAndExpiredFalseAndRevokedFalse(userDetails.getId());
            oldTokens.forEach(t -> {
                t.setExpired(true);
                t.setRevoked(true);
            });
            tokenRepository.saveAll(oldTokens);

            // Lấy user từ DB
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

            // Lưu token mới
            Token newToken = new Token();
            newToken.setUser(user);
            newToken.setToken(tokenStr);
            newToken.setTokenType("ACCESS_TOKEN");
            newToken.setExpirationDate(LocalDate.now().plusDays(1));
            newToken.setExpired(false);
            newToken.setRevoked(false);
            tokenRepository.save(newToken);

            // Trả về AuthResponse với thông tin bổ sung
            return new AuthResponse(tokenStr, userDetails.getAuthorities().iterator().next().getAuthority());
        } catch (BadCredentialsException e) {
            throw new CustomException("Email hoặc mật khẩu không đúng", 401);
        }
    }

    @Override
    public void logout(String tokenStr) {
        Token token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token không tồn tại"));
        token.setExpired(true);
        token.setRevoked(true);
        tokenRepository.save(token);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user != null) {
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setToken(token);
            prt.setExpirationDate(LocalDateTime.now().plusMinutes(15));
            prt.setUsed(false);
            prTokenRepo.save(prt);

            // Tạo email
            String resetUrl = "http://localhost:5173/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Shoozy Shop — Đặt lại mật khẩu");
            message.setText(
                    "Xin chào " + user.getFullname() + ",\n\n"
                            + "Bạn hoặc ai đó đã yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n"
                            + "Vui lòng click vào link sau để đặt lại (hết hạn sau 15 phút):\n"
                            + resetUrl + "\n\n"
                            + "Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n"
                            + "Trân trọng,\n"
                            + "Shoozy Shop Team"
            );

            // Gửi email
            mailSender.send(message);
        }

    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = prTokenRepo.findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token reset không hợp lệ"));
        if (prt.getUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã được sử dụng");
        }
        if (prt.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã hết hạn");
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDate.now());
        userRepository.save(user);

        prt.setUsed(true);
        prTokenRepo.save(prt);
    }
}