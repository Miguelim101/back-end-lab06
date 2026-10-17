package edu.eci.dosw.todo.service;




import edu.eci.dosw.todo.dto.CategorieCreateRequest;
import edu.eci.dosw.todo.dto.CategorieResponse;
import edu.eci.dosw.todo.dto.CategorieUpdateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.entity.*;
import edu.eci.dosw.todo.exception.CategorieNotFoundException;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.repository.CategorieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategorieServiceImpl implements CategorieService {

    private final CategorieRepository categorieRepository;

    public CategorieServiceImpl(CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorieResponse> findAll() {
        return categorieRepository.findAll().stream()
                .map(CategorieResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategorieResponse findById(Long id) {
        CategorieEntity task = categorieRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return new CategorieResponse(task);
    }

    @Override
    @Transactional
    public CategorieResponse create(CategorieCreateRequest request) {
        CategorieEntity categorie = new CategorieEntity();

        categorie.setName(request.getName());

        if (request.getColor() != null) {
            categorie.setColor(request.getColor());
        }
        else {
            categorie.setColor(CategorieColor.AZUL);
        }

        CategorieEntity savedCategorie = categorieRepository.save(categorie);
        return new CategorieResponse(savedCategorie);
    }

    @Override
    @Transactional
    public CategorieResponse update(Long id, CategorieUpdateRequest request) {
        CategorieEntity existingCategorie = categorieRepository.findById(id)
                .orElseThrow(() -> new CategorieNotFoundException(id));

        existingCategorie.setName(request.getName());

        if (request.getColor() != null) {
            existingCategorie.setColor(request.getColor());
        }
        else {
            existingCategorie.setColor(CategorieColor.AZUL);
        }

        CategorieEntity updatedCategorie = CategorieRepository.save(existingCategorie);
        return new CategorieResponse(updatedCategorie);
    }

    @Override
    public void delete(Long id) {
        if(!categorieRepository.existsById(id)){
            throw new TaskNotFoundException(id);
        }
        categorieRepository.deleteById(id);
    }
}

