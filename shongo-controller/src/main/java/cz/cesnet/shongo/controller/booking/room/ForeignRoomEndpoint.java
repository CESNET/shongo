package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.reservation.AbstractForeignReservation;
import cz.cesnet.shongo.controller.booking.reservation.ForeignRoomReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.TerminalCapability;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.executor.ExecutionReport;
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

    @Override
    public void modifyRoom(Room roomApi, Executor executor) throws ExecutionReportSet.RoomNotStartedException, ExecutionReportSet.CommandFailedException
    {
        throw new TodoImplementException("pravdepodobne pro nahravani");
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        try {
            return getForeignState(executableManager);
        }
        catch (ForeignDomainConnectException e) {
            ExecutionReport executionReport = new ExecutionReportSet.RoomNotStartedReport(getMeetingName());
            executableManager.createExecutionReport(this, executionReport);
            return State.STARTING_FAILED;
        }
    }

    @Override
    protected State onStop(Executor executor, ExecutableManager executableManager)
    {
        try {
            return getForeignState(executableManager);
        }
        catch (ForeignDomainConnectException e) {
            // TODO: parse exception
            return State.STOPPING_FAILED;
        }
    }

    @Override
    protected void onUpdate()
    {
//        throw new TodoImplementException("pravdepodobne jen pridavat participanty");
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
        //TODO: roomExecutableEndpointApi.setResourceId(ObjectIdentifier.formatId(getResource()));

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

    @Transient
    private State getForeignState(ExecutableManager executableManager) throws ForeignDomainConnectException
    {
        Reservation reservation = executableManager.getReservation(this);
        if (reservation == null) {
            return State.STOPPED;
        }
        else if (!(reservation instanceof ForeignRoomReservation)) {
            throw new IllegalStateException("Cannot get state for reservation other than foreign.");
        }
        AbstractForeignReservation foreignReservation = (AbstractForeignReservation) reservation;
        Domain domain = foreignReservation.getDomain().toApi();
        String foreignReservationRequestId = foreignReservation.getForeignReservationRequestId();
        cz.cesnet.shongo.controller.api.domains.response.Reservation response;
        response = InterDomainAgent.getInstance().getConnector().getReservationByRequest(domain, foreignReservationRequestId);

        cz.cesnet.shongo.controller.api.domains.response.RoomSpecification specification;
        specification = (cz.cesnet.shongo.controller.api.domains.response.RoomSpecification) response.getSpecification();

        return State.valueOf(specification.getState().toApi().name());
    }
}
