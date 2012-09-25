package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.reservation.VirtualRoomReservation;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.TerminalCapability;
import cz.cesnet.shongo.controller.scheduler.report.AbstractResourceReport;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a {@link DeviceResource} which acts as {@link VirtualRoom} in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceVirtualRoom extends VirtualRoom
{
    /**
     * {@link VirtualRoomReservation} for the {@link DeviceResource}.
     */
    private VirtualRoomReservation virtualRoomReservation;

    /**
     * Constructor.
     */
    public ResourceVirtualRoom()
    {
    }

    /**
     * Constructor.
     *
     * @param virtualRoomReservation sets the {@link #virtualRoomReservation}
     */
    public ResourceVirtualRoom(VirtualRoomReservation virtualRoomReservation)
    {
        this.virtualRoomReservation = virtualRoomReservation;
    }

    /**
     * @return {@link #virtualRoomReservation}
     */
    @OneToOne
    public VirtualRoomReservation getVirtualRoomReservation()
    {
        return virtualRoomReservation;
    }

    /**
     * @param virtualRoomReservation sets the {@link #virtualRoomReservation}
     */
    public void setVirtualRoomReservation(VirtualRoomReservation virtualRoomReservation)
    {
        this.virtualRoomReservation = virtualRoomReservation;
    }

    /**
     * @return {@link DeviceResource}
     */
    @Transient
    public DeviceResource getDeviceResource()
    {
        return virtualRoomReservation.getDeviceResource();
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

    @Override
    @Transient
    public String getReportDescription()
    {
        return String.format("virtual room in %s",
                AbstractResourceReport.formatResource(getDeviceResource()));
    }

    @Override
    @Transient
    public Integer getPortCount()
    {
        return virtualRoomReservation.getPortCount();
    }

    @Override
    @Transient
    public void start(CompartmentExecutor compartmentExecutor)
    {
        DeviceResource deviceResource = getDeviceResource();
        StringBuilder message = new StringBuilder();
        message.append(String.format("Starting %s for %d ports.", getReportDescription(), getPortCount()));
        if (deviceResource.hasIpAddress()) {
            message.append(String.format(" Device has address '%s'.", deviceResource.getAddress().getValue()));
        }
        compartmentExecutor.getLogger().debug(message.toString());
        List<Alias> aliases = getAliases();
        for (Alias alias : aliases) {
            StringBuilder aliasMessage = new StringBuilder();
            aliasMessage
                    .append(String.format("%s has allocated alias '%s'.", getReportDescription(), alias.getValue()));
            compartmentExecutor.getLogger().debug(aliasMessage.toString());
        }
    }

    @Override
    public void stop(CompartmentExecutor compartmentExecutor)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Stopping %s for %d ports.", getReportDescription(), getPortCount()));
        compartmentExecutor.getLogger().debug(message.toString());
    }
}
