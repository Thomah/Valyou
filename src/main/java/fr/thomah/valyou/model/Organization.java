package fr.thomah.valyou.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@Entity
@Table(name = "organizations")
public class Organization extends AuditEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    @NotNull
    private String name;

    @Column(name = "slack_team_id", unique=true)
    private String slackTeamId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "organizations_users",
            joinColumns = {@JoinColumn(name = "organization_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")})
    @JsonIgnoreProperties(value = {"username", "password", "lastPasswordResetDate", "userAuthorities", "userOrganizationAuthorities", "authorities", "organizations", "budgets", "projects", "donations"}, allowSetters = true)
    private Set<User> members = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "project_organizations",
            joinColumns = {@JoinColumn(name = "organization_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")})
    @JsonIgnoreProperties(value = {"leader", "budgets", "donations", "peopleGivingTime", "organizations"}, allowSetters = true)
    private Set<Project> projects = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "organization",
            orphanRemoval = true)
    @JsonIgnoreProperties(value = {"organization", "sponsor"}, allowSetters = true)
    private Set<Budget> budgets = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "organizations_contents",
            joinColumns = {@JoinColumn(name = "organization_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "content_id", referencedColumnName = "id")})
    @JsonIgnoreProperties(value = {"organization"}, allowSetters = true)
    private Set<Content> contents = new LinkedHashSet<>();

    @OneToMany
    @JsonIgnoreProperties(value = {"organization", "users"}, allowSetters = true)
    private Set<OrganizationAuthority> organizationAuthorities = new LinkedHashSet<>();

    public void addProject(Project project) {
        this.projects.add(project);
    }

}
