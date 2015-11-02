package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.TerminalCapability;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.report.Report;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a TODO  which acts as {@link RoomEndpoint}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class ForeignRoomEndpoint extends RoomEndpoint
{
    @Transient
    @Override
    public DeviceResource getResource()
    {
        return null;
    }

    @Transient
    @Override
    public String getRoomId()
    {
        return null;
    }

    @Transient
    @Override
    public void modifyRoom(Room roomApi, Executor executor) throws ExecutionReportSet.RoomNotStartedException, ExecutionReportSet.CommandFailedException
    {
        throw new TodoImplementException();
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        return State.STARTED;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        return new RoomExecutable();
    }

    @Override
    public RoomExecutable toApi(EntityManager entityManager, Report.UserType userType)
    {
        return (RoomExecutable) super.toApi(entityManager, userType);
    }

    @Override
    public void toApi(Executable executableApi, EntityManager entityManager, Report.UserType userType)
    {
        super.toApi(executableApi, entityManager, userType);

        RoomExecutable roomExecutableEndpointApi =
                (RoomExecutable) executableApi;
        roomExecutableEndpointApi.setLicenseCount(getLicenseCount());
//        roomExecutableEndpointApi.setResourceId(ObjectIdentifier.formatId(getResource()));

        // For Adobe Connect recordings
//        RecordingCapability resourceRecordingCapability = getResource().getCapability(RecordingCapability.class);
//        if (resourceRecordingCapability != null) {
//            roomExecutableEndpointApi.setRecordingFolderId(getRecordingFolderId(resourceRecordingCapability));
//        }

        roomExecutableEndpointApi.setRoomId(getRoomId());
        for (Technology technology : getTechnologies()) {
            roomExecutableEndpointApi.addTechnology(technology);
        }
        for (Alias alias : getAssignedAliases()) {
            roomExecutableEndpointApi.addAlias(alias.toApi());
        }
        for (RoomSetting roomSetting : getRoomSettings()) {
            roomExecutableEndpointApi.addRoomSetting(roomSetting.toApi());
        }
    }

    /**
     * @return {@link RoomConfiguration#licenseCount}
     */
    @Transient
    public int getLicenseCount()
    {
        RoomConfiguration roomConfiguration = getRoomConfiguration();
        if (roomConfiguration == null) {
            throw new IllegalStateException("Room configuration hasn't been set yet.");
        }
        return roomConfiguration.getLicenseCount();
    }

    /**
     * @return {@link RoomConfiguration#roomSettings} or empty collection if {@link #roomConfiguration} is null
     */
    @Transient
    private Collection<RoomSetting> getRoomSettings()
    {
        RoomConfiguration roomConfiguration = getRoomConfiguration();
        if (roomConfiguration == null) {
            throw new IllegalStateException("Room configuration hasn't been set yet.");
        }
        return roomConfiguration.getRoomSettings();
    }

//    @Override
//    @Transient
//    public boolean isStandalone()
//    {
//        return getResource().isStandaloneTerminal();
//    }
//
//    @Override
//    @Transient
//    public List<Alias> getAliases()
//    {
//        List<Alias> aliases = new ArrayList<Alias>();
//        TerminalCapability terminalCapability = getResource().getCapability(TerminalCapability.class);
//        if (terminalCapability != null) {
//            aliases.addAll(terminalCapability.getAliases());
//        }
//        aliases.addAll(super.getAssignedAliases());
//        return aliases;
//    }
//
//    @Override
//    public void addAssignedAlias(Alias assignedAlias) throws SchedulerException
//    {
//        getResource().evaluateAlias(assignedAlias);
//        super.addAssignedAlias(assignedAlias);
//    }

}
