package com.example.backend.file.service;

import com.example.backend.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${aws.access-key-id}")
    private String accessKeyId;

    @Value("${aws.secret-access-key}")
    private String secretAccessKey;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region:ap-southeast-1}")
    private String region;

    private S3Client s3Client;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "audio/mpeg", "audio/ogg",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "application/zip",
            "application/x-zip-compressed"
    );

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
        configureBucket();
    }

    private void configureBucket() {
        // Tắt block public access để có thể set bucket policy public
        s3Client.putPublicAccessBlock(PutPublicAccessBlockRequest.builder()
                .bucket(bucketName)
                .publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
                        .blockPublicAcls(false)
                        .ignorePublicAcls(false)
                        .blockPublicPolicy(false)
                        .restrictPublicBuckets(false)
                        .build())
                .build());

        // Bucket policy: public read cho tất cả objects
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Sid": "PublicReadGetObject",
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": "s3:GetObject",
                      "Resource": "arn:aws:s3:::%s/*"
                    }
                  ]
                }
                """.formatted(bucketName);

        s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(policy)
                .build());

        // CORS cho frontend upload trực tiếp
        s3Client.putBucketCors(PutBucketCorsRequest.builder()
                .bucket(bucketName)
                .corsConfiguration(CORSConfiguration.builder()
                        .corsRules(CORSRule.builder()
                                .allowedOrigins("*")
                                .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
                                .allowedHeaders("*")
                                .exposeHeaders("ETag")
                                .maxAgeSeconds(3000)
                                .build())
                        .build())
                .build());
    }

    /**
     * Upload file lên S3, trả về public URL đầy đủ.
     */
    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size (50MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }

        try {
            String extension = getExtensionFromContentType(contentType);
            String key = UUID.randomUUID() + extension;

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return getPublicUrl(key);
        } catch (IOException e) {
            throw new RuntimeException("Could not save file: " + e.getMessage());
        }
    }

    /**
     * Trả về public URL cho một S3 key.
     */
    public String getPublicUrl(String key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucketName, region, key);
    }

    /**
     * Tải file từ S3. Chấp nhận cả S3 URL đầy đủ lẫn key thuần.
     */
    public byte[] loadFile(String identifier) {
        String key = extractKey(identifier);
        try {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucketName).key(key).build()
            ).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("File not found: " + identifier);
        } catch (Exception e) {
            throw new RuntimeException("Could not load file: " + e.getMessage());
        }
    }

    public void deleteFile(String identifier) {
        String key = extractKey(identifier);
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    }

    public String detectContentType(String identifier) {
        String name = extractKey(identifier).toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".webm")) return "video/webm";
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".ogg")) return "audio/ogg";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".doc")) return "application/msword";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".xls")) return "application/vnd.ms-excel";
        if (name.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (name.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (name.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".zip")) return "application/zip";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /**
     * Nếu identifier là full URL, trích xuất key; nếu không thì dùng nguyên.
     */
    private String extractKey(String identifier) {
        if (identifier != null && identifier.startsWith("http")) {
            // https://bucket.s3.region.amazonaws.com/key
            int idx = identifier.indexOf(".amazonaws.com/");
            if (idx != -1) {
                return identifier.substring(idx + ".amazonaws.com/".length());
            }
        }
        return identifier;
    }

    private String getExtensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "application/pdf" -> ".pdf";
            case "audio/mpeg" -> ".mp3";
            case "audio/ogg" -> ".ogg";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.ms-excel" -> ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "application/vnd.ms-powerpoint" -> ".ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "text/plain" -> ".txt";
            case "application/zip", "application/x-zip-compressed" -> ".zip";
            default -> "";
        };
    }
}
