package com.wikicook.shoppinglist.service;

/** The week has no available dish to shop for (spec 040 Edge Case 1). */
public class EmptyMealPlanException extends RuntimeException {
    public EmptyMealPlanException(String message) {
        super(message);
    }
}
