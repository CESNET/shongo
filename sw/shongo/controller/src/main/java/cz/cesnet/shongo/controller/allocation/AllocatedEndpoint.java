package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;

import java.util.List;
import java.util.Set;

/**
 * Represents one or multiple endpoint(s) in a scheduler plan.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AllocatedEndpoint
{
    /**
     * @return number of the endpoints
     */
    public int getCount();

    /**
     * @return set of technologies which are supported by the endpoint(s)
     */
    public abstract Set<Technology> getSupportedTechnologies();

    /**
     * @return true if device can participate in 2-point video conference without virtual room,
     *         false otherwise
     */
    public boolean isStandalone();

    /**
     * @param alias to be assign to the endpoint
     */
    public void assignAlias(Alias alias);

    /**
     * @return list of aliases for the endpoint(s)
     */
    public List<Alias> getAssignedAliases();

    /**
     * @return IP address or URL of the endpoint
     */
    Address getAddress();
}
