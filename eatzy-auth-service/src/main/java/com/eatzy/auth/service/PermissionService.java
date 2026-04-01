package com.eatzy.auth.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.auth.domain.Permission;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.auth.repository.PermissionRepository;
import com.eatzy.common.exception.IdInvalidException;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public Permission getPermissionById(Long id) {
        Optional<Permission> permissionOpt = this.permissionRepository.findById(id);
        if (permissionOpt.isPresent()) {
            return permissionOpt.get();
        }
        return null;
    }

    public boolean checkPermissionExists(Permission permission) {
        return permissionRepository.existsByApiPathAndMethodAndModule(permission.getApiPath(), permission.getMethod(),
                permission.getModule());
    }

    public Permission createPermission(Permission permission) {
        return permissionRepository.save(permission);
    }

    public Permission updatePermission(Permission permission) throws IdInvalidException {

        // check id
        Permission currentPermission = getPermissionById(permission.getId());
        if (currentPermission == null) {
            throw new IdInvalidException("Permission not found with id: " + permission.getId());
        }

        if (permission.getApiPath() != null) {
            currentPermission.setApiPath(permission.getApiPath());
        }
        if (permission.getName() != null) {
            currentPermission.setName(permission.getName());
        }
        if (permission.getMethod() != null) {
            currentPermission.setMethod(permission.getMethod());
        }
        if (permission.getModule() != null) {
            currentPermission.setModule(permission.getModule());
        }

        // check exist
        boolean isPermissionExist = checkPermissionExists(currentPermission);
        if (isPermissionExist) {
            if (permission.getName() != null && !permission.getName().equals(currentPermission.getName())) {
                throw new IdInvalidException("Permission already exists: " + currentPermission.getName());
            }
        }
        return permissionRepository.save(currentPermission);
    }

    public ResultPaginationDTO getAllPermissions(Specification<Permission> spec,
            Pageable pageable) {
        Page<Permission> page = this.permissionRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent());
        return result;
    }

    public void deletePermission(Long id) {
        Optional<Permission> permissionOpt = this.permissionRepository.findById(id);
        if (permissionOpt.isPresent()) {
            Permission permission = permissionOpt.get();
            if (permission.getRoles() != null) {
                permission.getRoles().forEach(role -> {
                    role.getPermissions().remove(permission);
                });
            }
            this.permissionRepository.deleteById(id);
        }
    }
}
