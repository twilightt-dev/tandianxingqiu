package com.dianping.controller;

import com.dianping.config.UploadProperties;
import com.dianping.constant.RedisConstants;
import com.dianping.dto.UserDTO;
import com.dianping.result.Result;
import com.dianping.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@RestController
@RequestMapping("upload")
@Tag(name = "文件上传接口")
public class UploadController {

    private final Path baseDirectory;
    private final StringRedisTemplate stringRedisTemplate;

    public UploadController(UploadProperties properties, StringRedisTemplate stringRedisTemplate) {
        Path configured = properties.getDirectory();
        if (configured == null) throw new IllegalArgumentException("上传目录未配置");
        this.baseDirectory = configured.toAbsolutePath().normalize();
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostMapping("blog")
    @Operation(summary = "上传博客图片")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile image) {
        if (image == null || image.isEmpty()) return Result.error("请选择图片文件");
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null) return Result.error("请先登录");
        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) return Result.error("图片文件名不能为空");
        String suffix = suffixOf(originalFilename);
        if (suffix == null || !isSupported(suffix)) return Result.error("仅支持 jpg、jpeg、png、gif 图片");
        if (image.getContentType() == null || !image.getContentType().toLowerCase(Locale.ROOT).startsWith("image/")) return Result.error("文件内容类型必须为图片");
        try {
            byte[] bytes = image.getBytes();
            if (!isRealImage(bytes, suffix)) return Result.error("图片内容无效");
            String name = createNewFileName(suffix);
            Path target = safeResolve(name);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                StringRedisTemplate redis = stringRedisTemplate;
                redis.opsForValue().set(ownerKey(name), currentUser.getId().toString(), RedisConstants.UPLOAD_OWNER_TTL, TimeUnit.HOURS);
            } catch (RuntimeException redisFailure) {
                Files.deleteIfExists(target);
                return Result.error("上传服务暂不可用，请稍后重试");
            }
            log.debug("文件上传成功，{}", name);
            return Result.success(name);
        } catch (IOException e) {
            return Result.error("文件上传失败");
        }
    }

    @GetMapping("/blog/delete")
    @Operation(summary = "删除博客图片")
    public Result<Void> deleteBlogImg(@RequestParam("name") String filename) {
        if (filename == null || filename.isBlank()) return Result.error("错误的文件名称");
        String relative = filename.startsWith("/imgs/") ? filename.substring("/imgs/".length()) : filename;
        try {
            Path file = safeResolve(relative);
            UserDTO currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getId() == null) return Result.error("请先登录");
            String owner;
            try {
                owner = stringRedisTemplate.opsForValue().get(ownerKey(relative));
            } catch (RuntimeException redisFailure) {
                return Result.error("删除服务暂不可用，请稍后重试");
            }
            if (owner == null || !owner.equals(currentUser.getId().toString())) return Result.error("无权删除该图片");
            if (Files.isDirectory(file) || !Files.exists(file)) return Result.error("错误的文件名称");
            Files.delete(file);
            try {
                stringRedisTemplate.delete(ownerKey(relative));
            } catch (RuntimeException cleanupFailure) {
                log.warn("图片已删除，但所有权记录清理失败，{}", relative, cleanupFailure);
            }
            return Result.success();
        } catch (IOException | IllegalArgumentException e) {
            return Result.error("错误的文件名称");
        }
    }

    private String createNewFileName(String suffix) {
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        return String.format(Locale.ROOT, "/blogs/%x/%x/%s.%s", hash & 0xF, (hash >> 4) & 0xF, name, suffix);
    }

    private Path safeResolve(String relative) {
        String clean = relative.replace('\\', '/');
        while (clean.startsWith("/")) clean = clean.substring(1);
        Path resolved = baseDirectory.resolve(clean).normalize();
        if (!resolved.startsWith(baseDirectory)) throw new IllegalArgumentException("路径越界");
        return resolved;
    }

    private String ownerKey(String relative) {
        String clean = relative.replace('\\', '/');
        while (clean.startsWith("/")) clean = clean.substring(1);
        return RedisConstants.UPLOAD_OWNER_KEY + clean;
    }

    private String suffixOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return null;
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isSupported(String suffix) {
        return suffix.equals("jpg") || suffix.equals("jpeg") || suffix.equals("png") || suffix.equals("gif");
    }

    private boolean isRealImage(byte[] bytes, String suffix) throws IOException {
        if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) return false;
        if (suffix.equals("png")) return bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        if (suffix.equals("jpg") || suffix.equals("jpeg")) return bytes.length >= 2 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8;
        return bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
    }
}
