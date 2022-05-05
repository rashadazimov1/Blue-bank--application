package com.bluebank.dto;

import com.bluebank.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountDtoConverter  {

    public AccountDto converter(Account account){
       return AccountDto.builder()
                .id(account.getId())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .city(account.getCity())
                .customerId(account.getCustomerId())
                .build();











    }


}
