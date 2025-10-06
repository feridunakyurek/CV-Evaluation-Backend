package com.cvanalyzer.dtos;
import lombok.Data;

@Data
public class AuthRequest {
    private String email;

    private String password; // Kullanıcının şifresi.
}

