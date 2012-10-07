package cz.cesnet.shongo.jade.ontology.actions.multipoint.users;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DialParticipant extends ConnectorAgentAction
{
    private String roomId;

    // either alias or address will be used
    private Alias alias = null;
    private String address = null;

    public DialParticipant()
    {
    }

    public DialParticipant(String roomId, String address)
    {
        this.roomId = roomId;
        this.address = address;
    }

    public DialParticipant(String roomId, Alias alias)
    {
        this.roomId = roomId;
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
            logger.info("Dialing user at alias {} into room {}", alias, roomId);
            return getMultipoint(connector).dialParticipant(roomId, alias);
        }
        else {
            logger.info("Dialing user at address {} into room {}", address, roomId);
            return getMultipoint(connector).dialParticipant(roomId, address);
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

        return String.format("DialParticipant agent action (room: %s, %s)", roomId, target);
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

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }
}
