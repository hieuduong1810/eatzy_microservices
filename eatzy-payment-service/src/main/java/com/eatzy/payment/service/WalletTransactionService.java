package com.eatzy.payment.service;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.payment.designpattern.adapter.AuthServiceClient;
import com.eatzy.payment.designpattern.adapter.OrderServiceClient;
import com.eatzy.payment.domain.Wallet;
import com.eatzy.payment.domain.WalletTransaction;
import com.eatzy.payment.dto.response.ResWalletTransactionDTO;
import com.eatzy.payment.repository.WalletTransactionRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;

import com.eatzy.payment.domain.enums.TransactionType;

@Service
public class WalletTransactionService {
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;
    private final AuthServiceClient authServiceClient;
    private final OrderServiceClient orderServiceClient;

    public WalletTransactionService(WalletTransactionRepository walletTransactionRepository,
            WalletService walletService,
            AuthServiceClient authServiceClient,
            @Lazy OrderServiceClient orderServiceClient) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletService = walletService;
        this.authServiceClient = authServiceClient;
        this.orderServiceClient = orderServiceClient;
    }

    private ResWalletTransactionDTO convertToDTO(WalletTransaction transaction) {
        ResWalletTransactionDTO dto = new ResWalletTransactionDTO();
        dto.setId(transaction.getId());

        if (transaction.getWallet() != null) {
            Wallet wallet = transaction.getWallet();
            ResWalletTransactionDTO.Wallet walletDTO = new ResWalletTransactionDTO.Wallet();
            walletDTO.setId(wallet.getId());

            if (wallet.getUserId() != null) {
                ResWalletTransactionDTO.Wallet.User userDTO = new ResWalletTransactionDTO.Wallet.User();
                userDTO.setId(wallet.getUserId());
                userDTO.setName(extractUserName(wallet.getUserId()));
                walletDTO.setUser(userDTO);
            }

            dto.setWallet(walletDTO);
        }

        if (transaction.getOrderId() != null) {
            ResWalletTransactionDTO.Order orderDTO = new ResWalletTransactionDTO.Order();
            orderDTO.setId(transaction.getOrderId());
            dto.setOrder(orderDTO);
        }

        dto.setAmount(transaction.getAmount());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setDescription(transaction.getDescription());
        dto.setStatus(transaction.getStatus());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setTransactionDate(transaction.getTransactionDate());
        return dto;
    }

    private String extractUserName(Long userId) {
        try {
            Map<String, Object> response = authServiceClient.getUserById(userId);
            if (response == null) {
                return null;
            }

            Object data = response.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                Object name = dataMap.get("name");
                if (name == null) {
                    name = dataMap.get("fullName");
                }
                if (name == null) {
                    name = dataMap.get("username");
                }
                return name != null ? name.toString() : null;
            }

            Object name = response.get("name");
            if (name == null) {
                name = response.get("fullName");
            }
            if (name == null) {
                name = response.get("username");
            }
            return name != null ? name.toString() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    public ResWalletTransactionDTO getWalletTransactionById(Long id) {
        return walletTransactionRepository.findById(id).map(this::convertToDTO).orElse(null);
    }

    public List<ResWalletTransactionDTO> getWalletTransactionsByWalletId(Long walletId) {
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public ResWalletTransactionDTO createWalletTransaction(WalletTransaction walletTransaction)
            throws IdInvalidException {
        if (walletTransaction.getWallet() != null) {
            Wallet wallet = walletService.getWalletById(walletTransaction.getWallet().getId());
            if (wallet == null)
                throw new IdInvalidException("Wallet not found");
            walletTransaction.setWallet(wallet);
        } else {
            throw new IdInvalidException("Wallet is required");
        }

        if (walletTransaction.getAmount() == null || walletTransaction.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new IdInvalidException("Amount is required and must not be zero");
        }

        if (walletTransaction.getTransactionType() == null) {
            throw new IdInvalidException("Transaction type is required");
        }

        if (walletTransaction.getStatus() == null) {
            walletTransaction.setStatus("PENDING");
        }
        walletTransaction.setCreatedAt(Instant.now());

        WalletTransaction savedTransaction = walletTransactionRepository.save(walletTransaction);

        if ("SUCCESS".equals(walletTransaction.getStatus())) {
            updateWalletBalance(walletTransaction);
        }

        return convertToDTO(savedTransaction);
    }

    @Transactional
    public ResWalletTransactionDTO depositToWallet(Long walletId, BigDecimal amount, String description,
            TransactionType transactionType)
            throws IdInvalidException {
        Wallet wallet = walletService.getWalletById(walletId);
        if (wallet == null)
            throw new IdInvalidException("Wallet not found");
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IdInvalidException("Amount must be > 0");

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(transactionType != null ? transactionType : TransactionType.DEPOSIT)
                .description(description != null ? description : "Deposit to wallet")
                .status("SUCCESS")
                .createdAt(Instant.now())
                .build();

        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        walletService.addBalance(walletId, amount);

        return convertToDTO(savedTransaction);
    }

    @Transactional
    public ResWalletTransactionDTO withdrawFromWallet(Long walletId, BigDecimal amount, String description,
            TransactionType transactionType)
            throws IdInvalidException {
        Wallet wallet = walletService.getWalletById(walletId);
        if (wallet == null)
            throw new IdInvalidException("Wallet not found");
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IdInvalidException("Amount must be > 0");
        if (wallet.getBalance().compareTo(amount) < 0)
            throw new IdInvalidException("Insufficient balance");

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .transactionType(transactionType != null ? transactionType : TransactionType.WITHDRAWAL)
                .description(description != null ? description : "Withdrawal from wallet")
                .status("SUCCESS")
                .createdAt(Instant.now())
                .build();

        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        walletService.subtractBalance(walletId, amount);

        return convertToDTO(savedTransaction);
    }

    @Transactional
    public ResWalletTransactionDTO depositToWalletByUserId(Long userId, BigDecimal amount, String description,
            Long orderId, TransactionType transactionType) throws IdInvalidException {
        Wallet wallet = walletService.getWalletByUserId(userId);
        if (wallet == null) {
            wallet = walletService.createWallet(userId); // Auto-create if not exists
        }

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(transactionType != null ? transactionType : TransactionType.DEPOSIT)
                .description(description)
                .orderId(orderId)
                .status("SUCCESS")
                .createdAt(Instant.now())
                .build();

        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        walletService.addBalance(wallet.getId(), amount);

        return convertToDTO(savedTransaction);
    }

    @Transactional
    public ResWalletTransactionDTO withdrawFromWalletByUserId(Long userId, BigDecimal amount, String description,
            Long orderId, TransactionType transactionType) throws IdInvalidException {
        Wallet wallet = walletService.getWalletByUserId(userId);
        if (wallet == null) {
            wallet = walletService.createWallet(userId);
        }

        // Negative amount for withdraw
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .transactionType(transactionType != null ? transactionType : TransactionType.WITHDRAWAL)
                .description(description)
                .orderId(orderId)
                .status("SUCCESS")
                .createdAt(Instant.now())
                .build();

        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        walletService.subtractBalance(wallet.getId(), amount);

        return convertToDTO(savedTransaction);
    }

    private void updateWalletBalance(WalletTransaction transaction) throws IdInvalidException {
        Wallet wallet = transaction.getWallet();
        BigDecimal amount = transaction.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            walletService.addBalance(wallet.getId(), amount);
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            walletService.subtractBalance(wallet.getId(), amount.negate());
        }
    }

    public ResultPaginationDTO getWalletTransactionsByWalletIdWithSpec(Long walletId,
            Specification<WalletTransaction> spec, Pageable pageable) {
        // Build base spec only if walletId is provided
        Specification<WalletTransaction> baseSpec = null;
        if (walletId != null) {
            baseSpec = (root, query, cb) -> cb.equal(root.get("wallet").get("id"), walletId);
        }

        // Combine with additional spec if provided
        Specification<WalletTransaction> combinedSpec = baseSpec;
        if (spec != null) {
            combinedSpec = baseSpec != null ? baseSpec.and(spec) : spec;
        }

        Page<WalletTransaction> page = walletTransactionRepository.findAll(combinedSpec, pageable);

        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()));
        return result;
    }

    /**
     * Lấy danh sách giao dịch của current user (dựa vào userId từ JWT)
     */
    public ResultPaginationDTO getMyTransactions(Pageable pageable) throws IdInvalidException {
        Long userId = SecurityUtils.getCurrentUserId();
        Wallet wallet = walletService.getWalletByUserId(userId);
        if (wallet == null) {
            // Nếu chưa có wallet thì trả về empty
            ResultPaginationDTO result = new ResultPaginationDTO();
            ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
            meta.setPage(pageable.getPageNumber() + 1);
            meta.setPageSize(pageable.getPageSize());
            meta.setTotal(0L);
            meta.setPages(0);
            result.setMeta(meta);
            result.setResult(List.of());
            return result;
        }
        return getWalletTransactionsByWalletIdWithSpec(wallet.getId(), null, pageable);
    }
}
