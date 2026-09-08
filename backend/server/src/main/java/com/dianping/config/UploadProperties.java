package com.dianping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private Path directory;

    public Path getDirectory() { return directory; }
    public void setDirectory(Path directory) { this.directory = directory; }
}
