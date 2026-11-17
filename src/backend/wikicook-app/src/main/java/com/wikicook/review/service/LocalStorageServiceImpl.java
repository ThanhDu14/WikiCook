package com.wikicook.review.service;

import com.wikicook.review.config.ReviewStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class LocalStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    private final Path rootLocation;

    public LocalStorageServiceImpl(ReviewStorageProperties properties) {
        this.rootLocation = Paths.get(properties.getRootPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize storage directory at {}", this.rootLocation, e);
        }
    }

    @Override
    public void store(String storageKey, byte[] fullImageBytes, byte[] thumbnailBytes) throws IOException {
        Path fullPath = resolveFullPath(storageKey);
        Path thumbPath = resolveThumbPath(storageKey);

        Files.createDirectories(fullPath.getParent());

        Files.write(fullPath, fullImageBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        if (thumbnailBytes != null && thumbnailBytes.length > 0) {
            Files.write(thumbPath, thumbnailBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }

        registerRollbackDeletion(storageKey);
    }

    @Override
    public Resource loadAsResource(String storageKey, boolean thumbnail) {
        Path filePath = thumbnail ? resolveThumbPath(storageKey) : resolveFullPath(storageKey);
        if (!Files.exists(filePath) && thumbnail) {
            // fallback to full image if thumbnail doesn't exist
            filePath = resolveFullPath(storageKey);
        }

        if (Files.exists(filePath) && Files.isReadable(filePath)) {
            return new FileSystemResource(filePath);
        }
        return null;
    }

    @Override
    public void delete(String storageKey) {
        deleteFiles(storageKey);
    }

    @Override
    public void registerRollbackDeletion(String storageKey) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        log.info("Transaction rolled back: deleting files for storageKey {}", storageKey);
                        deleteFiles(storageKey);
                    }
                }
            });
        }
    }

    @Override
    public void registerPostCommitDeletion(String storageKey) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Transaction committed: deleting removed files for storageKey {}", storageKey);
                    deleteFiles(storageKey);
                }
            });
        } else {
            deleteFiles(storageKey);
        }
    }

    private void deleteFiles(String storageKey) {
        try {
            Path fullPath = resolveFullPath(storageKey);
            Path thumbPath = resolveThumbPath(storageKey);
            Files.deleteIfExists(fullPath);
            Files.deleteIfExists(thumbPath);
        } catch (IOException e) {
            log.warn("Failed to delete files for storageKey {}: {}", storageKey, e.getMessage());
        }
    }

    private Path resolveFullPath(String storageKey) {
        return this.rootLocation.resolve(storageKey).normalize();
    }

    private Path resolveThumbPath(String storageKey) {
        return this.rootLocation.resolve("thumb_" + storageKey).normalize();
    }
}

