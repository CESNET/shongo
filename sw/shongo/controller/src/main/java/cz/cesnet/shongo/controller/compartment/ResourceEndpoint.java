package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.TerminalCapability;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a {@link DeviceResource} which acts as {@link Endpoint} in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceEndpoint extends Endpoint
{
    /**
     * {@link EndpointReservation} for the {@link DeviceResource}.
     */
    private EndpointReservation endpointReservation;

    /**
     * Constructor.
     */
    public ResourceEndpoint()
    {
    }

    /**
     * @param endpointReservation sets the {@link #endpointReservation}
     */
    public ResourceEndpoint(EndpointReservation endpointReservation)
    {
        this.endpointReservation = endpointReservation;
    }

    /**
     * @return {@link #endpointReservation}
     */
    @OneToOne
    public EndpointReservation getEndpointReservation()
    {
        return endpointReservation;
    }

    /**
     * @param endpointReservation sets the {@link #endpointReservation}
     */
    public void setEndpointReservation(EndpointReservation endpointReservation)
    {
        this.endpointReservation = endpointReservation;
    }

    /**
     * @return {@link DeviceResource}
     */
    @Transient
    public DeviceResource getDeviceResource()
    {
        return endpointReservation.getDeviceResource();
    }

    @Override
    @Transient
    public Set<Technology> getTechnologies()
    {
        return getDeviceResource().getTechnologies();
    }

    @Override
    @Transient
    public boolean isStandalone()
    {
        return getDeviceResource().isStandaloneTerminal();
    }

    @Override
    @Transient
    public List<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        TerminalCapability terminalCapability = getDeviceResource().getCapability(TerminalCapability.class);
        if (terminalCapability != null) {
            aliases.addAll(terminalCapability.getAliases());
        }
        aliases.addAll(super.getAliases());
        return aliases;
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return getDeviceResource().getAddress();
    }
}
