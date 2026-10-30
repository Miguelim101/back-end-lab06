package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.UserEntity;

import java.util.Objects;

/**
 * ==========================================================================
 * == LAB 7: Datos NO sensibles del usuario. Nunca expone la contraseña,   ==
 * ==        su hash ni secretos del servidor                              ==
 * ==========================================================================
 */

/**
 * id
 * name
 * email
 * role
 */
public class UserResponse {

    private long id;
    private String name;
    private String email;
    private Role role;

    // Constructors
    public UserResponse (UserEntity entity){
        this.id = entity.getId();
        this.name = entity.getName();
        this.email = entity.getEmail();
        this.role = entity.getRole();
    }

    public long getId() { return this.id; }
    public String getName() { return this.name; }
    public String getEmail() { return this.email; }
    public Role getRole() { return this.role; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserResponse that = (UserResponse) o;

        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(email, that.email) &&
                Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, role);
    }
}
