package com.kazka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kazka.uploads")
public class UploadsProperties {
    private String dir = "./uploads";

    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }
}
