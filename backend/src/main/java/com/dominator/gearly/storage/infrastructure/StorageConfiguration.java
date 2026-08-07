package com.dominator.gearly.storage.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code gearly.storage.*}. The adapter is a {@code @Component}. */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {
}
