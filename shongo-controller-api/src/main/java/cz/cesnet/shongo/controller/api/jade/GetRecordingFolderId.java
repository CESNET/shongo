package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.jade.CommandException;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.controller.api.jade.Service#getRecordingFolderId}
 */
public class GetRecordingFolderId extends ControllerCommand
{
    private String roomId;

    public GetRecordingFolderId()
    {
    }

    public GetRecordingFolderId(String roomId)
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
        return commonService.getRecordingFolderId(senderAgentName, roomId);
    }

    @Override
    public String toString()
    {
        return String.format(GetRecordingFolderId.class.getSimpleName() + " (roomId: %s)", roomId);
    }
}
