package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.Room;

import java.util.List;

/**
 * Get room's info.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class GetRoom extends AbstractDomainRoomAction
{
    @Override
    public ConnectorCommand toApi()
    {
        cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom getRoom;
        getRoom = new cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom();

        return getRoom;
    }

    @Override
    public Class<Room> getReturnClass()
    {
        return Room.class;
    }
}
