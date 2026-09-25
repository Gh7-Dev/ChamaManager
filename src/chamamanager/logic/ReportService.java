/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.logic;

import chamamanager.dao.ContributionDAO;
import chamamanager.dao.LoanDAO;
import chamamanager.dao.MemberDAO;
import chamamanager.dao.RepaymentDAO;
import chamamanager.model.Contribution;
import chamamanager.model.Loan;
import chamamanager.model.Member;
import chamamanager.model.Repayment;
import java.util.Collections;
import java.util.List;

/**
 * Handles report generation and aggregation based on the application's data.
 *
 * Aggregates member, contribution, loan, and repayment data surfaced through
 * the DAO classes.
 *
 * @author gh7
 */
public class ReportService {

    private final MemberDAO memberDAO;
    private final ContributionDAO contributionDAO;
    private final LoanDAO loanDAO;
    private final RepaymentDAO repaymentDAO;

    public ReportService(MemberDAO memberDAO, ContributionDAO contributionDAO,
            LoanDAO loanDAO, RepaymentDAO repaymentDAO) {
        this.memberDAO = memberDAO;
        this.contributionDAO = contributionDAO;
        this.loanDAO = loanDAO;
        this.repaymentDAO = repaymentDAO;
    }

    /**
     * Aggregated members for reporting.
     *
     * @return a list of all members
     */
    public List<Member> reportMembers() {
        return copy(memberDAO.getAll());
    }

    /**
     * Aggregated contributions for reporting.
     *
     * @return a list of all contributions
     */
    public List<Contribution> reportContributions() {
        return copy(contributionDAO.getAll());
    }

    /**
     * Aggregated loans for reporting.
     *
     * @return a list of all loans
     */
    public List<Loan> reportLoans() {
        return copy(loanDAO.getAll());
    }

    /**
     * Aggregated repayments for reporting.
     *
     * @return a list of all repayments
     */
    public List<Repayment> reportRepayments() {
        return copy(repaymentDAO.getAll());
    }

    private <T> List<T> copy(List<T> source) {
        if (source == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(source);
    }
}
