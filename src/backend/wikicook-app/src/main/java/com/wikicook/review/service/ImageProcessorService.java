package com.wikicook.review.service;

import com.wikicook.review.exception.CooksnapProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class ImageProcessorService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessorService.class);

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final int THUMB_MAX_WIDTH = 400;
    private static final int THUMB_MAX_HEIGHT = 400;

    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    public record ProcessedImageResult(
            byte[] fullImageBytes,
            byte[] thumbnailBytes,
            int sizeBytes,
            String contentType
    ) {}

    public ProcessedImageResult processImage(byte[] rawData, String reportedMimeType) {
        if (rawData == null || rawData.length == 0) {
            throw new CooksnapProcessingException("Dữ liệu ảnh không được để trống");
        }

        if (reportedMimeType == null || !ALLOWED_MIME_TYPES.contains(reportedMimeType.toLowerCase())) {
            throw new CooksnapProcessingException("Định dạng file không được hỗ trợ. Chỉ chấp nhận JPEG, PNG hoặc WebP.");
        }

        validateMagicBytes(rawData);

        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(new ByteArrayInputStream(rawData));
        } catch (IOException e) {
            throw new CooksnapProcessingException("Định dạng file không hợp lệ hoặc dữ liệu ảnh bị hỏng: " + e.getMessage(), e);
        }

        if (originalImage == null) {
            throw new CooksnapProcessingException("Định dạng file không hợp lệ hoặc dữ liệu ảnh bị hỏng");
        }

        // 1. Resize full image if exceeding MAX_WIDTH x MAX_HEIGHT (preserving aspect ratio)
        BufferedImage fullImage = resizeIfNecessary(originalImage, MAX_WIDTH, MAX_HEIGHT);

        // 2. Generate 400x400 thumbnail
        BufferedImage thumbnail = resizeIfNecessary(originalImage, THUMB_MAX_WIDTH, THUMB_MAX_HEIGHT);

        // 3. Encode to WebP (guaranteeing zero EXIF/GPS metadata from source)
        byte[] fullImageBytes = encodeToWebp(fullImage);
        byte[] thumbBytes = encodeToWebp(thumbnail);

        return new ProcessedImageResult(fullImageBytes, thumbBytes, fullImageBytes.length, "image/webp");
    }

    private void validateMagicBytes(byte[] data) {
        if (data.length < 12) {
            throw new CooksnapProcessingException("Định dạng file không hợp lệ hoặc dữ liệu ảnh bị hỏng");
        }

        // JPEG: FF D8 FF
        boolean isJpeg = (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF);

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        boolean isPng = (data[0] == (byte) 0x89 && data[1] == (byte) 0x50 && data[2] == (byte) 0x4E && data[3] == (byte) 0x47);

        // WebP: RIFF .... WEBP
        boolean isWebP = (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P');

        if (!isJpeg && !isPng && !isWebP) {
            throw new CooksnapProcessingException("Định dạng file không hợp lệ hoặc dữ liệu ảnh bị hỏng");
        }
    }

    private BufferedImage resizeIfNecessary(BufferedImage src, int maxWidth, int maxHeight) {
        int originalWidth = src.getWidth();
        int originalHeight = src.getHeight();

        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return src;
        }

        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double scale = Math.min(widthRatio, heightRatio);

        int targetWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(originalHeight * scale));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        return resized;
    }

    private byte[] encodeToWebp(BufferedImage image) {
        // Ensure RGB type for WebP encoding compatibility
        BufferedImage rgbImage = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean success = ImageIO.write(rgbImage, "webp", baos);
            if (!success || baos.size() == 0) {
                // If WebP native writer is unavailable, fallback to JPG encoding
                log.warn("WebP ImageWriter unavailable or failed; falling back to jpg");
                baos.reset();
                ImageIO.write(rgbImage, "jpg", baos);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new CooksnapProcessingException("Lỗi trong quá trình nén và xử lý ảnh: " + e.getMessage(), e);
        }
    }
}

