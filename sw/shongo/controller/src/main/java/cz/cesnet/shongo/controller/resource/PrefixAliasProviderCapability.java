package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;

/**
 * Represents a special type of {@link AliasProviderCapability} which
 * can allocate aliases from a single prefix.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PrefixAliasProviderCapability extends AliasProviderCapability
{
    /**
     * Prefix of aliases.
     */
    private String prefix;

    /**
     * @return {@link #prefix}
     */
    @Column
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * @param prefix sets the {@link #prefix}
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Capability createApi()
    {
        throw new RuntimeException("TODO: Implement AliasProviderCapability.createApi");
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        super.toApi(api);
        throw new RuntimeException("TODO: Implement AliasProviderCapability.toApi");
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        super.fromApi(api, entityManager);
        throw new RuntimeException("TODO: Implement AliasProviderCapability.fromApi");
    }
}
