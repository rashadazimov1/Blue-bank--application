package com.bluebank.dto;

import com.bluebank.model.City;
import com.bluebank.model.Currency;
import lombok.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class AccountDto implements Serializable {
    private String id;

    @NotBlank(message = "Customer id not be null")
    private String customerId;

    @NotNull
    @Min(0)
    private Double balance;

    @NotNull(message = "City must not be null")
    private City city;

    @NotNull(message = "Currency must not be null")
    private Currency currency;


}
