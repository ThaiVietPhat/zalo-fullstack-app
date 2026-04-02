package com.example.backend.models;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Họ không được để trống")
    private String firstName;

    private String lastName;
}
