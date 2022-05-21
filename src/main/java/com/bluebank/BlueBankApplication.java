package com.bluebank;

import com.bluebank.model.*;
import com.bluebank.repository.AccountRepository;
import com.bluebank.repository.CustomerRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import java.util.Arrays;

@SpringBootApplication
@EnableCaching

public class BlueBankApplication implements CommandLineRunner {

	private final AccountRepository accountRepository;
	private final CustomerRepository customerRepository;

	public BlueBankApplication(AccountRepository accountRepository, CustomerRepository customerRepository) {
		this.accountRepository = accountRepository;
		this.customerRepository = customerRepository;
	}


	public static void main(String[] args) {
		SpringApplication.run(BlueBankApplication.class, args);
	}

	@Bean
	public OpenAPI customOpenAPI(@Value("${application-description}") String description,
								 @Value("${application-version}") String version){
		return new OpenAPI()
				.info(new Info()
						.title("BlueBank API")
						.version(version)
						.description(description)
						.license(new License().name("BlueBank API Licence")));
	}

	@Override
	public void run(String... args) throws Exception{
		Customer c1 = Customer.builder()
				.id("1234568")
				.name("Rahim")
				.address(Address.builder().city(City.Baku).postcode("456312").addressDetails("this is address").build())
				.city(City.Baku)
				.dateOfBirth(1988)
				.build();


		Customer c2 = Customer.builder()
				.id("789456")
				.name("Rashad")
				.city(City.Lankaran)
				.address(Address.builder().city(City.Lankaran).postcode("456312").addressDetails("this is address 2").build())
				.dateOfBirth(2000)
				.build();

		Customer c3 = Customer.builder()
				.id("456238")
				.name("Fuad")
				.city(City.Sumgait)
				.address(Address.builder().city(City.Sumgait).postcode("456312").addressDetails("this is address 3").build())
				.dateOfBirth(2005)
				.build();

		customerRepository.saveAll(Arrays.asList(c1,c2,c3));
		Account a1 = Account.builder()
				.id("10")
				.customerId("1234568")
				.city(City.Baku)
				.balance(1320.00)
				.currency(Currency.AZN)
				.build();
		Account a2 = Account.builder()
				.id("11")
				.customerId("789456")
				.city(City.Baku)
				.balance(7898.00)
				.currency(Currency.AZN)
				.build();
		Account a3 = Account.builder()
				.id("12")
				.customerId("456238")
				.city(City.Baku)
				.balance(555.00)
				.currency(Currency.AZN)
				.build();


		accountRepository.saveAll(Arrays.asList(a1,a2,a3));
	}

}
