package com.wikicook.support;

import com.wikicook.common.security.AdministratorOnly;
import com.wikicook.common.security.CurrentAccount;
import com.wikicook.common.security.ModeratorOnly;
import com.wikicook.common.security.VerifiedEmailRequired;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Routes of a module that does not exist yet, outside {@code com.wikicook.auth} on purpose, so the tests exercise the
 * security of a foreign package (research R10, R15). A {@code @TestConfiguration} that is also a {@code @Controller}:
 * test configurations are not component-scanned, so it exists only where a test adds {@code @Import(MatrixProbeController.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
@Controller
@RequestMapping("/api/v1/probe")
@ResponseBody
public class MatrixProbeController {

    private final CurrentAccount account;

    public MatrixProbeController(CurrentAccount account) {
        this.account = account;
    }

    /** Needs a signed-in caller only. */
    @GetMapping("/whoami")
    public Map<String, Object> whoami(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .sorted().toList();
        return Map.of("id", account.id(), "role", account.role().name(), "emailVerified", account.emailVerified(),
                "sessionId", account.sessionId(), "authorities", authorities);
    }

    @GetMapping("/verified")
    @VerifiedEmailRequired
    public Map<String, Object> verified() {
        return Map.of("ok", true);
    }

    @GetMapping("/moderator")
    @ModeratorOnly
    public Map<String, Object> moderator() {
        return Map.of("ok", true);
    }

    /** Matrix row "review, rate, post cooksnaps": needs a verified email (FR-006); the module (070) does not exist yet. */
    @GetMapping("/review")
    @VerifiedEmailRequired
    public Map<String, Object> review() {
        return Map.of("ok", true);
    }

    /** Matrix row "submit recipes": also needs a verified email (060 does not exist yet). */
    @GetMapping("/submit")
    @VerifiedEmailRequired
    public Map<String, Object> submit() {
        return Map.of("ok", true);
    }

    /** The Contributor badge is data about the author, not a right: any signed-in user may ask, only Contributors have it. */
    @GetMapping("/badge")
    public Map<String, Object> badge() {
        return Map.of("verifiedContributor", account.role() == com.wikicook.common.security.Role.CONTRIBUTOR);
    }

    /** Matrix row "moderate recipes, reviews and reports" (100 does not exist yet). */
    @GetMapping("/moderate")
    @ModeratorOnly
    public Map<String, Object> moderate() {
        return Map.of("ok", true);
    }

    /** Matrix row "manage categories and taxonomies" (100 does not exist yet). */
    @GetMapping("/taxonomies")
    @AdministratorOnly
    public Map<String, Object> taxonomies() {
        return Map.of("ok", true);
    }

    @GetMapping("/administrator")
    @AdministratorOnly
    public Map<String, Object> administrator() {
        return Map.of("ok", true);
    }
}
