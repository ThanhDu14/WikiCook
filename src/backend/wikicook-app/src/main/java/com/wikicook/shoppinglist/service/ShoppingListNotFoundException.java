package com.wikicook.shoppinglist.service;

public class ShoppingListNotFoundException extends RuntimeException {
    public ShoppingListNotFoundException(String message) {
        super(message);
    }
}
