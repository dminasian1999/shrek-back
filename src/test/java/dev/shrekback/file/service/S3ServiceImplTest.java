package dev.shrekback.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class S3ServiceImplTest {
    AmazonS3 s3 = mock(AmazonS3.class);
    S3ServiceImpl service = new S3ServiceImpl(s3);

    @Test void rejectsSpoofedActiveContentWithoutUploading() {
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
        MockMultipartFile spoofed = new MockMultipartFile("file", "image.png", "image/png", "<svg onload=alert(1)></svg>".getBytes());
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.uploadFile(spoofed));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(s3);
    }

    @Test void ignoresOriginalNameAndMimeAndUsesConfiguredBucket() throws Exception {
        ReflectionTestUtils.setField(service, "bucketName", "configured-bucket");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        when(s3.getUrl(anyString(), anyString())).thenAnswer(invocation -> new URL("https://configured-bucket.s3.amazonaws.com/" + invocation.getArgument(1)));
        service.uploadFile(new MockMultipartFile("file", "../../overwrite.html", "text/html", bytes.toByteArray()));
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture());
        assertEquals("configured-bucket", request.getValue().getBucketName());
        assertTrue(request.getValue().getKey().matches("ecom-sevan/[a-f0-9-]+\\.png"));
        assertEquals("image/png", request.getValue().getMetadata().getContentType());
    }

    @Test void missingStorageConfigurationIsExplicitAndDeletionCannotEscapeAppPrefix() {
        ReflectionTestUtils.setField(service, "bucketName", "");
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.deleteFile("file.png")).getStatusCode().value());
        ReflectionTestUtils.setField(service, "bucketName", "configured-bucket");
        assertThrows(ResponseStatusException.class, () -> service.deleteFile("../other-site/index.html"));
        verifyNoInteractions(s3);
    }
}
