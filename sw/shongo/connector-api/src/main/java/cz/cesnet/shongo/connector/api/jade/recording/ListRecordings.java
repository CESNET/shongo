package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ListRecordings extends ConnectorCommand
{
    private String roomId;

    public ListRecordings()
    {
    }

    public ListRecordings(String roomId)
    {
        this.roomId = roomId;
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
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getRecording(connector).listRecordings(roomId);
    }

    @Override
    public String toString()
    {
        return String.format(ListRecordings.class.getSimpleName() + " (roomId: %s)", roomId);
    }
}
