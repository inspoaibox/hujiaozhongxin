package com.qianniuyun.recording.service;

import com.qianniuyun.recording.entity.Recording;
import com.qianniuyun.recording.repository.RecordingRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 * 录音服务
 * 负责录音文件的加密、存储、检索和生命周期管理
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

    private final MinioClient minioClient;
    private final RecordingRepository recordingRepository;

    @Value("${minio.bucket.recordings:qianniu-recordings}")
    private String recordingsBucket;

    @Value("${recording.encryption.key}")
    private String encryptionKey;

    @Value("${recording.retention.days:90}")
    private int retentionDays;

    /**
     * 监听录音处理事件
     */
    @KafkaListener(topics = "qianniu.recording.process", groupId = "recording-service")
    public void processRecording(Map<String, Object> event) {
        String callId = (String) event.get("callId");
        String localPath = (String) event.get("path");

        try {
            processAndStore(callId, localPath);
        } catch (Exception e) {
            log.error("录音处理失败: callId={}", callId, e);
        }
    }

    /**
     * 处理并存储录音文件
     */
    public void processAndStore(String callId, String localPath) throws Exception {
        Path filePath = Path.of(localPath);
        if (!Files.exists(filePath)) {
            log.warn("录音文件不存在: {}", localPath);
            return;
        }

        // 1. 加密录音文件
        Path encryptedPath = encryptFile(filePath);

        // 2. 上传到 MinIO
        String objectName = generateObjectName(callId);
        uploadToMinio(encryptedPath, objectName);

        // 3. 更新录音记录
        Recording recording = new Recording();
        recording.setCallId(callId);
        recording.setObjectName(objectName);
        recording.setFileSize(Files.size(encryptedPath));
        recording.setEncrypted(true);
        recording.setCreatedAt(LocalDateTime.now());
        recording.setExpiresAt(LocalDateTime.now().plusDays(retentionDays));
        recordingRepository.save(recording);

        // 4. 删除本地临时文件
        Files.deleteIfExists(filePath);
        Files.deleteIfExists(encryptedPath);

        log.info("录音处理完成: callId={}, objectName={}", callId, objectName);
    }

    /**
     * 获取录音播放流（解密）
     */
    public InputStream getRecordingStream(String callId) throws Exception {
        Recording recording = recordingRepository.findByCallId(callId)
                .orElseThrow(() -> new RuntimeException("录音不存在: " + callId));

        InputStream encryptedStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(recordingsBucket)
                        .object(recording.getObjectName())
                        .build()
        );

        if (recording.isEncrypted()) {
            return decryptStream(encryptedStream);
        }
        return encryptedStream;
    }

    /**
     * AES-256-CBC 加密文件
     * 文件格式：[16字节IV][加密内容]
     */
    private Path encryptFile(Path inputPath) throws Exception {
        Path outputPath = Path.of(inputPath + ".enc");

        // 生成随机 IV（每次加密使用不同 IV）
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec secretKey = new SecretKeySpec(
                Arrays.copyOf(encryptionKey.getBytes(), 32), "AES"  // 确保32字节=256位
        );
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        try (InputStream fis = Files.newInputStream(inputPath);
             OutputStream fos = Files.newOutputStream(outputPath)) {
            // 先写入 IV（16字节），解密时读取
            fos.write(iv);
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }
        }

        return outputPath;
    }

    /**
     * AES-256-CBC 解密流
     * 从流头部读取 16 字节 IV，再解密后续内容
     */
    private InputStream decryptStream(InputStream encryptedStream) throws Exception {
        // 读取文件头部的 IV（16字节）
        byte[] iv = new byte[16];
        int read = encryptedStream.read(iv);
        if (read != 16) {
            throw new IllegalStateException("录音文件格式错误：IV 读取失败");
        }

        SecretKeySpec secretKey = new SecretKeySpec(
                Arrays.copyOf(encryptionKey.getBytes(), 32), "AES"
        );
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return new CipherInputStream(encryptedStream, cipher);
    }

    /**
     * 上传文件到 MinIO
     */
    private void uploadToMinio(Path filePath, String objectName) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(recordingsBucket)
                        .object(objectName)
                        .stream(Files.newInputStream(filePath), Files.size(filePath), -1)
                        .contentType("audio/wav")
                        .build()
        );
    }

    /**
     * 生成对象存储路径
     */
    private String generateObjectName(String callId) {
        LocalDate today = LocalDate.now();
        return String.format("recordings/%d/%02d/%02d/%s.enc",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), callId);
    }

    /**
     * 定时清理过期录音（每天凌晨3点执行）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredRecordings() {
        log.info("开始清理过期录音...");
        int deleted = recordingRepository.deleteExpiredRecordings(LocalDateTime.now());
        log.info("清理过期录音完成，共删除 {} 条", deleted);
    }
}
