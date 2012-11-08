package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
     * Technology of aliases.
     */
    private Technology technology;

    /**
     * Type of aliases.
     */
    private AliasType type;

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
     * @param technology sets the {@link #technology}
     * @param type       sets the {@link #type}
     * @param pattern    to be added to the {@link #patterns}
     */
    public AliasProviderCapability(Technology technology, AliasType type, String pattern)
    {
        this.technology = technology;
        this.type = type;
        this.addPattern(pattern);
    }

    /**
     * Constructor.
     *
     * @param technology                sets the {@link #technology}
     * @param type                      sets the {@link #type}
     * @param pattern                   to be added to the {@link #patterns}
     * @param restrictedToOwnerResource sets the {@link #restrictedToOwnerResource}
     */
    public AliasProviderCapability(Technology technology, AliasType type, String pattern,
            boolean restrictedToOwnerResource)
    {
        this.technology = technology;
        this.type = type;
        this.restrictedToOwnerResource = restrictedToOwnerResource;
        this.addPattern(pattern);
    }

    /**
     * @return {@link #technology}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @param technology sets the {@link #technology}
     */
    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public AliasType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(AliasType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #patterns}
     */
    @ElementCollection(fetch = FetchType.EAGER)
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
        apiAliasProvider.setTechnology(getTechnology());
        apiAliasProvider.setType(getType());
        for ( String pattern : patterns) {
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
        if (apiAliasProvider.isPropertyFilled(apiAliasProvider.TECHNOLOGY)) {
            setTechnology(apiAliasProvider.getTechnology());
        }
        if (apiAliasProvider.isPropertyFilled(apiAliasProvider.TYPE)) {
            setType(apiAliasProvider.getType());
        }
        if (apiAliasProvider.isPropertyFilled(apiAliasProvider.RESTRICTED_TO_OWNER_RESOURCE)) {
            setRestrictedToOwnerResource(apiAliasProvider.getRestrictedToOwnerResource());
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
        return new AliasPatternGenerator(technology, type, getPatterns());
    }
}
