package com.wikicook.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.auth.mail.MailRenderer.Rendered;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Research R8: one .html and one .txt per email, Vietnamese, variables replaced and escaped, no password anywhere. */
class MailRendererTest {

    private final MailRenderer renderer = new MailRenderer();

    private static final Map<String, String> VARIABLES = Map.of("name", "An", "link",
            "http://localhost:3000/verify-email#token=abc_DEF-123", "hours", "24");

    @Test
    void verifyEmailHasAnHtmlAndAPlainTextVersionInVietnamese() {
        Rendered mail = renderer.render("verify-email", VARIABLES);

        assertThat(mail.subject()).contains("WikiCook").containsIgnoringCase("xác minh");
        assertThat(mail.html()).contains("<html").contains("Xin chào An").contains("xác minh")
                .contains("href=\"http://localhost:3000/verify-email#token=abc_DEF-123\"");
        assertThat(mail.text()).doesNotContain("<").contains("Xin chào An").contains("xác minh")
                .contains("http://localhost:3000/verify-email#token=abc_DEF-123");
    }

    @Test
    void theExpiryIsStatedInTheMail() {
        Rendered mail = renderer.render("verify-email", VARIABLES);

        assertThat(mail.html()).contains("24 giờ");
        assertThat(mail.text()).contains("24 giờ");
    }

    @Test
    void variablesAreEscapedInHtmlButNotInPlainText() {
        Rendered mail = renderer.render("verify-email", Map.of("name", "<script>alert(1)</script> & \"Bé\"",
                "link", "http://localhost:3000/verify-email#token=x", "hours", "24"));

        assertThat(mail.html()).doesNotContain("<script>").contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("&amp;").contains("&quot;Bé&quot;");
        assertThat(mail.text()).contains("<script>alert(1)</script> & \"Bé\"");
    }

    @Test
    void aValueCannotSmuggleInAnotherPlaceholder() {
        Rendered mail = renderer.render("verify-email", Map.of("name", "${link}", "link",
                "http://localhost:3000/verify-email#token=x", "hours", "24"));

        assertThat(mail.html()).contains("Xin chào ${link}");
    }

    @Test
    void noMailAsksForOrShowsAPassword() {
        Rendered mail = renderer.render("verify-email", VARIABLES);

        assertThat(mail.html().toLowerCase()).doesNotContain("password").doesNotContain("mật khẩu của bạn");
        assertThat(mail.text().toLowerCase()).doesNotContain("password").doesNotContain("mật khẩu của bạn");
    }

    @Test
    void aMissingVariableIsAnErrorNotAnEmptyHole() {
        assertThatThrownBy(() -> renderer.render("verify-email", Map.of("name", "An")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("link");
    }

    @Test
    void anUnknownTemplateIsAnError() {
        assertThatThrownBy(() -> renderer.render("no-such-mail", Map.of())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-such-mail");
    }
}
