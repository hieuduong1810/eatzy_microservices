package com.eatzy.auth.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.auth.domain.Permission;
import com.eatzy.auth.domain.Role;
import com.eatzy.auth.repository.PermissionRepository;
import com.eatzy.auth.repository.RoleRepository;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.common.dto.ResultPaginationDTO;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }

    public Role getRoleById(Long id) {
        Optional<Role> roleOpt = this.roleRepository.findById(id);
        if (roleOpt.isPresent()) {
            return roleOpt.get();
        }
        return null;
    }

    public Role createRole(Role role) {
        // check permission exists
        if (role.getPermissions() != null) {
            List<Long> reqRoles = role.getPermissions().stream()
                    .map(permission -> permission.getId())
                    .collect(Collectors.toList());
            List<Permission> permissions = this.permissionRepository.findByIdIn(reqRoles);
            role.setPermissions(permissions);
        }
        return roleRepository.save(role);
    }

    public Role updateRole(Role role) throws IdInvalidException {

        // check id
        Role currentRole = getRoleById(role.getId());
        if (currentRole == null) {
            throw new IdInvalidException("Role not found with id: " + role.getId());
        }

        // check name
        if (role.getName() != null && !role.getName().equals(currentRole.getName())) {
            // if (this.existsByName(role.getName())) {
            // throw new IdInvalidException("Role name already exists: " + role.getName());
            // }
            currentRole.setName(role.getName());
        }
        if (role.getDescription() != null) {
            currentRole.setDescription(role.getDescription());
        }
        if (role.getPermissions() != null) {
            List<Long> reqRoles = role.getPermissions().stream()
                    .map(permission -> permission.getId())
                    .collect(Collectors.toList());
            List<Permission> permissions = this.permissionRepository.findByIdIn(reqRoles);
            currentRole.setPermissions(permissions);
        }
        currentRole.setActive(role.isActive());
        return roleRepository.save(currentRole);
    }

    public ResultPaginationDTO getAllRoles(Specification<Role> spec,
            Pageable pageable) {
        Page<Role> page = this.roleRepository.findAll(spec, pageable);
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

    public void deleteRole(Long id) {
        this.roleRepository.deleteById(id);
    }
}
