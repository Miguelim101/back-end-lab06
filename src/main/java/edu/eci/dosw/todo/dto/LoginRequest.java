package edu.eci.dosw.todo.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

/**
 * email
 * password
 */
public class LoginRequest {

    @NotBlank(message = "El correo no puede ser vacío")
    private String email;

    @NotBlank(message = "La contraseña no puede ser vacía")
    private String password;

    // Constructors
    public LoginRequest () { }
    public LoginRequest (String email, String password){
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return this.email; }
    public String getPassword() { return this.password; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginRequest that = (LoginRequest) o;

        return Objects.equals(email, that.email) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, password);
    }
}
