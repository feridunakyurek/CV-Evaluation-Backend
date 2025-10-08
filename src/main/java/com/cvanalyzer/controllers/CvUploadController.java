package com.cvanalyzer.controllers;

import com.cvanalyzer.entities.CvUpload;
import com.cvanalyzer.entities.User;
import com.cvanalyzer.exceptions.FileStorageException;
import com.cvanalyzer.repos.CvUploadRepository;
import com.cvanalyzer.repos.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/cv")
public class CvUploadController {

    private final String UPLOAD_DIR = "uploads/";

    private final CvUploadRepository cvUploadRepository;
    private final UserRepository userRepository;

    public CvUploadController(CvUploadRepository cvUploadRepository, UserRepository userRepository) {
        this.cvUploadRepository = cvUploadRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadCv(@RequestParam("file") MultipartFile file,
                                           @RequestParam("userId") Long userId) {

        try {
            // 10 MB sınırı
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("Dosya boyutu 10 MB'tan büyük olamaz.");
            }

            // İzin verilen dosya tipleri
            List<String> allowedTypes = List.of(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );

            if (!allowedTypes.contains(file.getContentType())) {
                throw new RuntimeException("Sadece PDF veya Word dosyaları yüklenebilir.");
            }

            // Kullanıcı kontrolü
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

            // Kullanıcının mevcut CV sayısını kontrol et
            long cvCount = cvUploadRepository.countByUser(user);
            if (cvCount >= 3) {
                throw new RuntimeException("Bir kullanıcı en fazla 3 CV yükleyebilir.");
            }

            // Benzersiz dosya adı
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            try {
                Files.copy(file.getInputStream(), filePath);
            } catch (IOException e) {
                throw new FileStorageException("Dosya yüklenirken hata oluştu: ", e);
            }

            // Aynı dosya daha önce yüklenmiş mi kontrol et
            boolean alreadyExists = cvUploadRepository.existsByUserAndOriginalFileName(user, file.getOriginalFilename());
            if (alreadyExists) {
                return ResponseEntity.ok("CV başarıyla yüklendi: " + fileName);
            }

            // CvUpload nesnesi oluştur
            CvUpload cvUpload = new CvUpload();
            cvUpload.setFileName(fileName);
            cvUpload.setOriginalFileName(file.getOriginalFilename());
            cvUpload.setFileType(file.getContentType());
            cvUpload.setFilePath(filePath.toString());
            cvUpload.setFileSize(file.getSize());
            cvUpload.setUploadDate(LocalDateTime.now());
            cvUpload.setUser(user);

            cvUploadRepository.save(cvUpload);

            return ResponseEntity.ok("CV başarıyla yüklendi: " + fileName);
        } catch (FileStorageException e) {
            throw new RuntimeException("Dosya yükleme sırasında hata oluştu.", e);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CvUpload>> getUserCvs(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        List<CvUpload> cvs = cvUploadRepository.findByUser(user);

        return ResponseEntity.ok(cvs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long id) {
        CvUpload cvUpload = cvUploadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CV bulunamadı"));

        try {
            Path filePath = Paths.get(cvUpload.getFilePath());
            UrlResource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cvUpload.getOriginalFileName() + "\"")
                        .body((Resource) resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteCv(@PathVariable Long id) {
        CvUpload cvUpload = cvUploadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CV bulunamadı"));

        try {
            // Dosyayı sil
            Path filePath = Paths.get(cvUpload.getFilePath());
            Files.deleteIfExists(filePath);

            // Veritabanından sil
            cvUploadRepository.delete(cvUpload);

            return ResponseEntity.ok("CV başarıyla silindi.");

        } catch (IOException e) {
            throw new FileStorageException("CV silme sırasında hata oluştu.");
        }

    }
}
