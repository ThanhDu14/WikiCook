package com.wikicook.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * The application with its Google sign-in switched on and pointed at {@link FakeGoogle} (spec 010 T141-T143). The
 * client id and secret are fakes; the registration is replaced by one that points at the WireMock server. Every IT with
 * this annotation shares one Spring context.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, FakeGoogle.Registration.class})
@TestPropertySource(properties = {
    "wikicook.auth.google.client-id=" + FakeGoogle.CLIENT_ID,
    "wikicook.auth.google.client-secret=" + FakeGoogle.CLIENT_SECRET})
public @interface GoogleIntegrationTest {
}
