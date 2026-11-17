package com.wikicook.common.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;

/** T013 (research R7): a file store that never leaves its root and never shows a half-written file. */
class LocalDiskMediaStorageTest {

    @TempDir
    Path root;

    LocalDiskMediaStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalDiskMediaStorage(root);
    }

    private List<Path> files() throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }

    @Test
    @DisplayName("store writes the bytes under the key and load reads them back")
    void storeAndLoad() throws IOException {
        byte[] bytes = {1, 2, 3, 4, 5};

        storage.store("avatars/abc.jpg", bytes);

        Optional<Resource> loaded = storage.load("avatars/abc.jpg");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getContentAsByteArray()).isEqualTo(bytes);
        assertThat(root.resolve("avatars/abc.jpg")).exists();
    }

    @Test
    @DisplayName("R7: the file is written to a temporary name and moved, so no temporary file stays behind")
    void noTemporaryFileRemains() throws IOException {
        storage.store("avatars/one.jpg", new byte[] {9});
        storage.store("avatars/one.jpg", new byte[] {8, 8});

        assertThat(files()).containsExactly(root.resolve("avatars/one.jpg"));
        assertThat(storage.load("avatars/one.jpg").orElseThrow().getContentAsByteArray()).containsExactly(8, 8);
    }

    @Test
    @DisplayName("load of an unknown key is empty")
    void loadMissing() {
        assertThat(storage.load("avatars/missing.jpg")).isEmpty();
    }

    @Test
    @DisplayName("delete removes the file; deleting what is not there is not an error")
    void delete() {
        storage.store("avatars/gone.jpg", new byte[] {1});

        storage.delete("avatars/gone.jpg");
        storage.delete("avatars/gone.jpg");

        assertThat(storage.load("avatars/gone.jpg")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside.jpg", "avatars/../../outside.jpg", "/etc/passwd", "avatars/../../../x", "",
            "avatars\\..\\..\\x", "C:/windows/x"})
    @DisplayName("R7: a key that would leave the root is refused by store, load and delete")
    void keysOutsideTheRootAreRefused(String key) {
        assertThatThrownBy(() -> storage.store(key, new byte[] {1})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load(key)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.delete(key)).isInstanceOf(IllegalArgumentException.class);
        assertThat(root.getParent().resolve("outside.jpg")).doesNotExist();
    }
}
