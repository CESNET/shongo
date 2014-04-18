package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Locale;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class RoomAvailableNotification extends ConfigurableNotification
{
    /**
     * @see RoomEndpoint
     */
    protected RoomEndpoint roomEndpoint;

    /**
     * Constructor.
     *
     * @param roomEndpoint sets the {@link #roomEndpoint}
     */
    public RoomAvailableNotification(RoomEndpoint roomEndpoint)
    {
        this.roomEndpoint = roomEndpoint;

        for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
            if (participant instanceof PersonParticipant) {
                PersonParticipant personParticipant = (PersonParticipant) participant;
                addRecipient(personParticipant.getPersonInformation());
            }
        }
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    protected boolean onBeforeAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        if (roomEndpoint.getRoomConfiguration().getLicenseCount() == 0) {
            return false;
        }
        return super.onBeforeAdded(notificationManager, entityManager);
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);

        String roomName = RoomNotification.getRoomName(roomEndpoint);
        if (roomName != null) {
            roomName = " " + roomName;
        }
        else {
            roomName = "";
        }

        String title = renderContext.message("room.available.title", roomName,
                renderContext.formatInterval(roomEndpoint.getOriginalSlot()));

        renderContext.addParameter("roomEndpoint", roomEndpoint);
        renderContext.addParameter("aliases", Alias.sortedList(roomEndpoint.getAliases()));
        renderContext.addParameter("roomName", roomName);
        renderContext.addParameter("pin", roomEndpoint.getPin());
        return renderTemplateMessage(renderContext, title, "room-available.ftl");
    }
}
