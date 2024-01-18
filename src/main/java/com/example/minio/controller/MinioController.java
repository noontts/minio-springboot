package com.example.minio.controller;

import com.example.minio.response.UploadFileResponse;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalTime;

import static com.example.minio.utils.FileUtils.getFileSizeMegaBytes;

@RestController
@RequestMapping("api/v1/file")
public class MinioController {

    @Value("${minio.bucketName}")
    private String bucketName;

    private final MinioClient minioClient;
    private final String PATH_OBJECT = "picture/";

    public MinioController(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostMapping("/upload")
    private UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            final long DEFAULT_FILESIZE_MB = 800; //800MB

            if (getFileSizeMegaBytes(file) > DEFAULT_FILESIZE_MB) {
                throw new RuntimeException("File > 800MB");
            }

            InputStream inputStream = file.getInputStream();
            PutObjectArgs uploadObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(PATH_OBJECT + LocalTime.now() + file.getOriginalFilename())
                    .stream(inputStream, inputStream.available(), -1)
                    .contentType("image/png")
                    .build();

            ObjectWriteResponse resp = minioClient.putObject(uploadObjectArgs);
            return new UploadFileResponse("File uploaded successfully.", resp.object().substring(8));
        } catch (Exception exception) {
            return new UploadFileResponse(exception.getMessage(), null);
        }
    }

    @GetMapping("{fileName}")
    private ResponseEntity<?> getFile(@PathVariable String fileName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(PATH_OBJECT + fileName)
                            .build()
            );
            byte[] bytes = IOUtils.toByteArray(stream);


            return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        } catch (Exception exception) {
            return ResponseEntity.ok(exception.getMessage());
        }
    }

    @PostMapping("/delete/{fileName}")
    private ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            boolean existData = isObjectExist(fileName);
            if (existData) {
                RemoveObjectArgs rArgs = RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(PATH_OBJECT + fileName)
                        .build();
                minioClient.removeObject(rArgs);
            }
            return ResponseEntity.ok("Delete " + fileName + " successfully.");
        } catch (Exception exception) {
            return ResponseEntity.ok(exception.getMessage());
        }
    }

    private boolean isObjectExist(String objName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(PATH_OBJECT + objName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
