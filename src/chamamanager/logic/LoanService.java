/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.logic;

import chamamanager.dao.LoanDAO;
import chamamanager.dao.RepaymentDAO;
import chamamanager.exceptions.InsufficientSavingsException;
import chamamanager.model.Loan;
import chamamanager.model.Repayment;
import java.util.List;

/**
 * Handles loan-related operations.
 *
 * @author gh7
 */
public class LoanService {

    private final LoanDAO loanDAO;
    private final RepaymentDAO repaymentDAO;

    public LoanService(LoanDAO loanDAO, RepaymentDAO repaymentDAO) {
        this.loanDAO = loanDAO;
        this.repaymentDAO = repaymentDAO;
    }

    /**
     * Issues a loan after savings validation.
     *
     * @param loan the loan to issue
     * @throws InsufficientSavingsException if the borrower lacks sufficient
     * savings
     */
    public void issueLoan(Loan loan) throws InsufficientSavingsException {
        // TODO: savings validation depends on a lookup method not yet defined
        // (pending in the DAO layer); add the savings check once finalized.
        loanDAO.create(loan);
    }

    /**
     * Retrieves a loan by identifier.
     *
     * @param id the loan identifier
     * @return the matching loan, or {@code null}
     */
    public Loan getLoan(int id) {
        return loanDAO.getById(id);
    }

    /**
     * Lists all loans.
     *
     * @return a list of all loans
     */
    public List<Loan> getAllLoans() {
        return loanDAO.getAll();
    }

    /**
     * Records a repayment against a loan.
     *
     * @param repayment the repayment to record
     */
    public void recordRepayment(Repayment repayment) {
        repaymentDAO.create(repayment);
    }

    /**
     * Lists all repayments.
     *
     * @return a list of all repayments
     */
    public List<Repayment> getAllRepayments() {
        return repaymentDAO.getAll();
    }
}
