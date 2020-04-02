package fr.thomah.valyou.steps;

import fr.thomah.valyou.component.AuthenticationHttpClient;
import fr.thomah.valyou.component.BudgetHttpClient;
import fr.thomah.valyou.component.CucumberContext;
import fr.thomah.valyou.entity.AuthenticationResponse;
import fr.thomah.valyou.entity.Budget;
import fr.thomah.valyou.repository.BudgetRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;

public class BudgetStepDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetStepDefinitions.class);

    @Autowired
    private AuthenticationHttpClient authenticationHttpClient;

    @Autowired
    private BudgetHttpClient budgetHttpClient;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CucumberContext context;

    @And("The following budgets are available")
    public void theFollowingBudgetsAreAvailable(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);

        // Get dates for budget
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.with(firstDayOfYear());
        LocalDate lastDay = now.with(lastDayOfYear());

        Budget budget;
        for (Map<String, String> columns : rows) {

            // Create budget
            budget = new Budget();
            budget.setName(columns.get("name"));
            budget.setAmountPerMember(Float.parseFloat(columns.get("amountPerMember")));
            budget.setSponsor(context.getUsers().get(columns.get("sponsor")));
            budget.setRules(context.getContents().get(columns.get("rules")));
            budget.setStartDate(Date.valueOf(firstDay));
            budget.setEndDate(Date.valueOf(lastDay));
            budget.setOrganization(context.getOrganizations().get(columns.get("organization")));
            budget.setIsDistributed(Boolean.valueOf(columns.get("isDistributed")));
            budget = budgetRepository.save(budget);

            // Save in Test Map
            context.getBudgets().put(budget.getName(), budget);
        }
    }

    @And("The following budgets are passed")
    public void theFollowingBudgetsArePassed(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);

        // Get dates for budget
        LocalDate lastYear = LocalDate.now().minusYears(1);
        LocalDate firstDay = lastYear.with(firstDayOfYear());
        LocalDate lastDay = lastYear.with(lastDayOfYear());

        Budget budget;
        for (Map<String, String> columns : rows) {

            // Create budget
            budget = new Budget();
            budget.setName(columns.get("name"));
            budget.setAmountPerMember(Float.parseFloat(columns.get("amountPerMember")));
            budget.setSponsor(context.getUsers().get(columns.get("sponsor")));
            budget.setRules(context.getContents().get(columns.get("rules")));
            budget.setStartDate(Date.valueOf(firstDay));
            budget.setEndDate(Date.valueOf(lastDay));
            budget.setOrganization(context.getOrganizations().get(columns.get("organization")));
            budget.setIsDistributed(Boolean.valueOf(columns.get("isDistributed")));
            budget = budgetRepository.save(budget);

            // Save in Test Map
            context.getBudgets().put(budget.getName(), budget);
        }
    }

    @When("{string} get usable budgets")
    public void getUsableBudgets(String userFirstname) {

        // Refresh Token
        authenticationHttpClient.setBearerAuth(context.getAuths().get(userFirstname).getToken());
        AuthenticationResponse response = authenticationHttpClient.refresh();
        context.getAuths().put(userFirstname, response);

        // Get budgets
        budgetHttpClient.setBearerAuth(response.getToken());
        budgetHttpClient.getUsableBudgets();
    }

    @Then("It returns following budgets")
    public void itReturnsFollowingBudgets(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);

        Set<Budget> budgetsReturned = budgetHttpClient.getLastBody();

        Budget budget;
        for (Map<String, String> columns : rows) {

            // Create budget from feature
            budget = new Budget();
            budget.setName(columns.get("name"));
            budget.setAmountPerMember(Float.parseFloat(columns.get("amountPerMember")));
            budget.setSponsor(context.getUsers().get(columns.get("sponsor")));
            budget.setRules(context.getContents().get(columns.get("rules")));
            budget.setOrganization(context.getOrganizations().get(columns.get("organization")));
            budget.setIsDistributed(Boolean.valueOf(columns.get("isDistributed")));
            final Budget budgetFinal = budget;

            budgetsReturned.stream()
                    .filter(budgetReturned -> budgetFinal.getName().equals(budgetReturned.getName()))
                    .filter(budgetReturned -> budgetFinal.getAmountPerMember() == budgetReturned.getAmountPerMember())
                    .filter(budgetReturned -> budgetFinal.getIsDistributed().equals(budgetReturned.getIsDistributed()))
                    .filter(budgetReturned -> budgetFinal.getSponsor().getId().equals(budgetReturned.getSponsor().getId()))
                    .filter(budgetReturned -> budgetFinal.getRules().getId().equals(budgetReturned.getRules().getId()))
                    .filter(budgetReturned -> budgetFinal.getOrganization().getId().equals(budgetReturned.getOrganization().getId()))
                    .findAny()
                    .ifPresentOrElse(
                            budgetsReturned::remove,
                            Assert::fail);
        }

        Assert.assertEquals(0, budgetsReturned.size());
    }
}
