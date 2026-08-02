package com.innowise.userservice.service;

import io.jsonwebtoken.Claims;

public interface JwtService {

    Claims validate(String token);

    Long getUserId(String token);

    String getRole(String token);
}
