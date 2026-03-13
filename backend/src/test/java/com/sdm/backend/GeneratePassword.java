package com.sdm.backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        String encoded = encoder.encode(rawPassword);
        System.out.println("BCrypt 密码: " + encoded);
        System.out.println("密码长度: " + encoded.length());
    }
}