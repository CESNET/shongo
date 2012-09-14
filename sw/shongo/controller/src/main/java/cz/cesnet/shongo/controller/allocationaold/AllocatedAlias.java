package cz.cesnet.shongo.controller.allocationaold;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;

import javax.persistence.*;

/**
 * Represents a special type of {@link AllocatedResource} an allocated alias from a resource
 * with {@link AliasProviderCapability}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AllocatedAlias extends AllocatedItem
{
    /**
     * {@link AliasProviderCapability} from which the alias is allocated.
     */
    private AliasProviderCapability aliasProviderCapability;

    /**
     * Alias that is allocated.
     */
    private Alias alias;

    /**
     * @return {@link #aliasProviderCapability}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public AliasProviderCapability getAliasProviderCapability()
    {
        return aliasProviderCapability;
    }

    /**
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public void setAliasProviderCapability(AliasProviderCapability aliasProviderCapability)
    {
        this.aliasProviderCapability = aliasProviderCapability;
    }

    /**
     * @return {@link #alias}
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public Alias getAlias()
    {
        return alias;
    }

    /**
     * @param alias sets the {@link #alias}
     */
    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AllocatedItem createApi()
    {
        return new cz.cesnet.shongo.controller.api.AllocatedAlias();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AllocatedItem api, Domain domain)
    {
        cz.cesnet.shongo.controller.api.AllocatedAlias apiAllocatedAlias =
                (cz.cesnet.shongo.controller.api.AllocatedAlias) api;
        apiAllocatedAlias.setAlias(getAlias().toApi());
        super.toApi(api, domain);
    }
}
