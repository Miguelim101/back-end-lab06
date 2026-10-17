package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.CategorieColor;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Objects;

public class CategorieUpdateRequest {

    @NotBlank(message = "El nombre no puede ser vacío")
    @Size(max = 40, message = "El nombre no puede tener más de 40 caracteres")
    private String name;

    @NotNull(message = "El color no puede ser vacío")
    private CategorieColor color  = CategorieColor.AZUL;

    // Constructors
    public CategorieUpdateRequest () { }
    public CategorieUpdateRequest (String name,
                              CategorieColor color){
        this.name = name;
        this.color = color;
    }

    public String getName()  { return this.name; }
    public CategorieColor getColor()  { return this.color; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategorieUpdateRequest that = (CategorieUpdateRequest) o;

        return Objects.equals(name, that.color) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, color);
    }
}
