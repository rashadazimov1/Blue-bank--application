package com.bluebank.service;

import com.bluebank.dto.*;
import com.bluebank.exception.CustomerNotFoundException;
import com.bluebank.model.Account;
import com.bluebank.model.Customer;
import com.bluebank.repository.AccountRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final CustomerService customerService;
    private final AccountDtoConverter accountDtoConverter;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final DirectExchange exchange;

    private final AmqpTemplate rabbitTemplate;


    @Value("${account.rabbitmq.routingKey}")
    String routingKey;

    @Value("${account.rabbitmq.queue}")
    String queueName;

    public AccountService(AccountRepository accountRepository, CustomerService customerService, AccountDtoConverter accountDtoConverter, KafkaTemplate<String, String> kafkaTemplate, DirectExchange exchange, AmqpTemplate rabbitTemplate) {
        this.accountRepository = accountRepository;
        this.customerService = customerService;
        this.accountDtoConverter = accountDtoConverter;
        this.kafkaTemplate = kafkaTemplate;
        this.exchange = exchange;
        this.rabbitTemplate = rabbitTemplate;
    }
    @CachePut(value = "accounts", key = "#id")
    public AccountDto createAccount(CreateAccountRequest createAccountRequest) {
        Customer customer = customerService.getCustomerById(createAccountRequest.getCustomerId());
        if (customer.getId().equals("") || customer.getId() == null) {
            throw new CustomerNotFoundException("Customer not found ");
        }
        Account account = Account.builder()
                .id(createAccountRequest.getId())
                .balance(createAccountRequest.getBalance())
                .currency(createAccountRequest.getCurrency())
                .customerId(createAccountRequest.getCustomerId())
                .city(createAccountRequest.getCity())

                .build();

        return accountDtoConverter.converter(accountRepository.save(account));
    }
    @CacheEvict(value = "accounts", allEntries = true)
    public AccountDto updateAccount(String id, UpdateAccountDto updateAccountDto) {
        Customer customer = customerService.getCustomerById(updateAccountDto.getCustomerId());
        if (customer.getId().equals("") || customer.getId() == null) {
            return AccountDto.builder().build();
        }
        Optional<Account> accountOptional = accountRepository.findById(id);
        accountOptional.ifPresent(account -> {
            account.setBalance(updateAccountDto.getBalance());
            account.setCity(updateAccountDto.getCity());
            account.setCurrency(updateAccountDto.getCurrency());
            account.setCustomerId(account.getCustomerId());
            accountRepository.save(account);
        });
        return accountOptional.map(accountDtoConverter::converter).orElse(new AccountDto());

    }
    @Cacheable(value = "accounts")
    public List<AccountDto> getAllAccounts() {
        List<Account> accountList = (List<Account>) accountRepository.findAll();

        return accountList.stream().map(accountDtoConverter::converter).collect(Collectors.toList());
    }

    public AccountDto getAccountById(String id) {
        return accountRepository.findById(id).map(accountDtoConverter::converter).orElse(new AccountDto());
    }
    @CacheEvict(value = "accounts", allEntries = true)
    public void deleteAccount(String id) {
        accountRepository.deleteById(id);
    }

    public AccountDto withDrawMoney(String id, Double amount) {
        Optional<Account> accountDtoOptional = accountRepository.findById(id);
        accountDtoOptional.ifPresent(account -> {
            if (account.getBalance() > amount) {
                account.setBalance(account.getBalance() - amount);
                accountRepository.save(account);
            } else {
                System.out.println("Insufucient funds -> accountId:" + id + "balance" + account.getBalance());
            }
        });
        return accountDtoOptional.map(accountDtoConverter::converter).orElse(new AccountDto());
    }


    public AccountDto addMoney(String id, Double amount) {
        Optional<Account> accountDtoOptional = accountRepository.findById(id);
        accountDtoOptional.ifPresent(account -> {
            account.setBalance(account.getBalance() + amount);
            accountRepository.save(account);

        });
        return accountDtoOptional.map(accountDtoConverter::converter).orElse(new AccountDto());
    }
    public void transferMoney(MoneyTransferRequest transferRequest){
        rabbitTemplate.convertAndSend(exchange.getName(), routingKey, transferRequest);
    }
    @RabbitListener(queues = "${account.rabbitmq.queue}")
    public void transferMoneyMessage(MoneyTransferRequest transferRequest) {
        Optional<Account> accountOptional = accountRepository.findById(transferRequest.getFromId());
        accountOptional.ifPresentOrElse(account -> {
            if (account.getBalance() > transferRequest.getAmount()) {
                account.setBalance(account.getBalance() - transferRequest.getAmount());
                accountRepository.save(account);
                rabbitTemplate.convertAndSend(exchange.getName(),"accountSecondRouting",transferRequest);
            } else {
                System.out.println("Insufficient funds -> accountId: " + transferRequest.getFromId() + " balance: " + account.getBalance() + " amount: " + transferRequest.getAmount());
            }},
                ()-> System.out.println("Account not foundTY")

        );
    }

    @RabbitListener(queues = "accountSecondStepQueue")
    public void updateReceiverAccount(MoneyTransferRequest transferRequest) {
        Optional<Account> accountOptional = accountRepository.findById(transferRequest.getToId());
        accountOptional.ifPresentOrElse(account -> {
                    account.setBalance(account.getBalance() + transferRequest.getAmount());
                    accountRepository.save(account);
                    rabbitTemplate.convertAndSend(exchange.getName(), "accountThirdRouting", transferRequest);
                },
                () -> {
                    System.out.println("Receiver Account not found");
                    Optional<Account> senderAccount = accountRepository.findById(transferRequest.getFromId());
                    senderAccount.ifPresent(sender -> {
                        System.out.println("Money charge back to sender");
                        sender.setBalance(sender.getBalance() + transferRequest.getAmount());
                        accountRepository.save(sender);
                    });

                }
        );
    }

    @RabbitListener(queues = "thirdStepQueue")
    public void finalizeTransfer(MoneyTransferRequest transferRequest) {
        Optional<Account> accountOptional = accountRepository.findById(transferRequest.getFromId());
        accountOptional.ifPresentOrElse(account ->
                {
                    String notificationMessage = "Dear customer %s \n Your money transfer request has been succeed. Your new balance is %s";
                    System.out.println("Sender(" + account.getId() +") new account balance: " + account.getBalance());
                    String senderMessage = String.format(notificationMessage, account.getId(), account.getBalance());
                    kafkaTemplate.send("transfer-notification",  senderMessage);
                }, () -> System.out.println("Account not found78")
        );

        Optional<Account> accountToOptional = accountRepository.findById(transferRequest.getToId());
        accountToOptional.ifPresentOrElse(account ->
                {
                    String notificationMessage = "Dear customer %s \n You received a money transfer from %s. Your new balance is %s";
                    System.out.println("Receiver(" + account.getId() +") new account balance: " + account.getBalance());
                    String receiverMessage = String.format(notificationMessage, account.getId(), transferRequest.getFromId(), account.getBalance());
                    kafkaTemplate.send("transfer-notification",  receiverMessage);
                },
                () -> System.out.println("Account not foundUYTY")
        );


    }


    }






