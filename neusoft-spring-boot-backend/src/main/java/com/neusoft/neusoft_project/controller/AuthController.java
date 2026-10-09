package com.neusoft.neusoft_project.controller;

import com.neusoft.neusoft_project.common.Result;
import com.neusoft.neusoft_project.entity.Users;
import com.neusoft.neusoft_project.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173") // Allow Frontend Access
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * User Login
     * Input: { "username": "...", "password": "..." }
     * Output: { code: "200", data: { token: "...", user: {...} } }
     */
    @PostMapping("/login")
    public Result login(@RequestBody Users user) {
        return authService.login(user.getUsername(), user.getPassword());
    }

    /**
     * User Registration
     * Input: { "username": "...", "password": "...", "email": "...", "phone": "...", "role": "..." }
     * Output: { code: "200", msg: "Registration successful" }
     */
    @PostMapping("/register")
    public Result register(@RequestBody Users user) {
        return authService.register(user);
    }
}