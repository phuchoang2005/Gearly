package com.dominator.bookify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "momo")
@Data
public class MomoConfig {
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String returnUrl;
    private String notifyUrl;
}
