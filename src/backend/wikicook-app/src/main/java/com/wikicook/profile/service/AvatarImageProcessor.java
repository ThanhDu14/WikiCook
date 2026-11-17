package com.wikicook.profile.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Component;

/**
 * What the server does to an uploaded avatar before it is stored (research R7, NFR-004). The bytes of the client are
 * never kept: the picture is decoded for real and a new JPEG is written, so EXIF (the GPS position included), appended
 * scripts and the tricks of polyglot files are gone. The steps, in the order that costs least first:
 * <ol>
 * <li>at most 5 MiB, else {@code AVATAR_TOO_LARGE};
 * <li>magic bytes of JPEG, PNG or {@code RIFF…WEBP}, whatever the file name or {@code Content-Type} says, else
 *     {@code AVATAR_TYPE_NOT_ALLOWED};
 * <li>the size is read from the header and refused above 6000 x 6000, before a single pixel is decoded, else
 *     {@code AVATAR_DIMENSIONS_TOO_LARGE};
 * <li>a real decode, else {@code AVATAR_NOT_AN_IMAGE} (a decoder warning, such as a cut-off file, counts);
 * <li>EXIF Orientation of a JPEG applied, the picture cut square from the middle and scaled to 512 x 512 on a white
 *     background (transparent pixels become white);
 * <li>encoded as JPEG, quality 0.85, without metadata.
 * </ol>
 * It runs outside the database transaction. Stateless and thread-safe.
 */
@Component
public class AvatarImageProcessor {

    static final int MAX_BYTES = 5 * 1024 * 1024;
    static final int MAX_SIDE = 6000;
    static final int SIZE = 512;
    private static final float QUALITY = 0.85f;
    private static final int EXIF_ORIENTATION_TAG = 0x0112;

    private enum Format {
        JPEG, PNG, WEBP
    }

    static {
        ImageIO.setUseCache(false); // decode in memory: no temporary file per upload
        ImageIO.scanForPlugins(); // the WebP reader lives in a jar of the application; see readerFor
    }

    private static final AtomicBoolean RESCANNED = new AtomicBoolean();

    /** @return the avatar as a 512 x 512 JPEG */
    public byte[] process(byte[] content) {
        if (content != null && content.length > MAX_BYTES) {
            throw new ProfileException(ProfileErrorCode.AVATAR_TOO_LARGE);
        }
        Format format = detect(content);
        BufferedImage decoded = decode(content);
        int orientation = format == Format.JPEG ? exifOrientation(content) : 1;
        return encode(squareThumbnail(upright(decoded, orientation)));
    }

    private static Format detect(byte[] content) {
        if (content == null || content.length < 12 && !startsWithJpeg(content)) {
            throw new ProfileException(ProfileErrorCode.AVATAR_TYPE_NOT_ALLOWED);
        }
        if (startsWithJpeg(content)) {
            return Format.JPEG;
        }
        if (content.length >= 8 && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G'
                && content[4] == 0x0D && content[5] == 0x0A && content[6] == 0x1A && content[7] == 0x0A) {
            return Format.PNG;
        }
        if (content.length >= 12 && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return Format.WEBP;
        }
        throw new ProfileException(ProfileErrorCode.AVATAR_TYPE_NOT_ALLOWED);
    }

    private static boolean startsWithJpeg(byte[] content) {
        return content != null && content.length >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF;
    }

    /** Steps 3 and 4: the size from the header first, then the decode. */
    private static BufferedImage decode(byte[] content) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            ImageReader reader = readerFor(in);
            try {
                boolean[] warned = {false};
                IIOReadWarningListener listener = (source, warning) -> warned[0] = true;
                reader.addIIOReadWarningListener(listener);
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_SIDE || height > MAX_SIDE) {
                    throw new ProfileException(ProfileErrorCode.AVATAR_DIMENSIONS_TOO_LARGE);
                }
                BufferedImage image = reader.read(0);
                if (image == null || warned[0] || width < 1 || height < 1) {
                    throw new ProfileException(ProfileErrorCode.AVATAR_NOT_AN_IMAGE);
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (ProfileException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ProfileException(ProfileErrorCode.AVATAR_NOT_AN_IMAGE);
        }
    }

    /**
     * The reader the registry offers for these bytes. The WebP reader comes from a jar nested in the application, and
     * the registry of a thread group that never scanned would not know it, so one rescan is made before giving up.
     */
    private static ImageReader readerFor(ImageInputStream in) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
        if (!readers.hasNext() && RESCANNED.compareAndSet(false, true)) {
            ImageIO.scanForPlugins();
            in.seek(0);
            readers = ImageIO.getImageReaders(in);
        }
        if (!readers.hasNext()) {
            throw new ProfileException(ProfileErrorCode.AVATAR_NOT_AN_IMAGE);
        }
        return readers.next();
    }

    /**
     * The Orientation of the EXIF block of a JPEG (1 when there is none or it can not be read). ImageIO decodes the
     * pixels as stored, so a photo from a phone would otherwise come out turned (research R7, step 5).
     */
    static int exifOrientation(byte[] jpeg) {
        try {
            int position = 2; // after SOI
            while (position + 4 <= jpeg.length && (jpeg[position] & 0xFF) == 0xFF) {
                int marker = jpeg[position + 1] & 0xFF;
                if (marker == 0xDA || marker == 0xD9) {
                    return 1; // image data or end: no EXIF came first
                }
                int length = ((jpeg[position + 2] & 0xFF) << 8) | (jpeg[position + 3] & 0xFF);
                if (marker == 0xE1 && length >= 14 && position + 4 + 6 <= jpeg.length && jpeg[position + 4] == 'E'
                        && jpeg[position + 5] == 'x' && jpeg[position + 6] == 'i' && jpeg[position + 7] == 'f') {
                    return readTiffOrientation(jpeg, position + 10, Math.min(jpeg.length, position + 2 + length));
                }
                position += 2 + length;
            }
        } catch (RuntimeException e) {
            // a broken EXIF block is not a reason to refuse the picture
        }
        return 1;
    }

    private static int readTiffOrientation(byte[] data, int tiff, int end) {
        if (tiff + 8 > end) {
            return 1;
        }
        boolean little = data[tiff] == 'I' && data[tiff + 1] == 'I';
        if (!little && !(data[tiff] == 'M' && data[tiff + 1] == 'M')) {
            return 1;
        }
        int ifd = tiff + (int) readInt(data, tiff + 4, 4, little);
        if (ifd < tiff || ifd + 2 > end) {
            return 1;
        }
        int entries = (int) readInt(data, ifd, 2, little);
        for (int i = 0; i < entries; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > end) {
                return 1;
            }
            if (readInt(data, entry, 2, little) == EXIF_ORIENTATION_TAG) {
                int value = (int) readInt(data, entry + 8, 2, little);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static long readInt(byte[] data, int offset, int bytes, boolean little) {
        long value = 0;
        for (int i = 0; i < bytes; i++) {
            int b = data[offset + (little ? bytes - 1 - i : i)] & 0xFF;
            value = (value << 8) | b;
        }
        return value;
    }

    /** The picture as stored turned the way the camera meant it, on white, so there is no alpha from here on. */
    private static BufferedImage upright(BufferedImage source, int orientation) {
        boolean opaque = source.getType() == BufferedImage.TYPE_INT_RGB || source.getType() == BufferedImage.TYPE_3BYTE_BGR;
        if (orientation == 1 && opaque) {
            return source; // the common photo: no second copy of a picture that can be 6000 x 6000
        }
        int w = source.getWidth();
        int h = source.getHeight();
        boolean swapped = orientation >= 5;
        BufferedImage out = new BufferedImage(swapped ? h : w, swapped ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
            g.drawImage(source, transform(orientation, w, h), null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static AffineTransform transform(int orientation, int w, int h) {
        return switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0); // mirrored left to right
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h); // upside down
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h); // mirrored top to bottom
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0); // transposed
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0); // turned 90 degrees clockwise to display
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w); // transversed
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w); // turned 90 degrees counter-clockwise to display
            default -> new AffineTransform();
        };
    }

    /** Cut square from the middle, halve while far bigger (a plain shrink of 6000 px would alias), end at 512 x 512. */
    private static BufferedImage squareThumbnail(BufferedImage image) {
        int side = Math.min(image.getWidth(), image.getHeight());
        BufferedImage current = image.getSubimage((image.getWidth() - side) / 2, (image.getHeight() - side) / 2, side, side);
        while (current.getWidth() >= 2 * SIZE) {
            current = scaled(current, current.getWidth() / 2);
        }
        return scaled(current, SIZE);
    }

    private static BufferedImage scaled(BufferedImage image, int side) {
        BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(image, 0, 0, side, side, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static byte[] encode(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG writer");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param); // no metadata object: nothing of the original
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Can not encode the avatar", e);
        } finally {
            writer.dispose();
        }
    }
}
