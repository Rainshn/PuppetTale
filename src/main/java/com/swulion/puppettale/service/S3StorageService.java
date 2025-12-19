package com.swulion.puppettale.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3StorageService {

//    private final S3Client s3Client;
//
//    @Value("${app.s3.bucket}")
//    private String bucket;
//
//    public String uploadImage(String fileName, byte[] data) {
//        PutObjectRequest put = PutObjectRequest.builder()
//                .bucket(bucket)
//                .key(fileName)
//                .contentType("image/png")
//                .build();
//
//        s3Client.putObject(put, RequestBody.fromBytes(data));
//
//        return "https://" + bucket + ".s3." +
//                s3Client.serviceClientConfiguration().region().id() +
//                ".amazonaws.com/" + fileName;
//    }

    // 테스트용: 실제 S3 호출 안함
    public String uploadImage(String fileName, byte[] data) {
        return "https://cdn.example.com/placeholder-image.png";
    }
}
