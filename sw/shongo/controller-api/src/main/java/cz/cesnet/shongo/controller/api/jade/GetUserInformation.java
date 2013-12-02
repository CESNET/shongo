package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.jade.CommandException;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see cz.cesnet.shongo.controller.api.jade.Service#getUserInformation
 * @see cz.cesnet.shongo.controller.api.jade.Service#getUserInformationByPrincipalName
 */
public class GetUserInformation extends ControllerCommand
{
    private String userId;

    private String principalName;

    public GetUserInformation()
    {
    }

    public static GetUserInformation byUserId(String userId)
    {
        GetUserInformation getUserInformation = new GetUserInformation();
        getUserInformation.setUserId(userId);
        return getUserInformation;
    }

    public static GetUserInformation byPrincipalName(String principalName)
    {
        GetUserInformation getUserInformation = new GetUserInformation();
        getUserInformation.setPrincipalName(principalName);
        return getUserInformation;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getPrincipalName()
    {
        return principalName;
    }

    public void setPrincipalName(String principalName)
    {
        this.principalName = principalName;
    }

    @Override
    public Object execute(Service commonService, String senderAgentName) throws CommandException
    {
        if (userId != null) {
            return commonService.getUserInformation(userId);
        }
        else {
            return commonService.getUserInformationByPrincipalName(principalName);
        }
    }

    @Override
    public String toString()
    {
        if (userId != null) {
            return String.format(GetUserInformation.class.getSimpleName() + " (userId: %s)", userId);
        }
        else {
            return String.format(GetUserInformation.class.getSimpleName() + " (principalName: %s)", principalName);
        }
    }
}
