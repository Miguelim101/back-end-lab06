package edu.eci.dosw.todo.exception;

public class CategorieNotFoundException extends RuntimeException{
    public CategorieNotFoundException(Long id) {
        super("Categorie with id " + id + " was not found");
    }
}
