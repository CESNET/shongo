package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.*;
import java.util.*;

/**
 * Capability tells that the resource acts as alias provider which can allocate aliases for itself and/or
 * other resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasProviderCapability extends Capability
{
    /**
     * Type of aliases.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * Cache for provided {@link Technology}s.
     */
    private Set<Technology> cachedProvidedTechnologies;

    /**
     * Cache for provided {@link AliasType}s.
     */
    private Set<AliasType> cachedProvidedAliasTypes;

    /**
     * List of pattern for aliases.
     * <p/>
     * Examples:
     * 1) "95[ddd]"     will generate 95001, 95002, 95003, ...
     * 2) "95[dd]2[dd]" will generate 9500201, 9500202, ..., 9501200, 9501201, ...
     */
    private List<String> patterns = new ArrayList<String>();

    /**
     * Specifies whether alias provider is restricted only to the owner resource or all resources can use the provider
     * for alias allocation.
     */
    private boolean restrictedToOwnerResource;

    /**
     * Constructor.
     */
    public AliasProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param type    to be added as {@link Alias} to {@link #aliases}
     * @param pattern to be added to the {@link #patterns}
     */
    public AliasProviderCapability(AliasType type, String pattern)
    {
        addAlias(new Alias(type, "{value}"));
        addPattern(pattern);
    }

    /**
     * Constructor.
     *
     * @param type                      to be added as {@link Alias} to {@link #aliases}
     * @param pattern                   to be added to the {@link #patterns}
     * @param restrictedToOwnerResource sets the {@link #restrictedToOwnerResource}
     */
    public AliasProviderCapability(AliasType type, String pattern,
            boolean restrictedToOwnerResource)
    {
        addAlias(new Alias(type, "{value}"));
        this.addPattern(pattern);
        this.restrictedToOwnerResource = restrictedToOwnerResource;
    }

    /**
     * @return {@link #aliases}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<Alias> getAliases()
    {
        return Collections.unmodifiableList(aliases);
    }

    /**
     * @param id
     * @return alias with given {@code id}
     * @throws EntityNotFoundException when alias doesn't exist
     */
    public Alias getAliasById(Long id) throws EntityNotFoundException
    {
        for (Alias alias : aliases) {
            if (alias.getId().equals(id)) {
                return alias;
            }
        }
        throw new EntityNotFoundException(Alias.class, id);
    }

    /**
     * @param alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        this.aliases.add(alias);

        // Reset caches
        this.cachedProvidedAliasTypes = null;
        this.cachedProvidedTechnologies = null;
    }

    /**
     * @param alias to be removed from the {@link #aliases}
     */
    public void removeAlias(Alias alias)
    {
        this.aliases.remove(alias);

        // Reset caches
        this.cachedProvidedAliasTypes = null;
        this.cachedProvidedTechnologies = null;
    }

    /**
     * @return {@link #patterns}
     */
    @ElementCollection
    @Access(AccessType.FIELD)
    public List<String> getPatterns()
    {
        return Collections.unmodifiableList(patterns);
    }

    /**
     * @param pattern to be added to the {@link #patterns}
     */
    public void addPattern(String pattern)
    {
        this.patterns.add(pattern);
    }

    /**
     * @param pattern to be removed from the {@link #patterns}
     */
    public void removePattern(String pattern)
    {
        this.patterns.remove(pattern);
    }

    /**
     * @return {@link #restrictedToOwnerResource}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isRestrictedToOwnerResource()
    {
        return restrictedToOwnerResource;
    }

    /**
     * @param restrictedToOwnerResource sets the {@link #restrictedToOwnerResource}
     */
    public void setRestrictedToOwnerResource(boolean restrictedToOwnerResource)
    {
        this.restrictedToOwnerResource = restrictedToOwnerResource;
    }

    /**
     * @param technologies to be checked
     * @return true if the {@link AliasProviderCapability} is able to provide an {@link Alias}
     *         for given {@code technology}, false otherwise
     */
    public boolean providesAliasTechnologies(Set<Technology> technologies)
    {
        if (cachedProvidedTechnologies == null) {
            cachedProvidedTechnologies = new HashSet<Technology>();
            for (Alias alias : aliases) {
                cachedProvidedTechnologies.add(alias.getTechnology());
            }
        }
        return cachedProvidedTechnologies.containsAll(technologies);
    }

    /**
     *
     * @param aliasType to be checked
     * @return true if the {@link AliasProviderCapability} is able to provide an {@link Alias}
     *         of given {@code aliasType}, false otherwise
     */
    public boolean providesAliasType(AliasType aliasType)
    {
        if (cachedProvidedAliasTypes == null) {
            cachedProvidedAliasTypes = new HashSet<AliasType>();
            for (Alias alias : aliases) {
                cachedProvidedAliasTypes.add(alias.getType());
            }
        }
        return cachedProvidedAliasTypes.contains(aliasType);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasProviderCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.AliasProviderCapability apiAliasProvider =
                (cz.cesnet.shongo.controller.api.AliasProviderCapability) api;
        for (Alias alias : aliases) {
            apiAliasProvider.addAlias(alias.toApi());
        }
        for (String pattern : patterns) {
            apiAliasProvider.addPattern(pattern);
        }
        apiAliasProvider.setRestrictedToOwnerResource(isRestrictedToOwnerResource());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.AliasProviderCapability apiAliasProvider =
                (cz.cesnet.shongo.controller.api.AliasProviderCapability) api;
        if (apiAliasProvider.isPropertyFilled(apiAliasProvider.RESTRICTED_TO_OWNER_RESOURCE)) {
            setRestrictedToOwnerResource(apiAliasProvider.getRestrictedToOwnerResource());
        }

        // Create/modify aliases
        for (cz.cesnet.shongo.api.Alias apiAlias : apiAliasProvider.getAliases()) {
            if (api.isPropertyItemMarkedAsNew(apiAliasProvider.ALIASES, apiAlias)) {
                Alias alias = new Alias();
                alias.fromApi(apiAlias);
                addAlias(alias);
            }
            else {
                Alias alias = getAliasById(apiAlias.notNullIdAsLong());
                alias.fromApi(apiAlias);
            }
        }
        // Delete aliases
        Set<cz.cesnet.shongo.api.Alias> apiDeletedAliases =
                api.getPropertyItemsMarkedAsDeleted(apiAliasProvider.ALIASES);
        for (cz.cesnet.shongo.api.Alias apiAlias : apiDeletedAliases) {
            removeAlias(getAliasById(apiAlias.notNullIdAsLong()));
        }

        // Create patterns
        for (String pattern : apiAliasProvider.getPatterns()) {
            if (api.isPropertyItemMarkedAsNew(cz.cesnet.shongo.controller.api.AliasProviderCapability.PATTERNS,
                    pattern)) {
                addPattern(pattern);
            }
        }
        // Delete patterns
        Set<String> patternsToDelete =
                api.getPropertyItemsMarkedAsDeleted(cz.cesnet.shongo.controller.api.AliasProviderCapability.PATTERNS);
        for (String pattern : patternsToDelete) {
            removePattern(pattern);
        }

        super.fromApi(api, entityManager);
    }

    @Transient
    public AliasGenerator getAliasGenerator()
    {
        return new AliasPatternGenerator(getPatterns());
    }
}
