package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;

/**
 * InterDomain agent for Domain Controller
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainAgent implements InterDomainProtocol
{
    private static InterDomainAgent instance;

    private ResourceService resourceService;

    private ReservationService reservationService;

    protected InterDomainAgent(ControllerConfiguration configuration) {
        setDomains(configuration);
    }

    public static synchronized InterDomainAgent create(ControllerConfiguration configuration)
    {
        if (instance != null) {
            throw new IllegalStateException("Another instance of InterDomain Agent already exists.");
        }
        InterDomainAgent interDomainAgent = new InterDomainAgent(configuration);
        instance = interDomainAgent;
        return instance;
    }

    public static InterDomainAgent getInstance()
    {
        if (instance == null) {
            throw new IllegalStateException("Cannot get instance of a domain controller, "
                    + "because no Inter Domain Agent has been created yet.");
        }
        return instance;
    }

    private void setDomains(ControllerConfiguration configuration)
    {

    }

    public void setResourceService(ResourceService resourceService)
    {
        this.resourceService = resourceService;
    }

    public void setReservationService(ReservationService reservationService)
    {
        this.reservationService = reservationService;
    }

    public static ReservationService getReservationService()
    {
        return getInstance().getReservationService();
    }

    public static ResourceService getResourceService()
    {
        return getInstance().getResourceService();
    }
}
