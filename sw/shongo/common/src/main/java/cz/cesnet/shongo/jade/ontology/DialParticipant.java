package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DialParticipant extends ConnectorAgentAction
{
    private String roomId;
    private String roomUserId;

    // either alias or address will be used
    private Alias alias = null;
    private String address = null;

    public DialParticipant()
    {
    }

    public DialParticipant(String roomId, String roomUserId, String address)
    {
        this.roomId = roomId;
        this.roomUserId = roomUserId;
        this.address = address;
    }

    public DialParticipant(String roomId, String roomUserId, Alias alias)
    {
        this.roomId = roomId;
        this.roomUserId = roomUserId;
        this.alias = alias;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        if (alias != null && address != null) {
            throw new IllegalStateException(
                    "Both alias and address set for the DialParticipant command - should be just one.");
        }

        if (alias != null) {
            logger.info(String.format("Dialing in room %s user %s at alias %s", roomId, roomUserId, alias));
            getMultipoint(connector).dialParticipant(roomId, roomUserId, alias);
            return null; // FIXME
        }
        else {
            logger.info(String.format("Dialing in room %s user %s at address %s", roomId, roomUserId, alias));
            getMultipoint(connector).dialParticipant(roomId, roomUserId, address);
            return null; // FIXME
        }
    }

    public String toString()
    {
        if (alias != null && address != null) {
            throw new IllegalStateException(
                    "Both alias and address set for the DialParticipant command - should be just one.");
        }

        String target;
        if (alias != null) {
            target = "alias: " + alias;
        }
        else {
            target = "address: " + address;
        }

        return String.format("DialParticipant agent action (room: %s, roomUser: %s, %s)", roomId, roomUserId, target);
    }

    public Alias getAlias()
    {
        return alias;
    }

    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    public String getRoomUserId()
    {
        return roomUserId;
    }

    public void setRoomUserId(String roomUserId)
    {
        this.roomUserId = roomUserId;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }
}
