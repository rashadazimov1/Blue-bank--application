package com.bluebank.repository;


import com.bluebank.model.Account;
import com.bluebank.model.Currency;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccountRepository extends CrudRepository<Account,String> {
    List<Account> findAllByBalanceGreaterThan(Double balance);
    List<Account>findAllByCurrencyIsAndAndBalanceLessThan(Currency currency, Double balance);



}
