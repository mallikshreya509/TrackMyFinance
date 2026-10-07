package com.myfinance.track.auth;

import com.myfinance.track.user.UserDto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserDto user) {}