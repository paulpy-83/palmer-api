package com.palmar.palmer.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "samba")
@Getter
@Setter
public class SambaProperties {
    private String host;
    private String share;
    private String folder;
    private String user;
    private String password;
    private String domain;
}
