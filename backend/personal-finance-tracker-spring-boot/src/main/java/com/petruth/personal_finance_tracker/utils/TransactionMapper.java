package com.petruth.personal_finance_tracker.utils;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import org.springframework.stereotype.Component;


@Component
public class TransactionMapper {
    // To not return JSON loops, we convert the Transaction entity into a cleaner TransactionDTO response
    public TransactionDTO toTransactionDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType().toString(),
                transaction.getDescription(),
                transaction.getDate(),
                transaction.getUser().getId(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName()
        );
    }
    // For cleaner userID inserts, this method spares us from inserting the userID like: "user":{"id":id}
    public Transaction toTransaction(TransactionDTO transactionDTO, User user, Category category) {
        return new Transaction(
                transactionDTO.getId(),
                transactionDTO.getAmount(),
                Transaction.TransactionType.valueOf(transactionDTO.getType()),
                transactionDTO.getDescription(),
                transactionDTO.getDate(),
                user,
                category
        );
    }
}

