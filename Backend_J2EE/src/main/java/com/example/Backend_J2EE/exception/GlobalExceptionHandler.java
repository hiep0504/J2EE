package com.example.Backend_J2EE.exception;

import com.example.Backend_J2EE.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Sai kiểu dữ liệu trên path variable (vd: /api/products/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Tham số '" + ex.getName() + "' không hợp lệ: " + ex.getValue();
        return ResponseEntity.ok(ApiResponse.error(message));
    }

    // URL không tồn tại (404)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.ok(ApiResponse.error("Không tìm thấy đường dẫn: " + ex.getResourcePath()));
    }

    // Bắt tất cả exception còn lại
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.ok(ApiResponse.error("Lỗi hệ thống: " + ex.getMessage()));
    }
}
