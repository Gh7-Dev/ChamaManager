/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.logic;

import chamamanager.dao.ContributionDAO;
import chamamanager.exceptions.DuplicateContributionException;
import chamamanager.model.Contribution;
import java.util.List;

/**
 * Handles contribution-related business operations.
 *
 * @author gh7
 */
public class ContributionService {

    private final ContributionDAO contributionDAO;

    public ContributionService(ContributionDAO contributionDAO) {
        this.contributionDAO = contributionDAO;
    }

    /**
     * Records a contribution, rejecting it if it already exists.
     *
     * @param contribution the contribution to record
     * @throws DuplicateContributionException if the contribution is a duplicate
     * of one already recorded
     */
    public void recordContribution(Contribution contribution)
            throws DuplicateContributionException {
        // TODO: duplicate-checking depends on a lookup method not yet defined
        // (pending in the DAO layer); add the duplicate rejection once finalized.
        contributionDAO.create(contribution);
    }

    /**
     * Retrieves a contribution by identifier.
     *
     * @param id the contribution identifier
     * @return the matching contribution, or {@code null}
     */
    public Contribution getContribution(int id) {
        return contributionDAO.getById(id);
    }

    /**
     * Lists all contributions.
     *
     * @return a list of all contributions
     */
    public List<Contribution> getAllContributions() {
        return contributionDAO.getAll();
    }
}
