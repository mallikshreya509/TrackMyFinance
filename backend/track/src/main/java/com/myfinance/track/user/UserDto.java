package com.myfinance.track.user;

public record UserDto(Long id, String email, String displayName) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName());
    }
}