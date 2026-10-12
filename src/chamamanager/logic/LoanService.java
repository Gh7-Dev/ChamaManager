package chamamanager.logic;

import chamamanager.dao.LoanDAO;
import chamamanager.dao.RepaymentDAO;
import chamamanager.exceptions.InsufficientSavingsException;
import chamamanager.exceptions.InvalidRepaymentException;
import chamamanager.exceptions.LoanStateException;
import chamamanager.model.Loan;
import chamamanager.model.Repayment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Loan business rules: apply, approve (= issue), reject, repay, arrears.
 *
 * @author gh7
 */
public class LoanService {

    /**
     * A member may borrow at most this many times their CONFIRMED savings.
     */
    public static final double BORROW_LIMIT_MULTIPLIER = 3.0;

    /**
     * An ACTIVE loan overdue by MORE than this many days becomes DEFAULTED.
     */
    public static final int DEFAULT_AFTER_DAYS = 30;

    /**
     * false = rules exactly as decided: DEFAULTED loans leave the arrears list
     * and cannot take repayments. true = defaulted loans stay collectable
     * (still listed in arrears, repayments allowed, can still reach PAID).
     */
    public static final boolean COLLECT_ON_DEFAULTED = false;

    // TEMP: replace with Person A's role constant/check once merged.
    public static final String ROLE_TREASURER = "TREASURER";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_DEFAULTED = "DEFAULTED";

    private final LoanDAO loanDAO;
    private final RepaymentDAO repaymentDAO;

    public LoanService(LoanDAO loanDAO, RepaymentDAO repaymentDAO) {
        this.loanDAO = loanDAO;
        this.repaymentDAO = repaymentDAO;
    }

    /**
     * One row of the arrears list.
     */
    public static class ArrearsEntry {

        private final Loan loan;
        private final long daysOverdue;
        private final BigDecimal outstanding;

        public ArrearsEntry(Loan loan, long daysOverdue, BigDecimal outstanding) {
            this.loan = loan;
            this.daysOverdue = daysOverdue;
            this.outstanding = outstanding;
        }

        public Loan getLoan() {
            return loan;
        }

        public long getDaysOverdue() {
            return daysOverdue;
        }

        public BigDecimal getOutstanding() {
            return outstanding;
        }
    }

    // ------------------------------------------------------------ savings/limit
    // TEMP: delegates to LoanDAO's own query until Person B's total is merged.
    public BigDecimal getConfirmedSavings(int memberId) {
        return loanDAO.getConfirmedSavings(memberId).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBorrowLimit(int memberId) {
        return getConfirmedSavings(memberId)
                .multiply(BigDecimal.valueOf(BORROW_LIMIT_MULTIPLIER))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------ apply/approve/reject
    /**
     * A member applies for a loan. Creates a PENDING loan with no rate or
     * dates.
     *
     * @throws InsufficientSavingsException if principal is more than the borrow
     * limit
     * @throws LoanStateException if the member already has an ACTIVE loan
     */
    public Loan applyForLoan(int memberId, double principal)
            throws InsufficientSavingsException, LoanStateException {
        BigDecimal amount = money(principal);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero.");
        }
        checkNoActiveLoan(memberId);
        checkBorrowLimit(memberId, amount);

        Loan loan = new Loan();
        loan.setMemberId(memberId);
        loan.setPrincipal(amount.doubleValue());
        loan.setInterestRate(0);
        loan.setStatus(STATUS_PENDING);
        loanDAO.create(loan);
        return loan;
    }

    /**
     * Treasurer approves a PENDING loan. Approval = issue: the loan becomes
     * ACTIVE immediately, date_issued = today.
     *
     * @param interestRate as a fraction, e.g. 0.10 for 10%
     */
    public Loan approveLoan(String callerRole, int loanId, double interestRate, LocalDate dueDate)
            throws LoanStateException, InsufficientSavingsException {
        requireTreasurer(callerRole);
        Loan loan = requireLoan(loanId);
        if (!STATUS_PENDING.equals(loan.getStatus())) {
            throw new LoanStateException("Loan #" + loanId + " is " + loan.getStatus()
                    + ", only PENDING loans can be approved.");
        }
        if (interestRate < 0 || interestRate > 1) {
            throw new IllegalArgumentException("Interest rate must be between 0% and 100%.");
        }
        if (dueDate == null || !dueDate.isAfter(today())) {
            throw new IllegalArgumentException("Due date must be after today.");
        }
        checkNoActiveLoan(loan.getMemberId());
        checkBorrowLimit(loan.getMemberId(), money(loan.getPrincipal()));

        if (!loanDAO.issuePending(loanId, interestRate, today(), dueDate)) {
            throw new LoanStateException("Loan #" + loanId + " was changed by someone else. Refresh and try again.");
        }
        return loanDAO.getById(loanId);
    }

    /**
     * Treasurer rejects a PENDING loan.
     */
    public void rejectLoan(String callerRole, int loanId) throws LoanStateException {
        requireTreasurer(callerRole);
        Loan loan = requireLoan(loanId);
        if (!STATUS_PENDING.equals(loan.getStatus())) {
            throw new LoanStateException("Loan #" + loanId + " is " + loan.getStatus()
                    + ", only PENDING loans can be rejected.");
        }
        if (!loanDAO.changeStatus(loanId, STATUS_PENDING, STATUS_REJECTED)) {
            throw new LoanStateException("Loan #" + loanId + " was changed by someone else. Refresh and try again.");
        }
    }

    // ------------------------------------------------------------ money maths
    /**
     * Flat interest: principal * (1 + rate), rounded to 2 dp.
     */
    public BigDecimal getTotalPayable(Loan loan) {
        BigDecimal principal = BigDecimal.valueOf(loan.getPrincipal());
        BigDecimal rate = BigDecimal.valueOf(loan.getInterestRate());
        return principal.add(principal.multiply(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAmountRepaid(int loanId) {
        return repaymentDAO.sumByLoanId(loanId).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Total payable minus repayments. PENDING/REJECTED loans have no balance
     * (0.00).
     */
    public BigDecimal getOutstandingBalance(Loan loan) {
        if (STATUS_PENDING.equals(loan.getStatus()) || STATUS_REJECTED.equals(loan.getStatus())) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal outstanding = getTotalPayable(loan).subtract(getAmountRepaid(loan.getId()));
        return outstanding.signum() < 0 ? BigDecimal.ZERO.setScale(2) : outstanding;
    }

    public BigDecimal getOutstandingBalance(int loanId) throws LoanStateException {
        return getOutstandingBalance(requireLoan(loanId));
    }

    // ------------------------------------------------------------ repayments
    /**
     * Treasurer records a repayment (partial allowed). Blocks overpaying. When
     * the balance reaches 0 the loan becomes PAID in the same transaction.
     */
    public Repayment recordRepayment(String callerRole, int loanId, double amountPaid)
            throws InvalidRepaymentException {
        requireTreasurer(callerRole);
        BigDecimal amount = BigDecimal.valueOf(amountPaid);
        if (amount.signum() <= 0) {
            throw new InvalidRepaymentException("Repayment amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidRepaymentException("Repayment amount can have at most 2 decimal places.");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        Loan loan = loanDAO.getById(loanId);
        if (loan == null) {
            throw new InvalidRepaymentException("Loan #" + loanId + " does not exist.");
        }
        boolean repayable = STATUS_ACTIVE.equals(loan.getStatus())
                || (COLLECT_ON_DEFAULTED && STATUS_DEFAULTED.equals(loan.getStatus()));
        if (!repayable) {
            throw new InvalidRepaymentException("Loan #" + loanId + " is " + loan.getStatus()
                    + ". Repayments are only accepted for ACTIVE loans.");
        }
        BigDecimal outstanding = getOutstandingBalance(loan);
        if (amount.compareTo(outstanding) > 0) {
            throw new InvalidRepaymentException(String.format(
                    "Amount KES %,.2f is more than the outstanding balance of KES %,.2f.", amount, outstanding));
        }
        String newStatus = amount.compareTo(outstanding) == 0 ? STATUS_PAID : null;

        Repayment repayment = new Repayment();
        repayment.setLoanId(loanId);
        repayment.setAmountPaid(amount.doubleValue());
        repayment.setDatePaid(today());
        repaymentDAO.createAndSetLoanStatus(repayment, newStatus);
        return repayment;
    }

    // ------------------------------------------------------------ arrears/defaults
    /**
     * Turns ACTIVE loans overdue by more than DEFAULT_AFTER_DAYS into
     * DEFAULTED. Call on screen load and before getArrears().
     *
     * @return how many loans were changed
     */
    public int refreshLoanStatuses() {
        int changed = 0;
        LocalDate today = today();
        for (Loan loan : loanDAO.getByStatus(STATUS_ACTIVE)) {
            if (loan.getDueDate() == null) {
                continue;
            }
            long overdue = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            if (overdue > DEFAULT_AFTER_DAYS && getOutstandingBalance(loan).signum() > 0) {
                if (loanDAO.changeStatus(loan.getId(), STATUS_ACTIVE, STATUS_DEFAULTED)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    /**
     * Loan arrears: ACTIVE loans past due_date with a balance, most overdue
     * first. (Includes DEFAULTED ones only if COLLECT_ON_DEFAULTED is true.)
     * Does NOT call refreshLoanStatuses(); call that first.
     */
    public List<ArrearsEntry> getArrears() {
        List<Loan> candidates = new ArrayList<>(loanDAO.getByStatus(STATUS_ACTIVE));
        if (COLLECT_ON_DEFAULTED) {
            candidates.addAll(loanDAO.getByStatus(STATUS_DEFAULTED));
        }
        List<ArrearsEntry> arrears = new ArrayList<>();
        LocalDate today = today();
        for (Loan loan : candidates) {
            if (loan.getDueDate() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            BigDecimal outstanding = getOutstandingBalance(loan);
            if (days > 0 && outstanding.signum() > 0) {
                arrears.add(new ArrearsEntry(loan, days, outstanding));
            }
        }
        arrears.sort((a, b) -> Long.compare(b.getDaysOverdue(), a.getDaysOverdue()));
        return arrears;
    }

    // ------------------------------------------------------------ simple lookups
    public Loan getLoan(int id) {
        return loanDAO.getById(id);
    }

    public List<Loan> getAllLoans() {
        return loanDAO.getAll();
    }

    public List<Loan> getLoansByMember(int memberId) {
        return loanDAO.getByMemberId(memberId);
    }

    public List<Loan> getPendingLoans() {
        return loanDAO.getByStatus(STATUS_PENDING);
    }

    /**
     * Loans that can currently take a repayment.
     *
     * @return
     */
    public List<Loan> getRepayableLoans() {
        List<Loan> loans = new ArrayList<>(loanDAO.getByStatus(STATUS_ACTIVE));
        if (COLLECT_ON_DEFAULTED) {
            loans.addAll(loanDAO.getByStatus(STATUS_DEFAULTED));
        }
        return loans;
    }

    public List<Repayment> getRepaymentsForLoan(int loanId) {
        return repaymentDAO.getByLoanId(loanId);
    }

    public List<Repayment> getAllRepayments() {
        return repaymentDAO.getAll();
    }

    // ----------- private helpers
    protected LocalDate today() {
        return LocalDate.now();
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    // TEMP: replace with Person A's role check once merged.
    private void requireTreasurer(String callerRole) {
        if (!ROLE_TREASURER.equals(callerRole)) {
            throw new SecurityException("Only the treasurer can do this.");
        }
    }

    private Loan requireLoan(int loanId) throws LoanStateException {
        Loan loan = loanDAO.getById(loanId);
        if (loan == null) {
            throw new LoanStateException("Loan #" + loanId + " does not exist.");
        }
        return loan;
    }

    private void checkNoActiveLoan(int memberId) throws LoanStateException {
        if (!loanDAO.getByMemberAndStatus(memberId, STATUS_ACTIVE).isEmpty()) {
            throw new LoanStateException("Member #" + memberId + " already has an ACTIVE loan. "
                    + "Only one active loan at a time is allowed.");
        }
    }

    private void checkBorrowLimit(int memberId, BigDecimal principal) throws InsufficientSavingsException {
        BigDecimal savings = getConfirmedSavings(memberId);
        BigDecimal limit = getBorrowLimit(memberId);
        if (principal.compareTo(limit) > 0) {
            throw new InsufficientSavingsException(String.format(
                    "KES %,.2f is above the borrowing limit of KES %,.2f (%.0fx confirmed savings of KES %,.2f).",
                    principal, limit, BORROW_LIMIT_MULTIPLIER, savings));
        }
    }
}
