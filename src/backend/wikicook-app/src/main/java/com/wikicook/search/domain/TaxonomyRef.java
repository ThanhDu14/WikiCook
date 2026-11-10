package com.wikicook.search.domain;

/** An active cuisine, category or METHOD tag chosen as a filter. */
public record TaxonomyRef(long id, String slug, String name) {
}
