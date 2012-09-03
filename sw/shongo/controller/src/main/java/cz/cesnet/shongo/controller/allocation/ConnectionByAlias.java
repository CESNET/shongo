package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.controller.resource.Alias;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Represents a {@link Connection} by alias.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ConnectionByAlias extends Connection
{
    /**
     * {@link Alias} for the {@link Connection#allocatedEndpointTo}.
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
