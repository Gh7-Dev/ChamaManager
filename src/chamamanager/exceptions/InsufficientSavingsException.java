/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.exceptions;

/**
 * Custom exception for insufficient savings conditions.
 *
 * Thrown by the loan/savings logic when a member does not have enough savings
 * to support a requested operation.
 *
 * @author gh7
 */
public class InsufficientSavingsException extends Exception {

    public InsufficientSavingsException() {
        super();
    }

    public InsufficientSavingsException(String message) {
        super(message);
    }

    public InsufficientSavingsException(String message, Throwable cause) {
        super(message, cause);
    }
}
