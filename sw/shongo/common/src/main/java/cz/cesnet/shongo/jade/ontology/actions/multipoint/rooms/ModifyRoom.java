package cz.cesnet.shongo.jade.ontology.actions.multipoint.rooms;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

import java.util.Map;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ModifyRoom extends ConnectorAgentAction
{
    private String roomId;
    private Map<String, Object> attributes;
    private Map<Room.Option, Object> options;

    public ModifyRoom()
    {
    }

    public ModifyRoom(String roomId, Map<String, Object> attributes, Map<Room.Option, Object> options)
    {
        this.attributes = attributes;
        this.options = options;
        this.roomId = roomId;
    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes)
    {
        this.attributes = attributes;
    }

    public Map<Room.Option, Object> getOptions()
    {
        return options;
    }

    public void setOptions(Map<Room.Option, Object> options)
    {
        this.options = options;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("Modifying room {}", roomId);
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                logger.info("  - setting attribute '{}' to '{}'", entry.getKey(), entry.getValue());
            }
        }
        if (options != null) {
            for (Map.Entry<Room.Option, Object> entry : options.entrySet()) {
                logger.info("  - setting option '{}' to '{}'", entry.getKey(), entry.getValue());
            }
        }
        return getMultipoint(connector).modifyRoom(roomId, attributes, options);
    }

    public String toString()
    {
        return String.format("ModifyRoom agent action (room: %s)", roomId);
    }
}
