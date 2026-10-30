package edu.eci.dosw.todo.dto;

import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * ==========================================================================
 * == LAB 7: Datos que envía un visitante para crear su cuenta. No incluye ==
 * ==        rol: el registro público siempre crea usuarios USER          ==
 * ==========================================================================
 */

/**
 * name
 * email
 * password
 */
public class RegisterRequest {

    @NotBlank(message = "El nombre no puede ser vacío")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String name;

    @NotBlank(message = "El correo no puede ser vacío")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
    private String email;

    @NotBlank(message = "La contraseña no puede ser vacía")
    // BCrypt solo procesa los primeros 72 bytes, por eso se limita el máximo
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    // Constructors
    public RegisterRequest () { }
    public RegisterRequest (String name, String email, String password){
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName() { return this.name; }
    public String getEmail() { return this.email; }
    public String getPassword() { return this.password; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterRequest that = (RegisterRequest) o;

        return Objects.equals(name, that.name) &&
                Objects.equals(email, that.email) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, password);
    }
}
