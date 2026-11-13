package com.wikicook.auth.mail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Fills the email templates (research R8). Each email is a pair {@code mail/<name>.html} and {@code mail/<name>.txt}
 * in Vietnamese, with {@code ${variable}} placeholders. Values are HTML-escaped in the {@code .html} version and left
 * as they are in the {@code .txt} one. Replacement is a single pass, so a value that looks like a placeholder is not
 * expanded. A missing variable is an error rather than an empty hole in a mail. No template takes a password.
 */
@Component
public class MailRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");

    private static final Map<String, String> SUBJECTS = Map.of(
            "verify-email", "Xác minh email tài khoản WikiCook");

    private final Map<String, String> templates = new ConcurrentHashMap<>();

    public Rendered render(String template, Map<String, String> variables) {
        String subject = SUBJECTS.get(template);
        if (subject == null) {
            throw new IllegalArgumentException("unknown mail template: " + template);
        }
        return new Rendered(subject, fill(load(template, "html"), variables, true),
                fill(load(template, "txt"), variables, false));
    }

    private String load(String template, String extension) {
        return templates.computeIfAbsent(template + "." + extension, name -> {
            try (InputStream in = new ClassPathResource("mail/" + name).getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalArgumentException("unknown mail template: " + template, e);
            }
        });
    }

    private static String fill(String text, Map<String, String> variables, boolean escape) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = variables.get(matcher.group(1));
            if (value == null) {
                throw new IllegalArgumentException("missing mail variable: " + matcher.group(1));
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(escape ? HtmlUtils.htmlEscape(value, "UTF-8") : value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public record Rendered(String subject, String html, String text) {
    }
}
