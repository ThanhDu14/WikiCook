package com.wikicook.search.service;

import java.util.List;

/** Query parameters exactly as they arrived (020 contracts/openapi.yaml); nothing validated yet. */
public record RawSearchParams(
        String q,
        String time,
        List<String> cuisine,
        List<String> category,
        List<String> difficulty,
        List<String> method,
        String diet,
        String sort,
        String page,
        String showHidden) {

    public RawSearchParams {
        cuisine = cuisine == null ? List.of() : List.copyOf(cuisine);
        category = category == null ? List.of() : List.copyOf(category);
        difficulty = difficulty == null ? List.of() : List.copyOf(difficulty);
        method = method == null ? List.of() : List.copyOf(method);
    }

    public RawSearchParams withQ(String value) {
        return new RawSearchParams(value, time, cuisine, category, difficulty, method, diet, sort, page, showHidden);
    }

    public RawSearchParams withSort(String value) {
        return new RawSearchParams(q, time, cuisine, category, difficulty, method, diet, value, page, showHidden);
    }

    public RawSearchParams withDiet(String value) {
        return new RawSearchParams(q, time, cuisine, category, difficulty, method, value, sort, page, showHidden);
    }

    public RawSearchParams withPage(String value) {
        return new RawSearchParams(q, time, cuisine, category, difficulty, method, diet, sort, value, showHidden);
    }

    public RawSearchParams withCuisines(List<String> value) {
        return new RawSearchParams(q, time, value, category, difficulty, method, diet, sort, page, showHidden);
    }
}
