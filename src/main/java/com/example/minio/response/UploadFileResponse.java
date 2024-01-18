package com.example.minio.response;


public record UploadFileResponse(
        String status,
        String fileUrls
) {
}
