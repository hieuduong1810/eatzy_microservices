package com.eatzy.interaction.controller;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.interaction.domain.Favorite;
import com.eatzy.interaction.dto.request.ReqFavoriteCreateDTO;
import com.eatzy.interaction.dto.response.ResFavouriteDTO;
import com.eatzy.interaction.service.FavoriteService;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ResponseEntity<ResFavouriteDTO> createFavourite(
            @Valid @RequestBody ReqFavoriteCreateDTO req) throws IdInvalidException {
        ResFavouriteDTO createdFavourite = favoriteService.createFavourite(req.getRestaurant().getId());
        return ResponseEntity.ok(createdFavourite);
    }

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAllFavourites(
            @Filter Specification<Favorite> spec, Pageable pageable) {
        return ResponseEntity.ok(favoriteService.getAllFavourites(spec, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResFavouriteDTO> getFavouriteById(@PathVariable("id") Long id)
            throws IdInvalidException {
        ResFavouriteDTO favourite = favoriteService.getFavouriteById(id);
        if (favourite == null) {
            throw new IdInvalidException("Customer favorite not found with id: " + id);
        }
        return ResponseEntity.ok(favourite);
    }

    @GetMapping("/my-favorites")
    public ResponseEntity<List<ResFavouriteDTO>> getMyFavorites() throws IdInvalidException {
        Long customerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(favoriteService.getFavouritesByCustomerId(customerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFavourite(@PathVariable("id") Long id) throws IdInvalidException {
        favoriteService.deleteFavourite(id);
        return ResponseEntity.noContent().build();
    }
}
