package com.smartfood.expiry.controller;

import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.enums.RoleType;
import com.smartfood.expiry.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("activeNav", "users");
        return "users/list";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("roles", RoleType.values());
        model.addAttribute("activeNav", "users");
        return "users/form";
    }

    @PostMapping("/save")
    public String saveUser(@RequestParam("username") String username,
                           @RequestParam("email") String email,
                           @RequestParam("fullName") String fullName,
                           @RequestParam("password") String password,
                           @RequestParam("role") RoleType role,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(username, email, password, fullName, role);
            redirectAttributes.addFlashAttribute("successMessage", "User '" + username + "' created successfully.");
            return "redirect:/users";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create user: " + ex.getMessage());
            return "redirect:/users/new";
        }
    }

    @PostMapping("/toggle/{id}")
    public String toggleStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/users";
    }
}
