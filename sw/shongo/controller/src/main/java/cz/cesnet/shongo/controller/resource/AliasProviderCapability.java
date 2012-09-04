package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.*;

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
     * Prefix of aliases.
     */
    private String pattern;

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
     * @param pattern    sets the {@link #pattern}
     */
    public AliasProviderCapability(Technology technology, AliasType type, String pattern)
    {
        this.technology = technology;
        this.type = type;
        this.pattern = pattern;
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
     * @return {@link #pattern}
     */
    @Column
    public String getPattern()
    {
        return pattern;
    }

    /**
     * @param pattern sets the {@link #pattern}
     */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
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
        apiAliasProvider.setPattern(getPattern());
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
        if (apiAliasProvider.isPropertyFilled(apiAliasProvider.PATTERN)) {
            setPattern(apiAliasProvider.getPattern());
        }
        super.fromApi(api, entityManager);
    }
}
