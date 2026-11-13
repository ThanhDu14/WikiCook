package com.wikicook.auth.security;

import com.wikicook.search.port.SearchViewerPort;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Who is searching, for 020: the signed-in user's id, or empty for a guest, so a guest sees unfiltered results
 * (020 acceptance 3.4). It reads only the security context, never a header. Being an application bean it wins over the
 * stand-ins of {@code SearchPortsConfig} ({@code @ConditionalOnMissingBean}).
 */
@Component
public class AuthSearchViewer implements SearchViewerPort {

    @Override
    public Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AccountAuthentication account) {
            return Optional.of(account.getPrincipal().userId());
        }
        return Optional.empty();
    }
}
