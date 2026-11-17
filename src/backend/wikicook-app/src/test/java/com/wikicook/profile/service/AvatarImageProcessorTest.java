package com.wikicook.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * T054 (research R7, NFR-004, SC-003): what the server does to an uploaded avatar before it is stored. The samples in
 * {@code src/test/resources/avatars/} come from the spike T007 (JPEG CMYK, PNG with alpha, WebP lossy, lossless and
 * animated, a JPEG stored rotated, a JPEG with a GPS position).
 */
class AvatarImageProcessorTest {

    private final AvatarImageProcessor processor = new AvatarImageProcessor();

    private static byte[] sample(String name) throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/avatars", name));
    }

    private static BufferedImage decode(byte[] jpeg) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(jpeg));
    }

    private static byte[] png(int width, int height, int argb) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, argb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private ProfileErrorCode refusal(byte[] content) {
        return org.junit.jupiter.api.Assertions.assertThrows(ProfileException.class, () -> processor.process(content)).code();
    }

    @ParameterizedTest
    @ValueSource(strings = {"plain.jpg", "cmyk.jpg", "alpha.png", "lossy.webp", "lossless.webp", "alpha-lossless.webp",
            "animated.webp", "orientation6.jpg", "gps.jpg"})
    @DisplayName("US2-3: every format of the spike comes out as a 512 x 512 JPEG")
    void everySampleBecomesASquareJpeg(String name) throws IOException {
        byte[] out = processor.process(sample(name));

        assertThat(out[0] & 0xFF).isEqualTo(0xFF);
        assertThat(out[1] & 0xFF).isEqualTo(0xD8);
        BufferedImage image = decode(out);
        assertThat(image.getWidth()).isEqualTo(512);
        assertThat(image.getHeight()).isEqualTo(512);
    }

    @Test
    @DisplayName("R7 step 5: transparent pixels are laid on a white background")
    void alphaBecomesWhite() throws IOException {
        byte[] out = processor.process(png(64, 64, 0x00000000)); // fully transparent black

        BufferedImage image = decode(out);
        int rgb = image.getRGB(256, 256);
        assertThat((rgb >> 16) & 0xFF).isGreaterThan(240);
        assertThat((rgb >> 8) & 0xFF).isGreaterThan(240);
        assertThat(rgb & 0xFF).isGreaterThan(240);
    }

    @Test
    @DisplayName("R7 step 5: a picture that is not square is cut square from the middle")
    void wideImageIsCroppedFromTheMiddle() throws IOException {
        BufferedImage wide = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 300; x++) {
            for (int y = 0; y < 100; y++) {
                wide.setRGB(x, y, x < 100 ? 0xFF0000 : x < 200 ? 0x00FF00 : 0x0000FF); // red | green | blue thirds
            }
        }
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(wide, "png", encoded);

        BufferedImage image = decode(processor.process(encoded.toByteArray()));

        int centre = image.getRGB(256, 256);
        assertThat((centre >> 8) & 0xFF).as("the middle third (green) is what stays").isGreaterThan(200);
        int left = image.getRGB(10, 256);
        assertThat((left >> 8) & 0xFF).as("the red third was cut away, the left edge is green").isGreaterThan(200);
    }

    @Test
    @DisplayName("R7 step 5: EXIF Orientation 6 is applied - the red corner of the stored picture ends up top right")
    void orientationIsApplied() throws IOException {
        BufferedImage image = decode(processor.process(sample("orientation6.jpg")));

        int topRight = image.getRGB(450, 20);
        int topLeft = image.getRGB(60, 20);
        assertThat((topRight >> 16) & 0xFF).as("red channel top right").isGreaterThan(180);
        assertThat((topRight >> 8) & 0xFF).as("green channel top right").isLessThan(90);
        assertThat((topLeft >> 16) & 0xFF).as("top left is not the red corner").isLessThan(180);
    }

    @Test
    @DisplayName("Edge: Avatar not an image / NFR-004: the GPS position of the original does not survive")
    void exifIsGone() throws IOException {
        byte[] original = sample("gps.jpg");
        assertThat(new String(original, StandardCharsets.ISO_8859_1)).as("the sample really has EXIF").contains("Exif");

        byte[] out = processor.process(original);

        String text = new String(out, StandardCharsets.ISO_8859_1);
        assertThat(text).doesNotContain("Exif").doesNotContain("GPS");
    }

    @Test
    @DisplayName("NFR-004: bytes hidden after a valid image (a polyglot) are not in the stored file")
    void appendedScriptIsDropped() throws IOException {
        byte[] jpeg = sample("plain.jpg");
        byte[] hidden = "<script>alert(document.cookie)</script><?php system($_GET['x']); ?>".getBytes(StandardCharsets.UTF_8);
        byte[] polyglot = Arrays.copyOf(jpeg, jpeg.length + hidden.length);
        System.arraycopy(hidden, 0, polyglot, jpeg.length, hidden.length);

        byte[] out = processor.process(polyglot);

        assertThat(new String(out, StandardCharsets.ISO_8859_1)).doesNotContain("<script").doesNotContain("<?php");
        assertThat(decode(out).getWidth()).isEqualTo(512);
    }

    @Test
    @DisplayName("Edge: Avatar not an image - a text file renamed .png is 415 AVATAR_TYPE_NOT_ALLOWED")
    void textIsNotAllowed() {
        assertThat(refusal("this is not a picture".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(ProfileErrorCode.AVATAR_TYPE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("SC-003: other formats and empty input are all refused by their magic bytes")
    void otherFormatsAreRefused() {
        byte[][] files = {
                "GIF89a\u0001\u0000\u0001\u0000".getBytes(StandardCharsets.ISO_8859_1),
                "BM\u0000\u0000\u0000\u0000".getBytes(StandardCharsets.ISO_8859_1),
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8),
                "%PDF-1.7 fake".getBytes(StandardCharsets.UTF_8),
                {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0}, // a Windows executable
                {'P', 'K', 3, 4, 20, 0}, // a zip
                {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'}, // RIFF but not WebP
                {(byte) 0xFF, (byte) 0xD8}, // one byte short of a JPEG signature
                {1},
                {},
        };
        for (byte[] file : files) {
            assertThat(refusal(file)).as("file starting %s", Arrays.toString(Arrays.copyOf(file, Math.min(file.length, 4))))
                    .isEqualTo(ProfileErrorCode.AVATAR_TYPE_NOT_ALLOWED);
        }
        assertThatThrownBy(() -> processor.process(null)).isInstanceOf(ProfileException.class);
    }

    @Test
    @DisplayName("Edge: Avatar not an image - the right signature on bytes that do not decode is 422 AVATAR_NOT_AN_IMAGE")
    void signatureWithoutAnImageIsNotAnImage() {
        byte[] fakeJpeg = new byte[200];
        fakeJpeg[0] = (byte) 0xFF;
        fakeJpeg[1] = (byte) 0xD8;
        fakeJpeg[2] = (byte) 0xFF;
        byte[] fakePng = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        byte[] fakeWebp = {'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P'};

        assertThat(refusal(fakeJpeg)).isEqualTo(ProfileErrorCode.AVATAR_NOT_AN_IMAGE);
        assertThat(refusal(fakePng)).isEqualTo(ProfileErrorCode.AVATAR_NOT_AN_IMAGE);
        assertThat(refusal(fakeWebp)).isEqualTo(ProfileErrorCode.AVATAR_NOT_AN_IMAGE);
    }

    @Test
    @DisplayName("Edge: Avatar not an image - a picture cut in half is 422 AVATAR_NOT_AN_IMAGE")
    void truncatedImageIsNotAnImage() throws IOException {
        byte[] jpeg = sample("plain.jpg");

        assertThat(refusal(Arrays.copyOf(jpeg, jpeg.length / 2))).isEqualTo(ProfileErrorCode.AVATAR_NOT_AN_IMAGE);
    }

    @Test
    @DisplayName("R7 step 3: more than 6000 pixels on a side is 422 AVATAR_DIMENSIONS_TOO_LARGE, in either direction")
    void tooManyPixelsIsRefused() throws IOException {
        assertThat(refusal(png(6001, 1, 0xFFFFFFFF))).isEqualTo(ProfileErrorCode.AVATAR_DIMENSIONS_TOO_LARGE);
        assertThat(refusal(png(1, 6001, 0xFFFFFFFF))).isEqualTo(ProfileErrorCode.AVATAR_DIMENSIONS_TOO_LARGE);
    }

    @Test
    @DisplayName("R7 step 3: the size is read before the picture is decoded, so a huge header costs no memory")
    void dimensionsAreCheckedBeforeDecoding() throws IOException {
        // 20000 x 20000 would need 1.6 GB decoded; the IHDR says so and the pixel data is a single empty block
        byte[] header = png(6001, 1, 0xFFFFFFFF);
        patchPngSize(header, 20_000, 20_000);

        assertThat(refusal(header)).isEqualTo(ProfileErrorCode.AVATAR_DIMENSIONS_TOO_LARGE);
    }

    @Test
    @DisplayName("R7 step 1: more than 5 MiB is 413 AVATAR_TOO_LARGE, checked first")
    void tooBigIsRefusedFirst() {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;

        assertThat(refusal(big)).isEqualTo(ProfileErrorCode.AVATAR_TOO_LARGE);
    }

    /** Rewrites width and height in the IHDR chunk and fixes its CRC, so the reader believes the new size. */
    private static void patchPngSize(byte[] png, int width, int height) {
        int ihdr = 8 + 8; // signature, then length + type of the first chunk
        putInt(png, ihdr, width);
        putInt(png, ihdr + 4, height);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(png, ihdr - 4, 4 + 13);
        putInt(png, ihdr + 13, (int) crc.getValue());
    }

    private static void putInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }
}
