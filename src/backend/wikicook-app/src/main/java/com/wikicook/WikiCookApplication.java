package com.wikicook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Scans every feature package under {@code com.wikicook}, including {@code com.wikicook.ai}. */
@SpringBootApplication
public class WikiCookApplication {

    public static void main(String[] args) {
        SpringApplication.run(WikiCookApplication.class, args);
    }
}
