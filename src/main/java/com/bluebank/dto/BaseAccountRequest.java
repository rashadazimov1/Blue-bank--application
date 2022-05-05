package com.bluebank.dto;

import com.bluebank.model.City;
import com.bluebank.model.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class BaseAccountRequest {

    private String customerId;
    private Double balance;
    private City city;
    private Currency currency;
}
