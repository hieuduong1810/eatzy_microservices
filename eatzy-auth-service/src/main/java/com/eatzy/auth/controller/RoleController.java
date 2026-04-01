package com.eatzy.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import com.eatzy.auth.domain.Role;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.auth.service.RoleService;
import com.eatzy.common.annotation.ApiMessage;
import com.eatzy.common.exception.IdInvalidException;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@RequestBody Role role) throws IdInvalidException {
        if (this.roleService.existsByName(role.getName())) {
            throw new IdInvalidException("Role name already exists: " + role.getName());
        }

        Role createdRole = roleService.createRole(role);
        return ResponseEntity.ok(createdRole);
    }

    @PutMapping("/roles")
    @ApiMessage("Update role")
    public ResponseEntity<Role> updateRole(@RequestBody Role role) throws IdInvalidException {

        Role updatedRole = roleService.updateRole(role);
        return ResponseEntity.ok(updatedRole);
    }

    @GetMapping("/roles")
    @ApiMessage("Get all roles")
    public ResponseEntity<ResultPaginationDTO> getAllRoles(
            @Filter Specification<Role> spec, Pageable pageable) {
        ResultPaginationDTO result = roleService.getAllRoles(spec, pageable);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/roles/{id}")
    @ApiMessage("Delete role by id")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") Long id) throws IdInvalidException {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            throw new IdInvalidException("Role not found with id: " + id);
        }
        roleService.deleteRole(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/roles/{id}")
    @ApiMessage("Get role by id")
    public ResponseEntity<Role> getRoleById(@PathVariable("id") Long id) throws IdInvalidException {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            throw new IdInvalidException("Role not found with id: " + id);
        }
        return ResponseEntity.ok(role);
    }

}
