/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.exceptions;

/**
 * Custom exception for invalid authentication conditions.
 *
 * Thrown by the authentication logic when login credentials cannot be
 * validated successfully.
 *
 * @author gh7
 */
public class InvalidLoginException extends Exception {

    public InvalidLoginException() {
        super();
    }

    public InvalidLoginException(String message) {
        super(message);
    }

    public InvalidLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
