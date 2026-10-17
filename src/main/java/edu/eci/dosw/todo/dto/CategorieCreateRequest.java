package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.CategorieColor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public class CategorieCreateRequest {

    @NotBlank(message = "El nombre no puese ser vacío")
    @Size(max = 40, message = "El nombre no puede superar los 40 caracteres")
    private String name;

    @NotNull(message = "El color no puede ser vacío")
    private CategorieColor color = CategorieColor.AZUL;
    // Constructors
    public CategorieCreateRequest () { }
    public CategorieCreateRequest (String name,
                                   CategorieColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName()  { return this.name; }
        public CategorieColor getColor()  { return this.color; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategorieCreateRequest that = (CategorieCreateRequest) o;

        return Objects.equals(name, that.name) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, color);
    }
}

