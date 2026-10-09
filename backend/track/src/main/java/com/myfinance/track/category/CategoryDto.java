package com.myfinance.track.category;

public record CategoryDto(Long id, String name, String color) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getColor());
    }
}