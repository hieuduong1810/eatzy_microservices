package com.eatzy.payment.controller;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.domain.Voucher;
import com.eatzy.payment.dto.response.ResVoucherDTO;
import com.eatzy.payment.service.VoucherService;
import com.turkraft.springfilter.boot.Filter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {
    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping
    public ResponseEntity<ResVoucherDTO> createVoucher(@RequestBody Voucher voucher) throws IdInvalidException {
        ResVoucherDTO dto = voucherService.createVoucher(voucher);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/all-restaurants")
    public ResponseEntity<ResVoucherDTO> createVoucherForAllRestaurants(@RequestBody Voucher voucher)
            throws IdInvalidException {
        ResVoucherDTO dto = voucherService.createVoucherForAllRestaurants(voucher);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping
    public ResponseEntity<ResVoucherDTO> updateVoucher(@RequestBody Voucher voucher) throws IdInvalidException {
        ResVoucherDTO dto = voucherService.updateVoucher(voucher);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAllVouchers(
            @Filter Specification<Voucher> spec, Pageable pageable) {
        return ResponseEntity.ok(voucherService.getAllVouchers(spec, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResVoucherDTO> getVoucherById(@PathVariable long id) {
        ResVoucherDTO dto = voucherService.getVoucherById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ResVoucherDTO> getVoucherByCode(@PathVariable String code) {
        return ResponseEntity.ok(voucherService.getVoucherByCode(code));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ResVoucherDTO>> getVouchersByRestaurantId(@PathVariable Long restaurantId)
            throws IdInvalidException {
        return ResponseEntity.ok(voucherService.getVouchersByRestaurantId(restaurantId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }
}
