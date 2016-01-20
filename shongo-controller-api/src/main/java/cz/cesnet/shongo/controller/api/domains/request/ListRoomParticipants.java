package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

import java.util.List;

/**
 * Get room's participants.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class ListRoomParticipants extends AbstractDomainRoomAction
{
    @Override
    public ConnectorCommand toApi()
    {
        cz.cesnet.shongo.connector.api.jade.multipoint.ListRoomParticipants listRoomParticipants;
        listRoomParticipants = new cz.cesnet.shongo.connector.api.jade.multipoint.ListRoomParticipants();

        return listRoomParticipants;
    }

    @Override
    public Class<List> getReturnClass()
    {
        return List.class;
    }
}
