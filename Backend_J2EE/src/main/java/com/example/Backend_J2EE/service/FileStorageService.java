package com.example.Backend_J2EE.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long REVIEW_VIDEO_MAX_BYTES = 25L * 1024L * 1024L;

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    public FileStorageService(
            S3Client s3Client,
            @Value("${app.aws.bucket-name}") String bucketName,
            @Value("${app.aws.region}") String region
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.region = region;
    }

    public String storeReviewVideo(MultipartFile file) {
        return store(file, "videos", REVIEW_VIDEO_MAX_BYTES, "Video chi duoc toi da 25MB");
    }

    public String store(MultipartFile file, String subDir) {
        return store(file, subDir, null, null);
    }

    private String store(MultipartFile file, String subDir, Long maxSizeBytes, String maxSizeMessage) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File khong hop le");
        }

        if (maxSizeBytes != null && file.getSize() > maxSizeBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    maxSizeMessage == null || maxSizeMessage.isBlank() ? "File vuot qua kich thuoc cho phep" : maxSizeMessage);
        }

        String normalizedSubDir = normalizeSubDir(subDir);
        if (bucketName == null || bucketName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AWS bucket name chua duoc cau hinh");
        }

        try {
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            String key = normalizedSubDir + "/" + UUID.randomUUID() + ext;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(resolveContentType(file))
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
        } catch (IOException | S3Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Luu file that bai");
        }
    }

    private String normalizeSubDir(String subDir) {
        if (subDir == null || subDir.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thu muc upload khong hop le");
        }
        return subDir.replace("\\", "/").replaceAll("^/+|/+$", "");
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType;
    }
}