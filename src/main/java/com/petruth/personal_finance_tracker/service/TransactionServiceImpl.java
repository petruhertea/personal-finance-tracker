package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.TransactionRepository;
import com.petruth.personal_finance_tracker.utils.TransactionMapper;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService{

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserService userService;
    private final CategoryService categoryService;

    TransactionServiceImpl(TransactionRepository transactionRepository,
                           TransactionMapper transactionMapper,
                           UserService userService,
                           CategoryService categoryService
                           ){
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @Override
    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction with id -> "+ id + " not found"));
    }

    @Override
    public List<TransactionDTO> findByUserId(int id) {
        List<Transaction> tempTransactions = transactionRepository.findByUserIdOrderByDateDesc(id);

        return tempTransactions
                .stream()
                .map(transactionMapper::toTransactionDTO)
                .collect(Collectors.toList());
    }

    // We use TransactionDTO for cleaner requests and responses
    @Override
    public Transaction saveFromDTO(TransactionDTO transactionDTO) {
        User dbUser = userService.findById(transactionDTO.getUserId());
        Category dbCategory = categoryService.findById(transactionDTO.getCategoryId());
        Transaction transaction = transactionMapper.toTransaction(transactionDTO, dbUser, dbCategory);
        return transactionRepository.save(transaction);
    }


    @Override
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public void deleteById(Long id) {
        transactionRepository.deleteById(id);
    }
}
