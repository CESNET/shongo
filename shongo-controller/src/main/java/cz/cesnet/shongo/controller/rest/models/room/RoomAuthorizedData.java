package cz.cesnet.shongo.controller.rest.models.room;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.api.AbstractRoomExecutable;
import lombok.Data;

import java.util.List;

/**
 * Represents authorized data for {@link AbstractRoomExecutable}.
 *
 * @author Filip Karnis
 */
@Data
public class RoomAuthorizedData
{

    private String pin;
    private String adminPin;
    private String guestPin;
    private Boolean allowGuests;
    private List<Alias> aliases;

    public RoomAuthorizedData(AbstractRoomExecutable roomExecutable)
    {
        pin = roomExecutable.getPin();
        adminPin = roomExecutable.getAdminPin();
        guestPin = roomExecutable.getGuestPin();
        allowGuests = roomExecutable.getAllowGuests();
        aliases = roomExecutable.getAliases();
    }
}
