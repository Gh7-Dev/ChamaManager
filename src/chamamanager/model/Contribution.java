
package chamamanager.model;

import java.time.LocalDate;

/**
 * A contribution recorded against a member.
 *
 * @author gh7
 */
public class Contribution {

    private int id;
    private int memberId;
    private double amount;
    private LocalDate datePaid;
    private String period;
    private String status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDate getDatePaid() {
        return datePaid;
    }

    public void setDatePaid(LocalDate datePaid) {
        this.datePaid = datePaid;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }



 public int getContributionId(){
    return this.id;

}

 public void setContributionId(int contributionId){
    this.id = contributionId;
 }
}
