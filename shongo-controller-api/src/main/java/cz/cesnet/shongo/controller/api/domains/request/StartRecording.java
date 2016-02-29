package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;

/**
 * Action to foreign virtual room to start recording if available.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class StartRecording extends AbstractDomainRoomAction
{
    @Override
    public ConnectorCommand toApi()
    {
        return null;
    }

    @Override
    public <T extends AbstractResponse> Class<T> getReturnClass()
    {
        return null;
    }
}
