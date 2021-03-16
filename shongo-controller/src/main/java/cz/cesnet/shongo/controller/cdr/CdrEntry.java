package cz.cesnet.shongo.controller.cdr;

import com.google.common.base.Strings;
import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.Controller;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.person.AbstractPerson;
import cz.cesnet.shongo.controller.booking.person.UserPerson;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import javax.persistence.*;

/**
 * Represents an CDR (Call detail record) entry for RoomEndpoint requested by {@link #requestedBy}.
 *
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
@Entity
public class CdrEntry extends SimplePersistentObject {

    /**
     * User-id of an user who requested the call.
     */
    private String requestedBy;

    /**
     * Organisation of an user who requested the call.
     */
    private String usersOrganization;

    /**
     * Start of the requested call slot.
     */
    private DateTime slotStart;

    /**
     * End of the requested call slot.
     */
    private DateTime slotEnd;

    /**
     * Licences reserved for this call.
     */
    private int licenseCount;

    /**
     * Description of the meeting.
     */
    private String meetingDescription;

    /**
     * Name of the used resource.
     */
    private String resourceName;


    public CdrEntry() {

    }

    public void fromRoomEndpoint(RoomEndpoint roomEndpoint, AuthorizationManager authorizationManager) {
        setSlotEnd(roomEndpoint.getSlotEnd());
        setSlotStart(roomEndpoint.getSlotStart());
        setRequestedBy(roomEndpoint.getRequestedBy());
        setLicenseCount(roomEndpoint.getRoomConfiguration().getLicenseCount());
        setMeetingDescription(roomEndpoint.getMeetingDescription());
        setResourceName(roomEndpoint.getResource().getName());
        if (roomEndpoint.getRequestedBy() != null) {
            setRequestedBy(roomEndpoint.getRequestedBy());
        } else {
            if (!roomEndpoint.getParticipants().isEmpty()) {
                AbstractParticipant participant = roomEndpoint.getParticipants().get(0);
                if (participant instanceof PersonParticipant) {
                    PersonParticipant personParticipant = (PersonParticipant) participant;
                    AbstractPerson person = personParticipant.getPerson();
                    if (person instanceof UserPerson) {
                        UserPerson userPerson = (UserPerson) person;
                        setRequestedBy(userPerson.getUserId());
                    }
                }
            }
        }
        if (!Strings.isNullOrEmpty(getRequestedBy()) && !requestedBy.contains("einfra.cesnet.cz")) {
            setRequestedBy(null);
        }
        if (!Strings.isNullOrEmpty(getRequestedBy())) {
            UserInformation userInformation = authorizationManager.getUserInformation(getRequestedBy());
            setUsersOrganization(userInformation.getOrganization());
        }
    }

    @Column(length = Controller.USER_ID_COLUMN_LENGTH)
    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getUsersOrganization() {
        return usersOrganization;
    }

    public void setUsersOrganization(String usersOrganization) {
        this.usersOrganization = usersOrganization;
    }

    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = PersistentDateTime.NAME)
    public DateTime getSlotStart() {
        return slotStart;
    }

    public void setSlotStart(DateTime slotStart) {
        this.slotStart = slotStart;
    }

    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = PersistentDateTime.NAME)
    public DateTime getSlotEnd() {
        return slotEnd;
    }

    public void setSlotEnd(DateTime slotEnd) {
        this.slotEnd = slotEnd;
    }

    public int getLicenseCount()
    {
        return licenseCount;
    }

    public void setLicenseCount(int licenseCount) {
        this.licenseCount = licenseCount;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getMeetingDescription() {
        return meetingDescription;
    }

    public void setMeetingDescription(String meetingDescription) {
        this.meetingDescription = meetingDescription;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Transient
    public Duration getDuration() {
        return new Duration(getSlotStart().getMillis(), getSlotEnd().getMillis());
    }

}
