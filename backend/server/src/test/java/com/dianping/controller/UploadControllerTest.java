package com.dianping.controller;

import com.dianping.config.UploadProperties;
import com.dianping.constant.RedisConstants;
import com.dianping.dto.UserDTO;
import com.dianping.utils.UserHolder;
import com.dianping.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;

class UploadControllerTest {
    @TempDir
    Path tempDir;
    StringRedisTemplate redis;
    ValueOperations<String, String> values;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("1");
        UserDTO user = new UserDTO(); user.setId(1L); UserHolder.saveUser(user);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() { UserHolder.removeUser(); }

    @Test
    void storesImageUnderNestedBlogsPathAndReturnsPublicName() throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);

        Result<String> result = controller.uploadImage(file("cover.jpg", "image/jpeg", "jpg"));

        assertThat(result.getCode()).isEqualTo(1);
        String name = result.getData();
        assertThat(name).matches("/blogs/[0-9a-f]+/[0-9a-f]+/[0-9a-f-]+\\.jpg");
        assertThat(Files.exists(tempDir.resolve(name.substring(1)))).isTrue();
    }

    @Test
    void rejectsInvalidUploadsAndTraversalDeletes() throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);

        Result<String> missingName = controller.uploadImage(new MockMultipartFile("file", null, "image/jpeg", pngBytes()));
        Result<String> unsupported = controller.uploadImage(new MockMultipartFile("file", "x.exe", "application/octet-stream", pngBytes()));
        Path outside = tempDir.getParent().resolve("outside-task8.txt");
        Files.writeString(outside, "keep");
        Result<Void> traversal = controller.deleteBlogImg("/blogs/..\\..\\" + outside.getFileName());

        assertThat(missingName.getCode()).isZero();
        assertThat(unsupported.getCode()).isZero();
        assertThat(traversal.getCode()).isZero();
        assertThat(Files.exists(outside)).isTrue();
        Files.deleteIfExists(outside);
    }

    @Test
    void acceptsRealPngJpegGifAndRejectsForgedJpg() {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);

        assertThat(controller.uploadImage(file("a.png", "image/png", "png")).getCode()).isEqualTo(1);
        assertThat(controller.uploadImage(file("b.jpg", "image/jpeg", "jpg")).getCode()).isEqualTo(1);
        assertThat(controller.uploadImage(file("c.gif", "image/gif", "gif")).getCode()).isEqualTo(1);
        assertThat(controller.uploadImage(new MockMultipartFile("file", "fake.jpg", "image/jpeg", "not-an-image".getBytes())).getCode()).isZero();
    }

    @Test
    void deletesLegacyImgsPathAndRejectsWindowsAbsolutePath() throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);
        Path nested = tempDir.resolve("blogs/1/2/legacy.jpg");
        Files.createDirectories(nested.getParent());
        Files.writeString(nested, "x");

        assertThat(controller.deleteBlogImg("/imgs/blogs/1/2/legacy.jpg").getCode()).isEqualTo(1);
        assertThat(controller.deleteBlogImg("C:\\outside\\file.jpg").getCode()).isZero();
    }

    @Test
    void doesNotOverwriteExistingGeneratedFile() throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);
        Result<String> result = controller.uploadImage(file("cover.png", "image/png", "png"));
        assertThat(result.getCode()).isEqualTo(1);
        String name = result.getData();
        assertThat(Files.size(tempDir.resolve(name.substring(1)))).isGreaterThan(0);
    }

    @Test
    void onlyOwnerCanDeleteAndMissingOwnerKeepsFile() throws Exception {
        UploadProperties properties = new UploadProperties(); properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);
        Path file = tempDir.resolve("blogs/1/2/owned.jpg"); Files.createDirectories(file.getParent()); Files.writeString(file, "x");
        when(values.get(RedisConstants.UPLOAD_OWNER_KEY + "blogs/1/2/owned.jpg")).thenReturn("2");
        assertThat(controller.deleteBlogImg("/blogs/1/2/owned.jpg").getCode()).isZero();
        assertThat(Files.exists(file)).isTrue();
        when(values.get(anyString())).thenReturn(null);
        assertThat(controller.deleteBlogImg("/blogs/1/2/owned.jpg").getCode()).isZero();
        assertThat(Files.exists(file)).isTrue();
    }

    @Test
    void rollsBackFileWhenOwnerRecordCannotBeWritten() throws Exception {
        doThrow(new RuntimeException("redis down")).when(values).set(anyString(), anyString(), anyLong(), any(java.util.concurrent.TimeUnit.class));
        UploadProperties properties = new UploadProperties(); properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);
        Result<String> result = controller.uploadImage(file("rollback.png", "image/png", "png"));
        assertThat(result.getCode()).isZero();
        assertThat(Files.walk(tempDir).noneMatch(Files::isRegularFile)).isTrue();
    }

    @Test
    void reportsSuccessWhenOwnerKeyCleanupFailsAfterFileDeletion() throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);
        Path file = tempDir.resolve("blogs/1/2/cleanup.jpg");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x");
        doThrow(new RuntimeException("redis down")).when(redis).delete(anyString());

        Result<Void> result = controller.deleteBlogImg("/blogs/1/2/cleanup.jpg");

        assertThat(result.getCode()).isEqualTo(1);
        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void keepsFileWhenOwnerLookupIsUnavailable() throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setDirectory(tempDir);
        UploadController controller = new UploadController(properties, redis);
        Path file = tempDir.resolve("blogs/1/2/lookup.jpg");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x");
        when(values.get(anyString())).thenThrow(new RuntimeException("redis down"));

        Result<Void> result = controller.deleteBlogImg("/blogs/1/2/lookup.jpg");

        assertThat(result.getCode()).isZero();
        assertThat(Files.exists(file)).isTrue();
    }

    private MockMultipartFile file(String name, String contentType, String format) {
        return new MockMultipartFile("file", name, contentType, bytes(format));
    }

    private byte[] bytes(String format) {
        try { return encoded(format); } catch (Exception e) { throw new RuntimeException(e); }
    }

    private byte[] pngBytes() { return bytes("png"); }

    private byte[] encoded(String format) throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xff336699);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
}
