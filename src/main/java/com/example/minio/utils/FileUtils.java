package com.example.minio.utils;

import org.springframework.web.multipart.MultipartFile;


public class FileUtils {
    public static long getFileSizeMegaBytes(MultipartFile file){
        return file.getSize() / (1024 * 1024);
    }
}
