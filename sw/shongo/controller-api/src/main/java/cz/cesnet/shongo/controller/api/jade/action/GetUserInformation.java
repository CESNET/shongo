package cz.cesnet.shongo.controller.api.jade.action;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.controller.api.jade.ControllerAgentAction;
import cz.cesnet.shongo.controller.api.jade.Service;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.controller.api.jade.Service#getUserInformation(String)}
 */
public class GetUserInformation extends ControllerAgentAction
{
    private String userId;

    public GetUserInformation()
    {
    }

    public GetUserInformation(String userId)
    {
        this.userId = userId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    @Override
    public Object execute(Service commonService) throws CommandException
    {
        return commonService.getUserInformation(userId);
    }

    @Override
    public String toString()
    {
        return String.format(GetUserInformation.class.getSimpleName() + " (userId: %s)", userId);
    }
}
