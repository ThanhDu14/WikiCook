package com.wikicook.support;

import com.wikicook.common.security.VerifiedEmailRequired;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * A controller of "another module": its package is not {@code com.wikicook.auth}, so only the global
 * {@code SecurityExceptionAdvice} can handle what it throws (research R15). Like {@link MatrixProbeController} it is a
 * {@code @TestConfiguration} controller, so no {@code @SpringBootTest} scans it; tests add it with {@code @Import}.
 */
@TestConfiguration(proxyBeanMethods = false)
@Controller
@RequestMapping("/api/v1/foreign")
@ResponseBody
public class ForeignModuleProbe {

    @GetMapping("/denied")
    public Map<String, Object> denied() {
        throw new AccessDeniedException("not allowed");
    }

    @GetMapping("/verified-only")
    @VerifiedEmailRequired
    public Map<String, Object> verifiedOnly() {
        throw new AccessDeniedException("method security said no");
    }
}
