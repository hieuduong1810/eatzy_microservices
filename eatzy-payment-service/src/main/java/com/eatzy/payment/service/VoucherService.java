package com.eatzy.payment.service;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.designpattern.adapter.AuthServiceClient;
import com.eatzy.payment.designpattern.adapter.OrderServiceClient;
import com.eatzy.payment.domain.Voucher;
import com.eatzy.payment.dto.response.ResVoucherDTO;
import com.eatzy.payment.repository.VoucherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final OrderServiceClient orderServiceClient;
    private final AuthServiceClient authServiceClient;

    public VoucherService(VoucherRepository voucherRepository,
                          OrderServiceClient orderServiceClient,
                          AuthServiceClient authServiceClient) {
        this.voucherRepository = voucherRepository;
        this.orderServiceClient = orderServiceClient;
        this.authServiceClient = authServiceClient;
    }

    private Long getCurrentUserId() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            if (email != null && !email.equals("anonymousUser")) {
                Map<String, Object> userBody = authServiceClient.getUserByEmail(email);
                if (userBody != null && userBody.containsKey("data")) {
                    Map<String, Object> userData = (Map<String, Object>) userBody.get("data");
                    if (userData != null && userData.containsKey("id")) {
                        return ((Number) userData.get("id")).longValue();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    public ResVoucherDTO convertToResVoucherDTO(Voucher voucher) {
        return convertToResVoucherDTO(voucher, null);
    }

    public ResVoucherDTO convertToResVoucherDTO(Voucher voucher, Long userId) {
        if (voucher == null) return null;
        ResVoucherDTO dto = new ResVoucherDTO();
        dto.setId(voucher.getId());
        dto.setCode(voucher.getCode());
        dto.setDescription(voucher.getDescription());
        dto.setDiscountType(voucher.getDiscountType());
        dto.setDiscountValue(voucher.getDiscountValue());
        dto.setMinOrderValue(voucher.getMinOrderValue());
        dto.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        dto.setUsageLimitPerUser(voucher.getUsageLimitPerUser());
        dto.setStartDate(voucher.getStartDate());
        dto.setEndDate(voucher.getEndDate());
        dto.setTotalQuantity(voucher.getTotalQuantity());
        dto.setRemainingQuantity(voucher.getRemainingQuantity());
        dto.setActive(voucher.getActive());

        if (userId == null) {
            userId = getCurrentUserId();
        }

        if (userId != null && voucher.getUsageLimitPerUser() != null) {
            try {
                Long usageCount = orderServiceClient.countByCustomerIdAndVoucherId(userId, voucher.getId());
                int remaining = voucher.getUsageLimitPerUser() - (usageCount != null ? usageCount.intValue() : 0);
                dto.setRemainingUsage(Math.max(0, remaining));
            } catch (Exception e) {
                dto.setRemainingUsage(voucher.getUsageLimitPerUser());
            }
        } else if (voucher.getUsageLimitPerUser() != null) {
            dto.setRemainingUsage(voucher.getUsageLimitPerUser());
        } else {
            dto.setRemainingUsage(null);
        }

        if (voucher.getRestaurantIds() != null && !voucher.getRestaurantIds().isEmpty()) {
            List<ResVoucherDTO.RestaurantSummary> restaurantDTOs = voucher.getRestaurantIds().stream()
                    .map(rId -> {
                        ResVoucherDTO.RestaurantSummary rs = new ResVoucherDTO.RestaurantSummary();
                        rs.setId(rId);
                        // Due to performance, we don't fetch every restaurant name.
                        rs.setName("Restaurant " + rId); 
                        return rs;
                    }).collect(Collectors.toList());
            dto.setRestaurants(restaurantDTOs);
        }

        return dto;
    }

    @Transactional
    public Voucher getVoucherById(Long id) {
        return voucherRepository.findById(id).orElse(null);
    }

    public ResVoucherDTO getVoucherByCode(String code) {
        return convertToResVoucherDTO(voucherRepository.findByCode(code).orElse(null));
    }

    public List<ResVoucherDTO> getVouchersByRestaurantId(Long restaurantId) throws IdInvalidException {
        Long customerId = getCurrentUserId();
        if (customerId == null) {
            throw new IdInvalidException("User not authenticated or not found");
        }

        List<Voucher> vouchers = voucherRepository.findAvailableVouchersForOrder(restaurantId, Instant.now());

        return vouchers.stream()
                .filter(voucher -> voucher.getActive() != null && voucher.getActive())
                .filter(voucher -> {
                    if (voucher.getUsageLimitPerUser() == null) return true;
                    try {
                        Long usageCount = orderServiceClient.countByCustomerIdAndVoucherId(customerId, voucher.getId());
                        return (usageCount != null ? usageCount : 0L) < voucher.getUsageLimitPerUser();
                    } catch (Exception e) {
                        return true;
                    }
                })
                .map(v -> convertToResVoucherDTO(v, customerId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ResVoucherDTO createVoucher(Voucher voucher) throws IdInvalidException {
        if (voucherRepository.existsByCode(voucher.getCode())) {
            throw new IdInvalidException("Voucher code already exists: " + voucher.getCode());
        }
        if (voucher.getTotalQuantity() != null) {
            voucher.setRemainingQuantity(voucher.getTotalQuantity());
        }
        Voucher savedVoucher = voucherRepository.save(voucher);
        return convertToResVoucherDTO(savedVoucher);
    }

    @Transactional
    public ResVoucherDTO createVoucherForAllRestaurants(Voucher voucher) throws IdInvalidException {
        if (voucherRepository.existsByCode(voucher.getCode())) {
            throw new IdInvalidException("Voucher code already exists");
        }
        if (voucher.getTotalQuantity() != null) {
            voucher.setRemainingQuantity(voucher.getTotalQuantity());
        }
        // Empty means all restaurants
        voucher.setRestaurantIds(new ArrayList<>()); 
        Voucher savedVoucher = voucherRepository.save(voucher);
        return convertToResVoucherDTO(savedVoucher);
    }

    @Transactional
    public ResVoucherDTO updateVoucher(Voucher voucher) throws IdInvalidException {
        Voucher currentVoucher = voucherRepository.findById(voucher.getId())
                .orElseThrow(() -> new IdInvalidException("Voucher not found with id: " + voucher.getId()));

        if (voucher.getCode() != null && !voucher.getCode().equals(currentVoucher.getCode())) {
            if (voucherRepository.existsByCode(voucher.getCode())) {
                throw new IdInvalidException("Voucher code already exists");
            }
            currentVoucher.setCode(voucher.getCode());
        }
        
        if (voucher.getDescription() != null) currentVoucher.setDescription(voucher.getDescription());
        if (voucher.getDiscountType() != null) currentVoucher.setDiscountType(voucher.getDiscountType());
        if (voucher.getDiscountValue() != null) currentVoucher.setDiscountValue(voucher.getDiscountValue());
        if (voucher.getMinOrderValue() != null) currentVoucher.setMinOrderValue(voucher.getMinOrderValue());
        if (voucher.getMaxDiscountAmount() != null) currentVoucher.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        if (voucher.getUsageLimitPerUser() != null) currentVoucher.setUsageLimitPerUser(voucher.getUsageLimitPerUser());
        if (voucher.getStartDate() != null) currentVoucher.setStartDate(voucher.getStartDate());
        if (voucher.getEndDate() != null) currentVoucher.setEndDate(voucher.getEndDate());
        
        if (voucher.getTotalQuantity() != null) {
            Integer quantityChange = voucher.getTotalQuantity();
            if (quantityChange > 0) {
                currentVoucher.setTotalQuantity(currentVoucher.getTotalQuantity() + quantityChange);
                currentVoucher.setRemainingQuantity((currentVoucher.getRemainingQuantity() != null ? currentVoucher.getRemainingQuantity() : 0) + quantityChange);
            } else if (quantityChange < 0) {
                Integer absChange = Math.abs(quantityChange);
                Integer currentRemaining = currentVoucher.getRemainingQuantity() != null ? currentVoucher.getRemainingQuantity() : 0;
                if (currentRemaining >= absChange) {
                    currentVoucher.setTotalQuantity(currentVoucher.getTotalQuantity() + quantityChange);
                    currentVoucher.setRemainingQuantity(currentRemaining + quantityChange);
                } else {
                    throw new IdInvalidException("Cannot reduce quantity. Only " + currentRemaining + " remaining");
                }
            }
        }

        if (voucher.getRestaurantIds() != null) {
            currentVoucher.setRestaurantIds(voucher.getRestaurantIds());
        }

        Voucher savedVoucher = voucherRepository.save(currentVoucher);
        return convertToResVoucherDTO(savedVoucher);
    }

    public ResultPaginationDTO getAllVouchers(Specification<Voucher> spec, Pageable pageable) {
        Page<Voucher> page = voucherRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent().stream().map(this::convertToResVoucherDTO).collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public void decrementVoucherQuantity(List<Long> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) return;

        for (Long voucherId : voucherIds) {
            Voucher voucher = voucherRepository.findById(voucherId).orElse(null);
            if (voucher != null && voucher.getRemainingQuantity() != null && voucher.getRemainingQuantity() > 0) {
                voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
                voucherRepository.save(voucher);
                // System log equivalent to eatzy_backend log
                System.out.println("Áp dụng voucher " + voucher.getCode() + " - Số lượng còn lại: " + voucher.getRemainingQuantity());
            }
        }
    }

    public void deleteVoucher(Long id) {
        voucherRepository.deleteById(id);
    }
}
