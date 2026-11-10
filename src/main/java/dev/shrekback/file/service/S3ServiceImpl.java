package dev.shrekback.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {
    private static final Map<String, String> TYPES = Map.of("png", "image/png", "jpeg", "image/jpeg", "gif", "image/gif");
    private final AmazonS3 s3;
    @Value("${aws.s3.bucketName:}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a PNG, JPEG or GIF image under 5 MB.");
        }
        requireBucket();
        try {
            byte[] content = file.getBytes();
            String format;
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalidImage();
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    if (!TYPES.containsKey(format) || (long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000) throw invalidImage();
                    if (reader.read(0) == null) throw invalidImage();
                } finally {
                    reader.dispose();
                }
            }
            String key = "ecom-sevan/" + UUID.randomUUID() + "." + (format.equals("jpeg") ? "jpg" : format);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(TYPES.get(format));
            metadata.setContentLength(content.length);
            metadata.setCacheControl("public, max-age=31536000, immutable");
            try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
                s3.putObject(new PutObjectRequest(bucketName, key, input, metadata));
            }
            return s3.getUrl(bucketName, key).toString();
        } catch (ResponseStatusException error) {
            throw error;
        } catch (java.io.IOException error) {
            throw invalidImage();
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image storage is unavailable. Please try again.");
        }
    }

    @Override
    public void deleteFile(String key) {
        requireBucket();
        // This endpoint can only address this application's generated image keys.
        if (key == null || !key.matches("(?:ecom-sevan/)?[a-zA-Z0-9._-]+") || key.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image key.");
        }
        String objectKey = key.startsWith("ecom-sevan/") ? key : "ecom-sevan/" + key;
        try {
            s3.deleteObject(bucketName, objectKey);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image storage is unavailable. Please try again.");
        }
    }

    private void requireBucket() {
        if (bucketName == null || bucketName.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Image storage is not configured.");
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid PNG, JPEG or GIF image with no more than 40 megapixels.");
    }
}
