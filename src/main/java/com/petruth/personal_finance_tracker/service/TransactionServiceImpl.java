package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.TransactionRepository;
import com.petruth.personal_finance_tracker.utils.TransactionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService{

    private TransactionRepository transactionRepository;
    private TransactionMapper transactionMapper;
    private UserService userService;

    TransactionServiceImpl(TransactionRepository transactionRepository,
                           TransactionMapper transactionMapper,
                           UserService userService){
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.userService = userService;
    }

    @Override
    public Transaction findById(Integer id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction with id -> "+ id + " not found"));
    }

    @Override
    public List<TransactionDTO> findByUserId(int id) {
        List<Transaction> tempTransactions = transactionRepository.findByUserId(id);

        List<TransactionDTO> transactionList = new ArrayList<>();

        for (Transaction transaction : tempTransactions){
            TransactionDTO result = transactionMapper.toTransactionDTO(transaction);

            transactionList.add(result);
        }

        return transactionList;
    }

    // We use TransactionDTO for cleaner requests and responses
    @Override
    public Transaction saveFromDTO(TransactionDTO transactionDTO) {
        User dbUser = userService.findById(transactionDTO.getUserId());
        Transaction transaction = transactionMapper.toTransaction(transactionDTO, dbUser);
        return transactionRepository.save(transaction);
    }


    @Override
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public void deleteById(Integer id) {
        transactionRepository.deleteById(id);
    }
}
