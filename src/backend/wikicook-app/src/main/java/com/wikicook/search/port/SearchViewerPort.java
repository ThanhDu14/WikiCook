package com.wikicook.search.port;

import java.util.Optional;

/**
 * Who is searching (020 research R6). Empty = guest: nothing is hidden by a dietary profile. Spec 010 will add an
 * adapter on the authenticated principal; until then the dev stub reads a header.
 */
public interface SearchViewerPort {

    Optional<Long> currentUserId();
}
