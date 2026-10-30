package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.UserResponse;

/**
 * =================================================================================
 * == LAB 7: Consulta del perfil del usuario autenticado (datos no sensibles)     ==
 * =================================================================================
 */
public interface UserService {
    UserResponse findById(Long id);
}
