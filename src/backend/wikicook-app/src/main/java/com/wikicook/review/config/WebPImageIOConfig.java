package com.wikicook.review.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.imageio.ImageIO;

@Configuration
@EnableConfigurationProperties(ReviewStorageProperties.class)
public class WebPImageIOConfig {

    private static final Logger log = LoggerFactory.getLogger(WebPImageIOConfig.class);

    @PostConstruct
    public void registerImageIOPlugins() {
        ImageIO.scanForPlugins();
        log.info("Initialized ImageIO SPI plugins. Available reader formats: {}", 
                String.join(", ", ImageIO.getReaderFormatNames()));
    }
}

