package com.wikicook.search.dto;

import com.wikicook.search.domain.TaxonomyRef;

/** A cuisine, category or cooking method as shown on filters and chips. */
public record TaxonomyOptionDto(String slug, String name) {

    public static TaxonomyOptionDto of(TaxonomyRef ref) {
        return new TaxonomyOptionDto(ref.slug(), ref.name());
    }
}
