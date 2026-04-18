package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.account.AccountProfileResponse;
import com.example.Backend_J2EE.dto.account.UpdateProfileRequest;
import com.example.Backend_J2EE.dto.auth.AuthMessageResponse;
import com.example.Backend_J2EE.dto.auth.ForgotPasswordRequest;
import com.example.Backend_J2EE.dto.auth.GoogleLoginRequest;
import com.example.Backend_J2EE.dto.auth.LoginRequest;
import com.example.Backend_J2EE.dto.auth.RegisterRequest;
import com.example.Backend_J2EE.dto.auth.ResetPasswordRequest;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.util.PasswordHasher;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HexFormat;

@Service
public class AuthService {

    public static final String SESSION_ACCOUNT_ID = "AUTH_ACCOUNT_ID";
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";
    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;
    private final String googleClientId;
    private final String frontendUrl;
    private final String mailFrom;

    public AuthService(
            AccountRepository accountRepository,
            JavaMailSender mailSender,
            @Value("${app.google.client-id:}") String googleClientId,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${spring.mail.username:}") String mailFrom) {
        this.accountRepository = accountRepository;
        this.restTemplate = new RestTemplate();
        this.mailSender = mailSender;
        this.googleClientId = googleClientId;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
    }

    public AccountProfileResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (accountRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username da ton tai");
        }
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email da ton tai");
        }

        Account account = Account.builder()
                .username(username)
                .password(PasswordHasher.hash(request.getPassword()))
                .email(email)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(Account.Role.user)
                .loginType(Account.LoginType.local)
                .build();

        Account saved = accountRepository.save(account);
        return toProfile(saved);
    }

    public Account login(LoginRequest request) {
        validateLoginRequest(request);

        String key = request.getUsernameOrEmail().trim();
        Optional<Account> found = accountRepository.findByUsernameOrEmail(key, key);
        if (found.isEmpty() && key.contains("@")) {
            found = accountRepository.findByEmailIgnoreCase(key);
        }
        Account account = found.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tai khoan hoac mat khau"));

        if (Boolean.TRUE.equals(account.getLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan da bi khoa");
        }

        if (account.getLoginType() == Account.LoginType.google) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tai khoan nay dang nhap bang Google");
        }

        if (!PasswordHasher.matches(request.getPassword(), account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tai khoan hoac mat khau");
        }

        // Optional: auto-upgrade legacy plaintext passwords to hashed format.
        if (account.getPassword() != null && !account.getPassword().startsWith("pbkdf2$")) {
            account.setPassword(PasswordHasher.hash(request.getPassword()));
            accountRepository.save(account);
        }

        return account;
    }

    public Account loginWithGoogle(GoogleLoginRequest request) {
        validateGoogleLoginRequest(request);

        Map<String, Object> tokenInfo = fetchGoogleTokenInfo(request.getIdToken().trim());

        String googleId = asString(tokenInfo.get("sub"));
        String email = asString(tokenInfo.get("email")).toLowerCase(Locale.ROOT);
        String name = asString(tokenInfo.get("name"));
        String avatarUrl = asString(tokenInfo.get("picture"));
        String audience = asString(tokenInfo.get("aud"));
        String emailVerified = asString(tokenInfo.get("email_verified"));

        if (googleId.isBlank() || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thong tin Google khong hop le");
        }

        if (!googleClientId.isBlank() && !googleClientId.equals(audience)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token khong dung client id");
        }

        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email Google chua duoc xac minh");
        }

        Optional<Account> byGoogleId = accountRepository.findByGoogleId(googleId);
        Optional<Account> byEmail = accountRepository.findByEmailIgnoreCase(email);

        Account account = byGoogleId.or(() -> byEmail)
                .orElseGet(() -> createGoogleAccount(googleId, email, name, avatarUrl));

        if (Boolean.TRUE.equals(account.getLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan da bi khoa");
        }

        boolean changed = false;

        if (account.getLoginType() != Account.LoginType.google) {
            account.setLoginType(Account.LoginType.google);
            changed = true;
        }

        if (account.getGoogleId() == null || account.getGoogleId().isBlank()) {
            account.setGoogleId(googleId);
            changed = true;
        }

        if (avatarUrl != null && !avatarUrl.isBlank() && !avatarUrl.equals(account.getAvatarUrl())) {
            account.setAvatarUrl(avatarUrl);
            changed = true;
        }

        if (changed) {
            account = accountRepository.save(account);
        }

        return account;
    }

    public Account getAccountOrThrow(Integer accountId) {
        if (accountId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
        }
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tai khoan khong ton tai"));
    }

    public AccountProfileResponse getProfile(Integer accountId) {
        return toProfile(getAccountOrThrow(accountId));
    }

    public AccountProfileResponse updateProfile(Integer accountId, UpdateProfileRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thong tin cap nhat la bat buoc");
        }
        Account account = getAccountOrThrow(accountId);

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim();
            if (newEmail.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email khong hop le");
            }
            if (!newEmail.equals(account.getEmail()) && accountRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email da ton tai");
            }
            account.setEmail(newEmail);
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            account.setPhone(phone.isBlank() ? null : phone);
        }

        Account saved = accountRepository.save(account);
        return toProfile(saved);
    }

    public AccountProfileResponse toProfile(Account account) {
        return new AccountProfileResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getPhone(),
                account.getRole() != null ? account.getRole().name() : null,
                account.getCreatedAt()
        );
    }

    public AuthMessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email la bat buoc");
        }

        String email = request.getEmail().trim();
        accountRepository.findByEmailIgnoreCase(email).ifPresent(this::createAndSendPasswordResetToken);

        return new AuthMessageResponse("Neu email ton tai, chung toi da gui lien ket dat lai mat khau qua Gmail.");
    }

    public AuthMessageResponse resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thong tin dat lai mat khau la bat buoc");
        }
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token dat lai mat khau la bat buoc");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mat khau la bat buoc");
        }

        String tokenHash = hashToken(request.getToken().trim());
        Account account = accountRepository
                .findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token khong hop le hoac da het han"));

        account.setPassword(PasswordHasher.hash(request.getPassword()));
        account.setLoginType(Account.LoginType.local);
        account.setPasswordResetTokenHash(null);
        account.setPasswordResetTokenExpiresAt(null);
        accountRepository.save(account);

        return new AuthMessageResponse("Dat lai mat khau thanh cong. Ban co the dang nhap bang mat khau moi.");
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thong tin dang ky la bat buoc");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username la bat buoc");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password la bat buoc");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email la bat buoc");
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thong tin dang nhap la bat buoc");
        }
        if (request.getUsernameOrEmail() == null || request.getUsernameOrEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username/Email la bat buoc");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password la bat buoc");
        }
    }

    private void validateGoogleLoginRequest(GoogleLoginRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google idToken la bat buoc");
        }
    }

    private Map<String, Object> fetchGoogleTokenInfo(String idToken) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(GOOGLE_TOKEN_INFO_URL, Map.class, idToken);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Khong xac thuc duoc Google token");
            }
            return response;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token khong hop le");
        }
    }

    private Account createGoogleAccount(String googleId, String email, String name, String avatarUrl) {
        String username = generateUsername(email, name);

        Account account = Account.builder()
                .username(username)
                .email(email)
                .password(null)
                .phone(null)
                .role(Account.Role.user)
                .loginType(Account.LoginType.google)
                .googleId(googleId)
                .avatarUrl(avatarUrl)
                .build();

        return accountRepository.save(account);
    }

    private String generateUsername(String email, String name) {
        String base;

        if (name != null && !name.isBlank()) {
            base = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        } else {
            int atIndex = email.indexOf('@');
            base = atIndex > 0 ? email.substring(0, atIndex).toLowerCase(Locale.ROOT) : "googleuser";
            base = base.replaceAll("[^a-z0-9]", "");
        }

        if (base.isBlank()) {
            base = "googleuser";
        }

        String candidate = base;
        int suffix = 1;
        while (accountRepository.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private void createAndSendPasswordResetToken(Account account) {
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chua cau hinh Gmail de gui email khoi phuc mat khau");
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        account.setPasswordResetTokenHash(hashToken(rawToken));
        account.setPasswordResetTokenExpiresAt(LocalDateTime.now().plus(PASSWORD_RESET_TOKEN_TTL));
        accountRepository.save(account);

        sendResetEmail(account.getEmail(), account.getUsername(), buildResetLink(rawToken));
    }

    private void sendResetEmail(String recipientEmail, String username, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom);
            helper.setTo(recipientEmail);
            helper.setSubject("Dat lai mat khau tai khoan");
            helper.setText(buildEmailHtml(username, resetLink), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong the gui email dat lai mat khau");
        }
    }

    private String buildResetLink(String rawToken) {
        String baseUrl = frontendUrl == null ? "" : frontendUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/reset-password?token=" + rawToken;
    }

    private String buildEmailHtml(String username, String resetLink) {
        String safeUsername = username == null || username.isBlank() ? "ban" : username;
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.6;color:#2d2a27">
                  <h2 style="margin:0 0 16px">Dat lai mat khau</h2>
                  <p>Xin chao %s,</p>
                  <p>Chung toi da nhan yeu cau dat lai mat khau cho tai khoan cua ban.</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 18px;background:#2f7a5f;color:#fff;text-decoration:none;border-radius:8px;font-weight:700">Dat lai mat khau</a></p>
                  <p>Hoac sao chep lien ket sau vao trinh duyet:</p>
                  <p style="word-break:break-all"><a href="%s">%s</a></p>
                  <p>Lien ket nay se het han sau 30 phut.</p>
                  <p>Neu ban khong yeu cau thao tac nay, hay bo qua email nay.</p>
                </div>
                """.formatted(safeUsername, resetLink, resetLink, resetLink);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Khong the tao token dat lai mat khau", ex);
        }
    }
}
