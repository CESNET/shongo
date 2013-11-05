package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.SimplePersistentObject;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Represents a service for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class EndpointService extends SimplePersistentObject
{
    /**
     * Specifies whether the service is currently active.
     */
    private boolean enabled;

    /**
     * @return {@link #enabled}
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @param enabled sets the {@link #enabled}
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
