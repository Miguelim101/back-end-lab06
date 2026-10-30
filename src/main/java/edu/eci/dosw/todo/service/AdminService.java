package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.AdminSummaryResponse;

/**
 * =================================================================================
 * == LAB 7: Operaciones exclusivas del rol ADMIN (solo información agregada)     ==
 * =================================================================================
 */
public interface AdminService {
    AdminSummaryResponse summary();
}
