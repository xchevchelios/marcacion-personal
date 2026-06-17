package com.epesa.demo.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket:evidencias-marcaciones}")
    private String bucketName;

    public String subirEvidencia(UUID eventId, MultipartFile archivo) {
        try {
            // Asegurar que el bucket exista
            boolean encontrado = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!encontrado) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Generar nombre único manteniendo la extensión original
            String extension = obtenerExtension(archivo.getOriginalFilename());
            String nombreArchivo = "evidencia_" + eventId + extension;

            try (InputStream is = archivo.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(nombreArchivo)
                                .stream(is, archivo.getSize(), -1)
                                .contentType(archivo.getContentType())
                                .build()
                );
            }

            return nombreArchivo;

        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a MinIO: " + e.getMessage(), e);
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) return ".jpg";
        return nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
    }
    
    public String obtenerBucketName() {
        return this.bucketName;
    }
}