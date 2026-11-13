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

    @GetMapping("/administrator")
    @AdministratorOnly
    public Map<String, Object> administrator() {
        return Map.of("ok", true);
    }
}
