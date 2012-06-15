package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasResource;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Represents a special type of {@link AllocatedResource} an allocated {@link AliasResource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AllocatedAlias extends AllocatedResource
{
    /**
     * Alias that is allocated.
     */
    private Alias alias;

    /**
     * @return {@link #alias}
     */
    @OneToOne
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
}
