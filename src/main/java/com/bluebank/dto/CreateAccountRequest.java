package com.bluebank.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class CreateAccountRequest extends BaseAccountRequest {
    @NotBlank(message = "Account id not be empty")

    private String id;
}
