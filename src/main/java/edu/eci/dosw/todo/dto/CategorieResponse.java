package edu.eci.dosw.todo.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import edu.eci.dosw.todo.entity.CategorieColor;
import edu.eci.dosw.todo.entity.CategorieEntity;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL) // Oculta propiedades que vengan en null
public class CategorieResponse {

    private long id;
    private String name;
    private CategorieColor color;
    // Constructors
    public CategorieResponse (CategorieEntity entity){
        this.id = entity.getId();
        this.name = entity.getName();
        this.color = entity.getColor();
    }

    public long getId() { return this.id; }
    public String getName()  { return this.name; }
    public CategorieColor getColor()  { return this.color; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategorieResponse that = (CategorieResponse) o;

        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color);
    }
}
