package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.TransactionRequest;
import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.Transaction;
import org.springframework.stereotype.Service;


@Service
public interface ITransactionService {
    Transaction createOrRenewVnPayTransaction(Order order) throws Exception;
    Transaction ensureInitialCashTransaction(Order order) throws Exception;


}
