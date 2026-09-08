package com.dianping.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = UploadPropertiesTest.Config.class)
@TestPropertySource(properties = "app.upload.directory=C:/tmp/task8-uploads")
class UploadPropertiesTest {
    @Autowired
    UploadProperties properties;

    @Test
    void bindsDirectoryOverrideAsPath() {
        assertThat(properties.getDirectory()).isEqualTo(Path.of("C:/tmp/task8-uploads"));
    }

    @EnableConfigurationProperties(UploadProperties.class)
    static class Config { }
}
