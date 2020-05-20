package fr.lesprojetscagnottes.core.controller;

import fr.lesprojetscagnottes.core.entity.*;
import fr.lesprojetscagnottes.core.model.*;
import fr.lesprojetscagnottes.core.exception.BadRequestException;
import fr.lesprojetscagnottes.core.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.exception.NotFoundException;
import fr.lesprojetscagnottes.core.pagination.DataPage;
import fr.lesprojetscagnottes.core.repository.*;
import fr.lesprojetscagnottes.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Budgets", description = "The Budgets API")
public class BudgetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetController.class);

    @Autowired
    private UserController userController;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationAuthorityRepository organizationAuthorityRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Operation(summary = "Get list of budgets by a list of IDs", description = "Find a list of budgets by a list of IDs", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the budgets", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"ids"})
    public Set<BudgetModel> getByIds(Principal principal, @RequestParam("ids") Set<Long> ids) {

        Long userLoggedInId = userService.get(principal).getId();
        boolean userLoggedIn_isNotAdmin = userService.isNotAdmin(userLoggedInId);
        Set<Organization> userLoggedInOrganizations = organizationRepository.findAllByMembers_Id(userLoggedInId);
        Set<BudgetModel> models = new LinkedHashSet<>();

        for(Long id : ids) {

            // Retrieve full referenced objects
            Budget budget = budgetRepository.findById(id).orElse(null);
            if(budget == null) {
                LOGGER.error("Impossible to get budget {} : it doesn't exist", id);
                continue;
            }

            // Verify that principal share an organization with the user
            if(!userLoggedInOrganizations.contains(budget.getOrganization()) && userLoggedIn_isNotAdmin) {
                LOGGER.error("Impossible to get budget {} : principal {} is not in its organization", id, userLoggedInId);
                continue;
            }

            // Add the user to returned list
            models.add(BudgetModel.fromEntity(budget));
        }

        return models;
    }

    @Operation(summary = "Find all usable budgets for the current user", description = "A usable budget has an unreached end date and is distributed", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return all usable budgets for the current user", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetModel.class))))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget/usable", method = RequestMethod.GET)
    public Set<BudgetModel> getUsableBudgets(Principal principal) {

        // Get user organizations
        User user = userService.get(principal);
        Set<Organization> organizations = organizationRepository.findAllByMembers_Id(user.getId());

        // Put all organization IDs in a single list
        Set<Long> organizationIds = new LinkedHashSet<>();
        organizations.forEach(organization -> organizationIds.add(organization.getId()));

        // Retrieve all corresponding entities
        Set<Budget> entities = budgetRepository.findAllUsableBudgetsInOrganizations(new Date(), organizationIds);

        // Convert all entities to models
        Set<BudgetModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(BudgetModel.fromEntity(entity)));

        return models;
    }

    @Operation(summary = "Get accounts using a budget", description = "Get accounts using a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns corresponding accounts", content = @Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "Budget ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget/{id}/accounts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit"})
    public DataPage<AccountModel> getAccounts(Principal principal, @PathVariable("id") Long id, @RequestParam("offset") int offset, @RequestParam("limit") int limit) {

        // Fails if budget ID is missing
        if(id <= 0) {
            LOGGER.error("Impossible to get accounts by budget ID : budget ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that principal is in one organization of the campaign
        Long userLoggedInId = userService.get(principal).getId();
        if(budgetRepository.findAllByUserAndId(userLoggedInId, id).isEmpty() && userService.isNotAdmin(userLoggedInId)) {
            LOGGER.error("Impossible to get accounts by budget ID : user {} has not enough privileges", userLoggedInId);
            throw new ForbiddenException();
        }

        // Retrieve full referenced objects
        Budget budget = budgetRepository.findById(id).orElse(null);

        // Verify that any of references are not null
        if(budget == null) {
            LOGGER.error("Impossible to get accounts by budget ID : budget {} not found", id);
            throw new NotFoundException();
        }

        // Get and transform donations
        Page<Account> entities = accountRepository.findByBudgetId(id, PageRequest.of(offset, limit, Sort.by("id").ascending()));
        DataPage<AccountModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(AccountModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get campaigns using a budget", description = "Get campaigns using a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns corresponding campaigns", content = @Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "Budget ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget/{id}/campaigns", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit"})
    public DataPage<CampaignModel> getCampaigns(Principal principal, @PathVariable("id") Long id, @RequestParam("offset") int offset, @RequestParam("limit") int limit) {

        // Fails if budget ID is missing
        if(id <= 0) {
            LOGGER.error("Impossible to get campaigns by budget ID : budget ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that principal is in one organization of the campaign
        Long userLoggedInId = userService.get(principal).getId();
        if(budgetRepository.findAllByUserAndId(userLoggedInId, id).isEmpty() && userService.isNotAdmin(userLoggedInId)) {
            LOGGER.error("Impossible to get campaigns by budget ID : user {} has not enough privileges", userLoggedInId);
            throw new ForbiddenException();
        }

        // Retrieve full referenced objects
        Budget budget = budgetRepository.findById(id).orElse(null);

        // Verify that any of references are not null
        if(budget == null) {
            LOGGER.error("Impossible to get campaigns by budget ID : budget {} not found", id);
            throw new NotFoundException();
        }

        // Get and transform donations
        Page<Campaign> entities = campaignRepository.findByBudgets_id(id, PageRequest.of(offset, limit, Sort.by("id").ascending()));
        DataPage<CampaignModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(CampaignModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get donations imputed on a budget", description = "Get donations imputed on a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns corresponding donations", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonationModel.class)))),
            @ApiResponse(responseCode = "400", description = "Budget ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget/{id}/donations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<DonationModel> getDonations(Principal principal, @PathVariable("id") long budgetId) {

        // Fails if budget ID is missing
        if(budgetId <= 0) {
            LOGGER.error("Impossible to get donations by budget ID : Budget ID is incorrect");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        Budget budget = budgetRepository.findById(budgetId).orElse(null);

        // Verify that any of references are not null
        if(budget == null) {
            LOGGER.error("Impossible to get donations by budget ID : budget not found");
            throw new NotFoundException();
        }

        // Verify that principal is member of organization
        Long userLoggedInId = userService.get(principal).getId();
        if(userService.isMemberOfOrganization(userLoggedInId, budget.getOrganization().getId()) && userService.isNotAdmin(userLoggedInId)) {
            LOGGER.error("Impossible to get donations by budget ID : principal {} is not member of organization {}", userLoggedInId, budget.getOrganization().getId());
            throw new ForbiddenException();
        }

        Set<Donation> entities = donationRepository.findAllByBudgetId(budgetId);
        Set<DonationModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(DonationModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get donations made by a user imputed on a budget", description = "Get donations made by a user imputed on a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns corresponding donations", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonationModel.class)))),
            @ApiResponse(responseCode = "400", description = "Campaign ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "User has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Campaign not found", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/api/budget/{id}/donations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"contributorId"})
    public Set<DonationModel> getDonationsByContributorId(Principal principal, @PathVariable("id") long budgetId, @RequestParam("contributorId") long contributorId) {
        return userController.getDonationsByBudgetId(principal, contributorId, budgetId);
    }

    @Operation(summary = "Create a budget", description = "Create a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Budget created", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Some references are missing", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Some references doesn't exist", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Principal or sponsor has not enough privileges", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void create(Principal principal, @RequestBody BudgetModel budget) {

        // Fails if any of references are null
        if(budget == null || budget.getOrganization() == null || budget.getSponsor() == null || budget.getRules() == null
                || budget.getOrganization().getId() == null || budget.getSponsor().getId() == null || budget.getRules().getId() == null) {
            if(budget != null ) {
                LOGGER.error("Impossible to create budget {} : some references are missing", budget.getName());
            } else {
                LOGGER.error("Impossible to create a null budget");
            }
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        Organization organization = organizationRepository.findById(budget.getOrganization().getId()).orElse(null);
        User sponsor = userRepository.findById(budget.getSponsor().getId()).orElse(null);
        fr.lesprojetscagnottes.core.entity.Content rules = contentRepository.findById(budget.getRules().getId()).orElse(null);

        // Fails if any of references are null
        if(organization == null || sponsor == null || rules == null) {
            LOGGER.error("Impossible to create budget {} : one or more reference(s) doesn't exist", budget.getName());
            throw new NotFoundException();
        }

        // Test that user logged in has correct rights
        User userLoggedIn = userService.get(principal);
        if(!userService.isSponsorOfOrganization(userLoggedIn.getId(), organization.getId()) && userService.isNotAdmin(userLoggedIn.getId())) {
            LOGGER.error("Impossible to create budget {} : principal {} has not enough privileges", budget.getName(), userLoggedIn.getId());
            throw new ForbiddenException();
        }

        // Test that sponsor has correct rights
        if(!userService.isSponsorOfOrganization(budget.getSponsor().getId(), organization.getId())) {
            LOGGER.error("Impossible to create budget {} : sponsor {} has not enough privileges", budget.getName(), sponsor.getId());
            throw new ForbiddenException();
        }

        // Save budget
        Budget budgetToSave = new Budget();
        budgetToSave.setName(budget.getName());
        budgetToSave.setAmountPerMember(budget.getAmountPerMember());
        budgetToSave.setStartDate(budget.getStartDate());
        budgetToSave.setEndDate(budget.getEndDate());
        budgetToSave.setIsDistributed(budget.getIsDistributed());
        budgetToSave.setOrganization(organization);
        budgetToSave.setSponsor(sponsor);
        budgetToSave.setRules(rules);
        budgetRepository.save(budgetToSave);
    }

    @Operation(summary = "Save multiple budgets", description = "Save a collection of budgets", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budgets saved", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Body is missing", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public void save(Principal principal, @RequestBody List<BudgetModel> budgets) {

        // Fails if body is null
        if(budgets == null) {
            LOGGER.error("Impossible to update null budgets");
            throw new BadRequestException();
        }

        User userLoggedIn = userService.get(principal);
        boolean userLoggedIn_isAdmin = authorityRepository.findByNameAndUsers_Id(AuthorityName.ROLE_ADMIN, userLoggedIn.getId()) == null;

        for(BudgetModel budget : budgets) {

            // Fails if any of references are null
            if(budget.getOrganization() == null || budget.getSponsor() == null || budget.getRules() == null
                    || budget.getOrganization().getId() == null || budget.getSponsor().getId() == null || budget.getRules().getId() == null) {
                LOGGER.error("Impossible to update budget {} : some references are missing", budget.getId());
                continue;
            }

            // Retrieve full referenced objects
            Budget budgetInDb = budgetRepository.findById(budget.getId()).orElse(null);
            Organization organization = organizationRepository.findById(budget.getOrganization().getId()).orElse(null);
            User sponsor = userRepository.findById(budget.getSponsor().getId()).orElse(null);
            fr.lesprojetscagnottes.core.entity.Content rules = contentRepository.findById(budget.getRules().getId()).orElse(null);
            if(budgetInDb == null || organization == null || sponsor == null || rules == null) {
                LOGGER.error("Impossible to update budget {} : one or more reference(s) doesn't exist", budget.getId());
                continue;
            }

            // Test that user logged in has correct rights
            if(!userService.isSponsorOfOrganization(userLoggedIn.getId(), organization.getId()) && userService.isNotAdmin(userLoggedIn.getId())) {
                LOGGER.error("Impossible to update budget {} : principal {} has not enough privileges", budget.getName(), userLoggedIn.getId());
                continue;
            }

            // Test that sponsor has correct rights
            if(!userService.isSponsorOfOrganization(budget.getSponsor().getId(), organization.getId())) {
                LOGGER.error("Impossible to update budget {} : sponsor {} has not enough privileges", budget.getName(), sponsor.getId());
                continue;
            }

            // Update budget in DB
            budgetInDb.setName(budget.getName());
            if(!budget.getIsDistributed()) {
                budgetInDb.setStartDate(budget.getStartDate());
                budgetInDb.setEndDate(budget.getEndDate());
                budgetInDb.setAmountPerMember(budget.getAmountPerMember());
                budgetInDb.setOrganization(organization);
                budgetInDb.setSponsor(sponsor);
                budgetInDb.setRules(rules);
            }
            budgetRepository.save(budgetInDb);
        }
    }

    @Operation(summary = "Distribute a budget", description = "Distribute a budget between members of organization", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget distributed", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "ID is missing", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @RequestMapping(value = "/budget/{id}/distribute", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('USER')")
    public void distribute(Principal principal, @PathVariable("id") Long id) {

        // Fails if any of references are null
        if(id <= 0) {
            LOGGER.error("Impossible to distribute budget : ID is missing");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        Budget budget = budgetRepository.findById(id).orElse(null);
        if(budget == null) {
            LOGGER.error("Impossible to distribute budget {} : budget not found", id);
            throw new NotFoundException();
        }

        // Verify that principal is member of organization
        Long userLoggedInId = userService.get(principal).getId();
        Long organizationId = budget.getOrganization().getId();
        if(userService.isSponsorOfOrganization(userLoggedInId, organizationId) && userService.isNotAdmin(userLoggedInId)) {
            LOGGER.error("Impossible to distribute budget {} : principal {} is not sponsor of organization {}", budget.getName(), userLoggedInId, budget.getOrganization().getId());
            throw new ForbiddenException();
        }

        // Create personal accounts for all members
        Set<User> members = userRepository.findAllByOrganizations_id(organizationId);
        members.forEach(member -> {
            Account account = accountRepository.findByOwnerIdAndBudgetId(member.getId(), budget.getId());
            if(account == null) {
                account = new Account();
                account.setAmount(budget.getAmountPerMember());
                account.setBudget(budget);
            }
            account.setInitialAmount(budget.getAmountPerMember());
            account.setOwner(member);
            accountRepository.save(account);
        });

        // Distribute budget
        budget.setIsDistributed(true);
        budgetRepository.save(budget);
    }

    @Operation(summary = "Delete a budget", description = "Delete a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget distributed", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is missing", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @RequestMapping(value = "/budget/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('USER')")
    public void delete(Principal principal, @PathVariable("id") Long id) {

        // Fails if any of references are null
        if(id <= 0) {
            LOGGER.error("Impossible to delete budget : ID is missing");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        Budget budget = budgetRepository.findById(id).orElse(null);
        if(budget == null) {
            LOGGER.error("Impossible to delete budget {} : budget not found", id);
            throw new NotFoundException();
        }

        // Verify that principal is member of organization
        User userLoggedIn = userService.get(principal);
        Organization principalOrganization = organizationRepository.findByIdAndMembers_Id(budget.getOrganization().getId(), userLoggedIn.getId());
        if(principalOrganization == null) {
            LOGGER.error("Impossible to delete budget {} : principal {} is not member of organization {}", budget.getName(), userLoggedIn.getId(), budget.getOrganization().getId());
            throw new ForbiddenException();
        }

        // Test that user logged in has correct rights
        if(organizationAuthorityRepository.findByOrganizationIdAndUsersIdAndName(budget.getOrganization().getId(), userLoggedIn.getId(), OrganizationAuthorityName.ROLE_SPONSOR) == null &&
                authorityRepository.findByNameAndUsers_Id(AuthorityName.ROLE_ADMIN, userLoggedIn.getId()) == null) {
            LOGGER.error("Impossible to delete budget {} : principal {} has not enough privileges", budget.getName(), userLoggedIn.getId());
            throw new ForbiddenException();
        }

        // Delete budget
        budgetRepository.deleteById(id);
    }

}
