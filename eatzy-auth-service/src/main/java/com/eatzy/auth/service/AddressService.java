package com.eatzy.auth.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.auth.domain.Address;
import com.eatzy.auth.domain.User;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.auth.domain.res.address.ResAddressDTO;
import com.eatzy.auth.repository.AddressRepository;
import com.eatzy.auth.mapper.AddressMapper;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.common.exception.IdInvalidException;

@Service
public class AddressService {
    private final AddressRepository addressRepository;
    private final UserService userService;
    private final AddressMapper addressMapper;

    public AddressService(AddressRepository addressRepository, UserService userService, AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.userService = userService;
        this.addressMapper = addressMapper;
    }

    public Address getAddressById(Long id) {
        Optional<Address> addressOpt = this.addressRepository.findById(id);
        return addressOpt.orElse(null);
    }

    public List<Address> getAddressesByCustomerId(Long customerId) {
        return this.addressRepository.findByCustomerId(customerId);
    }

    public Address createAddress(Address address) throws IdInvalidException {
        // check customer exists
        if (address.getCustomer() != null) {
            User customer = this.userService.getUserById(address.getCustomer().getId());
            if (customer == null) {
                throw new IdInvalidException("Customer not found with id: " + address.getCustomer().getId());
            }
            address.setCustomer(customer);
        } else {
            throw new IdInvalidException("Customer is required");
        }

        return addressRepository.save(address);
    }

    public Address updateAddress(Address address) throws IdInvalidException {
        // check id
        Address currentAddress = getAddressById(address.getId());
        if (currentAddress == null) {
            throw new IdInvalidException("Address not found with id: " + address.getId());
        }

        // update fields
        if (address.getAddressLine() != null) {
            currentAddress.setAddressLine(address.getAddressLine());
        }
        if (address.getLatitude() != null) {
            currentAddress.setLatitude(address.getLatitude());
        }
        if (address.getLongitude() != null) {
            currentAddress.setLongitude(address.getLongitude());
        }
        if (address.getLabel() != null) {
            currentAddress.setLabel(address.getLabel());
        }
        if (address.getCustomer() != null) {
            User customer = this.userService.getUserById(address.getCustomer().getId());
            if (customer == null) {
                throw new IdInvalidException("Customer not found with id: " + address.getCustomer().getId());
            }
            currentAddress.setCustomer(customer);
        }

        return addressRepository.save(currentAddress);
    }

    public ResultPaginationDTO getAllAddresses(Specification<Address> spec, Pageable pageable) {
        Page<Address> page = this.addressRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);

        List<ResAddressDTO> dtoList = page.getContent().stream()
                .map(addressMapper::convertToDTO)
                .collect(Collectors.toList());
        result.setResult(dtoList);
        return result;
    }

    public void deleteAddress(Long id) {
        this.addressRepository.deleteById(id);
    }

    // Methods for current logged-in user

    public List<ResAddressDTO> getCurrentUserAddresses() throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        List<Address> addresses = this.addressRepository.findByCustomerId(user.getId());
        return addresses.stream()
                .map(addressMapper::convertToDTO)
                .collect(Collectors.toList());
    }

    public ResAddressDTO getCurrentUserAddressById(Long id) throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        Address address = this.getAddressById(id);
        if (address == null) {
            throw new IdInvalidException("Address not found with id: " + id);
        }

        // Check if address belongs to current user
        if (!address.getCustomer().getId().equals(user.getId())) {
            throw new IdInvalidException("You do not have permission to access this address");
        }

        return addressMapper.convertToDTO(address);
    }

    public Address createCurrentUserAddress(Address address) throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        address.setCustomer(user);
        return addressRepository.save(address);
    }

    public Address updateCurrentUserAddress(Long id, Address address) throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        Address currentAddress = this.getAddressById(id);
        if (currentAddress == null) {
            throw new IdInvalidException("Address not found with id: " + id);
        }

        // Check if address belongs to current user
        if (!currentAddress.getCustomer().getId().equals(user.getId())) {
            throw new IdInvalidException("You do not have permission to modify this address");
        }

        // Update fields
        if (address.getAddressLine() != null) {
            currentAddress.setAddressLine(address.getAddressLine());
        }
        if (address.getLatitude() != null) {
            currentAddress.setLatitude(address.getLatitude());
        }
        if (address.getLongitude() != null) {
            currentAddress.setLongitude(address.getLongitude());
        }
        if (address.getLabel() != null) {
            currentAddress.setLabel(address.getLabel());
        }

        return addressRepository.save(currentAddress);
    }

    public void deleteCurrentUserAddress(Long id) throws IdInvalidException {
        String userEmail = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("No authenticated user found"));

        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("User not found with email: " + userEmail);
        }

        Address address = this.getAddressById(id);
        if (address == null) {
            throw new IdInvalidException("Address not found with id: " + id);
        }

        // Check if address belongs to current user
        if (!address.getCustomer().getId().equals(user.getId())) {
            throw new IdInvalidException("You do not have permission to delete this address");
        }

        this.addressRepository.deleteById(id);
    }
}
