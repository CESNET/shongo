package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.controller.resource.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceResolver
{
    Map<Technology, Integer> technologyPorts = new HashMap<Technology, Integer>();

    public void addTechnologyPorts(Technology technology, int portCount)
    {
        Integer currentPortCount = technologyPorts.get(technology);
        if ( currentPortCount == null ) {
            currentPortCount = 0;
        }
        currentPortCount += portCount;
        technologyPorts.put(technology, currentPortCount);
    }

    public List<Resource> resolve() throws FaultException
    {
        // TODO: Allocate endpoints

        // TODO: Allocate aliases for endpoint (if needed)

        if ( technologyPorts.size() == 0 ) {
            throw new FaultException("No resources are requested for allocation.");
        }
        else if ( technologyPorts.size() > 1 ) {
            throw new FaultException("Only resources of a single technology is allowed for now.");
        }

        Technology technology = technologyPorts.keySet().iterator().next();

        // TODO: Resolve virtual rooms and gateways for connecting endpoints

        List<Resource> resources = new ArrayList<Resource>();

        // TODO: add resources

        return resources;
    }
}
