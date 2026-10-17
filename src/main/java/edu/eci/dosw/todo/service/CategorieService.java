package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.*;

import java.util.List;

public interface CategorieService {
    List<CategorieResponse> findAll();
    CategorieResponse findById(Long id);
    CategorieResponse create(CategorieCreateRequest request);
    CategorieResponse update(Long id, CategorieUpdateRequest request);
    void delete(Long id);
}
