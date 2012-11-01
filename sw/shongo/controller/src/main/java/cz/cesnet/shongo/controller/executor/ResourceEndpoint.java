package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.report.AbstractResourceReport;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
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
public class ResourceEndpoint extends Endpoint implements ManagedEndpoint
{
    /**
     * {@link EndpointReservation} for the {@link DeviceResource}.
     */
    private DeviceResource deviceResource;

    /**
     * Constructor.
     */
    public ResourceEndpoint()
    {
    }

    /**
     * @param deviceResource sets the {@link #deviceResource}
     */
    public ResourceEndpoint(DeviceResource deviceResource)
    {
        this.deviceResource = deviceResource;
    }

    /**
     * @return {@link #deviceResource}
     */
    @OneToOne
    public DeviceResource getDeviceResource()
    {
        return deviceResource;
    }

    /**
     * @param deviceResource sets the {@link #deviceResource}
     */
    public void setDeviceResource(DeviceResource deviceResource)
    {
        this.deviceResource = deviceResource;
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
        aliases.addAll(super.getAssignedAliases());
        return aliases;
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return getDeviceResource().getAddress();
    }

    @Override
    @Transient
    public String getReportDescription()
    {
        return AbstractResourceReport.formatResource(getDeviceResource());
    }

    @Override
    @Transient
    public String getConnectorAgentName()
    {
        Mode mode = getDeviceResource().getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        else {
            throw new IllegalStateException("Resource " + getReportDescription() + " is not managed!");
        }
    }

    @Override
    protected State onStart(ExecutorThread executorThread, EntityManager entityManager)
    {
        List<Alias> aliases = getAssignedAliases();
        for (Alias alias : aliases) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Assigning alias '%s' to %s .", alias.getValue(), getReportDescription()));
            executorThread.getLogger().debug(message.toString());
        }
        return super.onStart(executorThread, entityManager);
    }

    @Override
    protected State onStop(ExecutorThread executorThread, EntityManager entityManager)
    {
        List<Alias> aliases = getAssignedAliases();
        for (Alias alias : aliases) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Removing alias '%s' from %s .", alias.getValue(), getReportDescription()));
            executorThread.getLogger().debug(message.toString());
        }
        return super.onStop(executorThread, entityManager);
    }
}
