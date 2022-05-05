package com.bluebank.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UpdateAccountDto extends BaseAccountRequest {
    private String id;
}
