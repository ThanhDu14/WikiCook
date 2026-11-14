package com.wikicook.review.service;

import com.wikicook.review.exception.CooksnapProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageProcessorServiceTest {

    private ImageProcessorService imageProcessorService;

    @BeforeEach
    void setUp() {
        imageProcessorService = new ImageProcessorService();
    }

    private byte[] createSampleImageBytes(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Process valid JPEG image: strips metadata, encodes to WebP, generates thumbnail")
    void testProcessValidJpeg() throws Exception {
        byte[] jpegBytes = createSampleImageBytes(800, 600, "jpg");

        ImageProcessorService.ProcessedImageResult result = imageProcessorService.processImage(jpegBytes, "image/jpeg");

        assertThat(result).isNotNull();
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.fullImageBytes()).isNotEmpty();
        assertThat(result.thumbnailBytes()).isNotEmpty();

        // Check thumbnail dimensions <= 400x400
        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(result.thumbnailBytes()));
        assertThat(thumb).isNotNull();
        assertThat(thumb.getWidth()).isLessThanOrEqualTo(400);
        assertThat(thumb.getHeight()).isLessThanOrEqualTo(400);
    }

    @Test
    @DisplayName("Process valid PNG image successfully")
    void testProcessValidPng() throws Exception {
        byte[] pngBytes = createSampleImageBytes(500, 500, "png");

        ImageProcessorService.ProcessedImageResult result = imageProcessorService.processImage(pngBytes, "image/png");

        assertThat(result).isNotNull();
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.fullImageBytes()).isNotEmpty();
        assertThat(result.thumbnailBytes()).isNotEmpty();
    }

    @Test
    @DisplayName("Reject spoofed or invalid file with CooksnapProcessingException")
    void testRejectSpoofedFile() {
        byte[] fakeData = "This is not an image file".getBytes();

        assertThatThrownBy(() -> imageProcessorService.processImage(fakeData, "image/jpeg"))
                .isInstanceOf(CooksnapProcessingException.class)
                .hasMessageContaining("Định dạng file không hợp lệ hoặc dữ liệu ảnh bị hỏng");
    }

    @Test
    @DisplayName("Reject unsupported MIME type")
    void testRejectUnsupportedMimeType() {
        byte[] validJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

        assertThatThrownBy(() -> imageProcessorService.processImage(validJpeg, "application/pdf"))
                .isInstanceOf(CooksnapProcessingException.class)
                .hasMessageContaining("Định dạng file không được hỗ trợ");
    }

    @Test
    @DisplayName("Resize large image exceeding 1920x1080 bounds")
    void testResizeLargeImage() throws Exception {
        byte[] largeImageBytes = createSampleImageBytes(2400, 1600, "jpg");

        ImageProcessorService.ProcessedImageResult result = imageProcessorService.processImage(largeImageBytes, "image/jpeg");

        BufferedImage fullImg = ImageIO.read(new ByteArrayInputStream(result.fullImageBytes()));
        assertThat(fullImg.getWidth()).isLessThanOrEqualTo(1920);
        assertThat(fullImg.getHeight()).isLessThanOrEqualTo(1080);
    }
}

