package com.cvanalyzer.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private String fullName;
    private String email;
    public UserResponse(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }
}
