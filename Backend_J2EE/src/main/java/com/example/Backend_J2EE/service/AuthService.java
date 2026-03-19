package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.account.AccountProfileResponse;
import com.example.Backend_J2EE.dto.account.UpdateProfileRequest;
import com.example.Backend_J2EE.dto.auth.LoginRequest;
import com.example.Backend_J2EE.dto.auth.RegisterRequest;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.util.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    public static final String SESSION_ACCOUNT_ID = "AUTH_ACCOUNT_ID";

    private final AccountRepository accountRepository;

    public AuthService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountProfileResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (accountRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username da ton tai");
        }
        if (accountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email da ton tai");
        }

        Account account = Account.builder()
                .username(username)
                .password(PasswordHasher.hash(request.getPassword()))
                .email(email)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(Account.Role.user)
                .build();

        Account saved = accountRepository.save(account);
        return toProfile(saved);
    }

    public Account login(LoginRequest request) {
        validateLoginRequest(request);

        String key = request.getUsernameOrEmail().trim();
        Optional<Account> found = accountRepository.findByUsernameOrEmail(key, key);
        Account account = found.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tai khoan hoac mat khau"));

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
            if (!newEmail.equals(account.getEmail()) && accountRepository.existsByEmail(newEmail)) {
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
}
