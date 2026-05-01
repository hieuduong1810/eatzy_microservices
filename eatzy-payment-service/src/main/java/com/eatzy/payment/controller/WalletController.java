package com.eatzy.payment.controller;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.payment.dto.response.ResWalletDTO;
import com.eatzy.payment.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Lấy thông tin wallet của người dùng hiện tại.
     * Nếu chưa có wallet sẽ tự động tạo mới.
     */
    @GetMapping("/my-wallet")
    public ResponseEntity<ResWalletDTO> getMyWallet() throws IdInvalidException {
        return ResponseEntity.ok(walletService.getMyWallet());
    }
}
