package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.jade.CommandException;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link Service#getRoom}
 */
public class GetRoom extends ControllerCommand
{
    private String roomId;

    public GetRoom()
    {
    }

    public GetRoom(String roomId)
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
    public Object execute(Service commonService, String senderAgentName) throws CommandException
    {
        return commonService.getRoom(senderAgentName, roomId);
    }

    @Override
    public String toString()
    {
        return String.format(GetRoom.class.getSimpleName() + " (roomId: %s)", roomId);
    }
}
