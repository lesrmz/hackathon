package com.prettypetty.dto;

import com.prettypetty.model.User;

public record UserResponse(String id, String email, String displayName, String villainId, String aestheticId) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.email(), user.displayName(), user.villainId(), user.aestheticId());
    }
}
