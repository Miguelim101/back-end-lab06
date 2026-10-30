package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.RegisterRequest;
import edu.eci.dosw.todo.dto.UserResponse;

/**
 * =================================================================================
 * == LAB 7: Contrato de registro y autenticación de usuarios                     ==
 * =================================================================================
 */
public interface AuthService {
    UserResponse register(RegisterRequest request);
}
