package fr.lesprojetscagnottes.core.repository;

import fr.lesprojetscagnottes.core.entity.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    Page<Donation> findAll(Pageable pageable);

    // TODO : Retrieve it by jointures with accounts
    Set<Donation> findAllByBudgetId(long budgetId);

    Set<Donation> findAllByCampaignId(long campaignId);

    Set<Donation> findAllByAccountId(Long id);

    Set<Donation> findAllByContributorIdOrderByBudgetIdAsc(long contributorId);

    Set<Donation> findAllByContributorIdAndBudgetId(long contributorId, long budgetId);

    Page<Donation> findByCampaign_idOrderByIdAsc(long id, Pageable pageable);

    void deleteByCampaignId(Long id);

    @Query(value = "SELECT create_donation(:_account_id, :_campaign_id, :_budget_id, :_amount);", nativeQuery = true)
    boolean createDonation(@Param("_account_id") long accountId, @Param("_campaign_id") long campaignId, @Param("_budget_id") long budgetId, @Param("_amount") float amount);

    @Query(value = "SELECT delete_donation(:_donation_id);", nativeQuery = true)
    boolean deleteDonation(@Param("_donation_id") long donationId);

}