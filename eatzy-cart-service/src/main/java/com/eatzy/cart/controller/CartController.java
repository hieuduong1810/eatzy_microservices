package com.eatzy.cart.controller;

import com.eatzy.cart.domain.Cart;
import com.eatzy.cart.dto.req.ReqCartDTO;
import com.eatzy.cart.dto.res.ResCartDTO;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.cart.service.CartService;
import com.eatzy.common.exception.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/carts")
    public ResponseEntity<ResultPaginationDTO> getAllCarts(
            @Filter Specification<Cart> spec, Pageable pageable) {
        ResultPaginationDTO result = cartService.getAllCarts(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/carts/{id}")
    public ResponseEntity<ResCartDTO> getCartById(@PathVariable("id") Long id) {
        ResCartDTO cart = cartService.getCartById(id);
        if (cart == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/carts/customer/{customerId}")
    public ResponseEntity<List<ResCartDTO>> getCartsByCustomerId(@PathVariable("customerId") Long customerId) {
        List<ResCartDTO> carts = cartService.getCartsByCustomerId(customerId);
        return ResponseEntity.ok(carts);
    }

    @GetMapping("/carts/my-carts")
    public ResponseEntity<List<ResCartDTO>> getMyCart() throws IdInvalidException {
        List<ResCartDTO> carts = cartService.getMyCarts();
        return ResponseEntity.ok(carts);
    }

    @DeleteMapping("/carts/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable("id") Long id) {
        cartService.deleteCart(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/carts")
    public ResponseEntity<ResCartDTO> saveOrUpdateCart(@Valid @RequestBody ReqCartDTO reqCartDTO) {
        ResCartDTO result = cartService.saveOrUpdateCart(reqCartDTO);
        if (result == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(result);
    }
}
