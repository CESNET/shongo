package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ForeignExecutable;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.person.UserPerson;
import cz.cesnet.shongo.controller.booking.reservation.AbstractForeignReservation;
import cz.cesnet.shongo.controller.booking.reservation.ForeignRoomReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.executor.ExecutionReport;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.report.Report;

import javax.persistence.EntityManager;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a TODO  which acts as {@link RoomEndpoint}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class ForeignRoomEndpoint extends RoomEndpoint implements ForeignExecutable
{
    private String foreignReservationRequestId;

    @Override
    public String getForeignReservationRequestId()
    {
        return foreignReservationRequestId;
    }

    public void setForeignReservationRequestId(String foreignReservationRequestId)
    {
        this.foreignReservationRequestId = foreignReservationRequestId;
    }

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
        return getForeignReservationRequestId();
    }

    @Override
    public void modifyRoom(Room roomApi, Executor executor) throws ExecutionReportSet.RoomNotStartedException, ExecutionReportSet.CommandFailedException
    {
        throw new TodoImplementException("pravdepodobne pro nahravani");
    }

    /**
     * @return {@link Alias} for given {@code type}
     */
    @Transient
    private Alias getAlias(AliasType aliasType)
    {
        for (Alias alias : this.getAliases()) {
            if (alias.getType() == aliasType) {
                return alias;
            }
        }
        return null;
    }

    @Transient
    public String getRoomName()
    {
        return getAlias(AliasType.ROOM_NAME).getValue();
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        try {
            return getForeignState(executableManager);
        }
        catch (ForeignDomainConnectException e) {
            String roomName = getRoomName();
            ExecutionReport executionReport = new ExecutionReportSet.RoomNotStartedReport(roomName);
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
        for (AbstractParticipant participant : this.getParticipants()) {
            UserInformation userInformation = Authorization.getInstance().getUserInformation(((UserPerson) ((PersonParticipant) participant).getPerson()).getUserId());
            System.out.println("kontrola participantu: " + userInformation.getPrincipalNames().toArray(new String[]{}));
        }

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
