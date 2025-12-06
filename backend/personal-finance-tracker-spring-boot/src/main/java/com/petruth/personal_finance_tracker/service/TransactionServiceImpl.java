package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.TransactionRepository;
import com.petruth.personal_finance_tracker.specifications.TransactionSpecifications;
import com.petruth.personal_finance_tracker.utils.TransactionMapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "transactions")
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
    @Cacheable(
            key = "'user:' + #userId + ':default'",
            condition = "#type == null && #fromDate == null && #toDate == null && " +
                    "#categoryId == null && #minAmount == null && #maxAmount == null",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<TransactionDTO> findByUserId(int userId, String type, String fromDate, String toDate,
                                             Long categoryId, Double minAmount, Double maxAmount) {

        Specification<Transaction> spec = Specification.unrestricted();

        // fetch only items belonging to the user
        spec = spec.and(TransactionSpecifications.belongsToUser(userId));

        // apply filter criteria
        if (type != null) {
            spec = spec.and(TransactionSpecifications.hasType(Transaction.TransactionType.valueOf(type.toUpperCase())));
        }
        if (categoryId != null) {
            spec = spec.and(TransactionSpecifications.hasCategory(categoryId));
        }
        if (minAmount != null) {
            spec = spec.and(TransactionSpecifications.minAmount(minAmount));
        }
        if (maxAmount != null) {
            spec = spec.and(TransactionSpecifications.maxAmount(maxAmount));
        }
        if (fromDate != null) {
            spec = spec.and(TransactionSpecifications.dateAfter(LocalDateTime.parse(fromDate)));
        }
        if (toDate != null) {
            spec = spec.and(TransactionSpecifications.dateBefore(LocalDateTime.parse(toDate)));
        }

        // fetch data after applying filtering criteria and use streams to map the returned list to TransactionDTO
        return transactionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "date"))
                .stream()
                .map(transactionMapper::toTransactionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(
            key = "'user:' + #userId + ':page:' + #page + ':filters:' + " +
                    "#type + ':' + #categoryId + ':' + #fromDate + ':' + #toDate",
            condition = "#page < 5",  // Only cache first 5 pages
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<TransactionDTO> findByUserId(
            int userId, String type, String fromDate, String toDate,
            Long categoryId, Double minAmount, Double maxAmount,
            int page, int size, String sortBy, String sortDirection
    ) {
        Specification<Transaction> spec = Specification.unrestricted();
        spec = spec.and(TransactionSpecifications.belongsToUser(userId));

        // Apply filters (existing code)...
        // apply filter criteria
        if (type != null) {
            spec = spec.and(TransactionSpecifications.hasType(Transaction.TransactionType.valueOf(type.toUpperCase())));
        }
        if (categoryId != null) {
            spec = spec.and(TransactionSpecifications.hasCategory(categoryId));
        }
        if (minAmount != null) {
            spec = spec.and(TransactionSpecifications.minAmount(minAmount));
        }
        if (maxAmount != null) {
            spec = spec.and(TransactionSpecifications.maxAmount(maxAmount));
        }
        if (fromDate != null) {
            spec = spec.and(TransactionSpecifications.dateAfter(LocalDateTime.parse(fromDate)));
        }
        if (toDate != null) {
            spec = spec.and(TransactionSpecifications.dateBefore(LocalDateTime.parse(toDate)));
        }

        // Create pageable with sorting
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

        return transactionPage.map(transactionMapper::toTransactionDTO);
    }



    // We use TransactionDTO for cleaner requests and responses
    @Override
    @CacheEvict(allEntries = true)
    public Transaction saveFromDTO(TransactionDTO transactionDTO) {
        User dbUser = userService.findById(transactionDTO.getUserId());
        Category dbCategory = categoryService.findById(transactionDTO.getCategoryId());
        Transaction transaction = transactionMapper.toTransaction(transactionDTO, dbUser, dbCategory);
        return transactionRepository.save(transaction);
    }

    @Override
    @Cacheable(key = "'user:' + #id + ':type:' + #type.name()")
    public List<TransactionDTO> findByUserIdAndTypeOrderByDate(int id, Transaction.TransactionType type) {
        List<Transaction> dbTransactions = transactionRepository.findByUserIdAndTypeOrderByDate(id, type);

        return dbTransactions.stream()
                .map(transactionMapper::toTransactionDTO)
                .collect(Collectors.toList());
    }


    @Override
    @CacheEvict(allEntries = true)
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void deleteById(Long id) {
        transactionRepository.deleteById(id);
    }
}
