package edu.eci.dosw.todo.entity;

import jakarta.persistence.*;

import java.util.List;
import java.util.Objects;

/**
 * ======================================================================
 * == PASO 2: Se crea la entidad (el espejo) que permite traducir java ==
 * ==         a la base de datos mapeando por ORM con JPA            ==
 * ======================================================================
 */

@Entity
@Table(name = "categories")
public class CategorieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", length = 40, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", length = 10, nullable = false)
    private CategorieColor color = CategorieColor.AZUL;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "task", fetch = FetchType.EAGER)
    private List<TaskEntity> tasks;

    public CategorieEntity() { }

    public long getId() { return this.id; }
    public String getName()  { return this.name; }
    public CategorieColor getColor()  { return this.color; }

    public void setId(long id) { this.id = id; }
    public void setName(String name)  { this.name = name; }
    public void setColor(CategorieColor color)  { this.color = CategorieColor.AZUL; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategorieEntity that = (CategorieEntity) o;

        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color);
    }
}