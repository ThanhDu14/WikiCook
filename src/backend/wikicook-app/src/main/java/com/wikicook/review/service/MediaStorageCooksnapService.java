package com.wikicook.review.service;

import com.wikicook.common.media.CooksnapKeys;
import com.wikicook.common.media.MediaStorage;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Cooksnap files in the shared {@link MediaStorage} (local disk or S3, {@code wikicook.media.storage}), under the keys
 * of {@link CooksnapKeys}. Writes are undone when the transaction rolls back; deletions and hiding happen after it
 * commits, so the database and the files never disagree about a committed Cooksnap.
 */
@Service
public class MediaStorageCooksnapService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageCooksnapService.class);

    private final MediaStorage storage;

    public MediaStorageCooksnapService(MediaStorage storage) {
        this.storage = storage;
    }

    @Override
    public void store(String storageKey, byte[] fullImageBytes, byte[] thumbnailBytes) {
        storage.store(CooksnapKeys.publicKey(storageKey, false), fullImageBytes);
        // The CDN serves the thumbnail key as it is, so there is always one: the full image when none was made
        byte[] thumbnail = thumbnailBytes != null && thumbnailBytes.length > 0 ? thumbnailBytes : fullImageBytes;
        storage.store(CooksnapKeys.publicKey(storageKey, true), thumbnail);

        registerRollbackDeletion(storageKey);
    }

    @Override
    public Resource loadAsResource(String storageKey, boolean thumbnail) {
        Optional<Resource> resource = storage.load(CooksnapKeys.publicKey(storageKey, thumbnail));
        if (resource.isEmpty() && thumbnail) {
            resource = storage.load(CooksnapKeys.publicKey(storageKey, false));
        }
        return resource.orElse(null);
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
        afterCommit(storageKey, "deleting removed files", () -> deleteFiles(storageKey));
    }

    @Override
    public void registerPostCommitHide(String storageKey) {
        afterCommit(storageKey, "moving hidden files off the public paths", () -> moveFiles(storageKey, true));
    }

    @Override
    public void registerPostCommitRestore(String storageKey) {
        afterCommit(storageKey, "moving restored files back to the public paths", () -> moveFiles(storageKey, false));
    }

    private void afterCommit(String storageKey, String what, Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Transaction committed: {} for storageKey {}", what, storageKey);
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void moveFiles(String storageKey, boolean hide) {
        for (boolean thumbnail : new boolean[] {false, true}) {
            String publicKey = CooksnapKeys.publicKey(storageKey, thumbnail);
            String hiddenKey = CooksnapKeys.hiddenKey(storageKey, thumbnail);
            try {
                if (hide) {
                    storage.move(publicKey, hiddenKey);
                } else {
                    storage.move(hiddenKey, publicKey);
                }
            } catch (RuntimeException e) {
                log.warn("Failed to {} file {} of storageKey {}: {}", hide ? "hide" : "restore",
                        thumbnail ? "thumbnail" : "full", storageKey, e.getMessage());
            }
        }
    }

    private void deleteFiles(String storageKey) {
        for (boolean thumbnail : new boolean[] {false, true}) {
            for (String key : new String[] {CooksnapKeys.publicKey(storageKey, thumbnail),
                    CooksnapKeys.hiddenKey(storageKey, thumbnail)}) {
                try {
                    storage.delete(key);
                } catch (RuntimeException e) {
                    log.warn("Failed to delete {} for storageKey {}: {}", key, storageKey, e.getMessage());
                }
            }
        }
    }
}
