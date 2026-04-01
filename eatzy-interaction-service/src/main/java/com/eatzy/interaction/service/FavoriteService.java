package com.eatzy.interaction.service;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.interaction.designpattern.adapter.AuthServiceClient;
import com.eatzy.interaction.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.interaction.domain.Favorite;
import com.eatzy.interaction.dto.response.ResFavouriteDTO;
import com.eatzy.interaction.repository.FavoriteRepository;
import feign.FeignException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final AuthServiceClient authServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           AuthServiceClient authServiceClient,
                           RestaurantServiceClient restaurantServiceClient) {
        this.favoriteRepository = favoriteRepository;
        this.authServiceClient = authServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
    }

    public ResFavouriteDTO convertToDTO(Favorite favorite) {
        if (favorite == null) {
            return null;
        }
        ResFavouriteDTO dto = new ResFavouriteDTO();
        dto.setId(favorite.getId());

        // Fetch User
        try {
            Map<String, Object> userResponse = authServiceClient.getUserById(favorite.getCustomerId());
            if (userResponse != null && userResponse.get("result") != null) {
                Map<String, Object> userMap = (Map<String, Object>) userResponse.get("result");
                ResFavouriteDTO.User customerDTO = new ResFavouriteDTO.User();
                customerDTO.setId(favorite.getCustomerId());
                customerDTO.setName((String) userMap.get("name"));
                dto.setCustomer(customerDTO);
            }
        } catch (FeignException e) {
            // User not found or auth service down
        }

        // Fetch Restaurant
        try {
            Map<String, Object> restaurantResponse = restaurantServiceClient.getRestaurantById(favorite.getRestaurantId());
            if (restaurantResponse != null && restaurantResponse.get("result") != null) {
                Map<String, Object> restMap = (Map<String, Object>) restaurantResponse.get("result");
                ResFavouriteDTO.Restaurant restaurantDTO = new ResFavouriteDTO.Restaurant();
                restaurantDTO.setId(favorite.getRestaurantId());
                restaurantDTO.setName((String) restMap.get("name"));
                restaurantDTO.setSlug((String) restMap.get("slug"));
                restaurantDTO.setAddress((String) restMap.get("address"));
                restaurantDTO.setDescription((String) restMap.get("description"));
                restaurantDTO.setStatus((String) restMap.get("status"));

                if (restMap.get("averageRating") != null) {
                    restaurantDTO.setAverageRating(new BigDecimal(restMap.get("averageRating").toString()));
                }
                restaurantDTO.setImageUrl((String) restMap.get("imageUrl"));

                // Map restaurantTypes
                if (restMap.get("restaurantTypes") != null) {
                    List<Map<String, Object>> typesList = (List<Map<String, Object>>) restMap.get("restaurantTypes");
                    restaurantDTO.setRestaurantTypes(typesList.stream().map(type -> {
                        ResFavouriteDTO.RestaurantType typeDTO = new ResFavouriteDTO.RestaurantType();
                        typeDTO.setId(Long.valueOf(type.get("id").toString()));
                        typeDTO.setName((String) type.get("name"));
                        return typeDTO;
                    }).collect(Collectors.toList()));
                }
                dto.setRestaurant(restaurantDTO);
            }
        } catch (FeignException e) {
            // Restaurant not found or service down
        }

        return dto;
    }

    public ResFavouriteDTO getFavouriteById(Long id) {
        Optional<Favorite> favouriteOpt = this.favoriteRepository.findById(id);
        return convertToDTO(favouriteOpt.orElse(null));
    }

    public List<ResFavouriteDTO> getFavouritesByCustomerId(Long customerId) {
        return this.favoriteRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public ResFavouriteDTO createFavourite(Long restaurantId) throws IdInvalidException {
        Long customerId = SecurityUtils.getCurrentUserId();

        // Check if restaurant exists
        try {
            Map<String, Object> restaurantResponse = restaurantServiceClient.getRestaurantById(restaurantId);
            if (restaurantResponse == null || restaurantResponse.get("result") == null) {
                throw new IdInvalidException("Restaurant not found with id: " + restaurantId);
            }
        } catch (FeignException.NotFound e) {
            throw new IdInvalidException("Restaurant not found with id: " + restaurantId);
        }

        // Check if already favorited
        boolean exists = this.favoriteRepository.existsByCustomerIdAndRestaurantId(customerId, restaurantId);
        if (exists) {
            throw new IdInvalidException("This restaurant is already in favorites");
        }

        Favorite favourite = new Favorite();
        favourite.setCustomerId(customerId);
        favourite.setRestaurantId(restaurantId);

        Favorite savedFavorite = favoriteRepository.save(favourite);
        return convertToDTO(savedFavorite);
    }

    public void deleteFavourite(Long id) throws IdInvalidException {
        Long customerId = SecurityUtils.getCurrentUserId();
        Favorite favorite = favoriteRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Favorite not found with id: " + id));

        if (!favorite.getCustomerId().equals(customerId)) {
            throw new IdInvalidException("You do not have permission to delete this favorite");
        }
        this.favoriteRepository.deleteById(id);
    }

    public ResultPaginationDTO getAllFavourites(Specification<Favorite> spec, Pageable pageable) {
        Page<Favorite> page = this.favoriteRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        List<ResFavouriteDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        result.setResult(dtoList);
        return result;
    }
}
