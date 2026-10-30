package edu.eci.dosw.todo.exception;

public class InvalidCredentialsException extends RuntimeException {

    // Mismo mensaje si el correo no existe o la contraseña es incorrecta: no revela qué cuentas existen
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
