package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.CommandException;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.controller.api.jade.Service#getUserInformation}
 */
public class GetUserInformation extends ControllerCommand
{
    private String userId;

    private String originalId;

    public GetUserInformation()
    {
    }

    public static GetUserInformation byUserId(String userId)
    {
        GetUserInformation getUserInformation = new GetUserInformation();
        getUserInformation.setUserId(userId);
        return getUserInformation;
    }

    public static GetUserInformation byOriginalId(String originalId)
    {
        GetUserInformation getUserInformation = new GetUserInformation();
        getUserInformation.setOriginalId(originalId);
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

    public String getOriginalId()
    {
        return originalId;
    }

    public void setOriginalId(String originalId)
    {
        this.originalId = originalId;
    }

    @Override
    public Object execute(Service commonService, String senderAgentName) throws CommandException
    {
        if (userId != null) {
            return commonService.getUserInformation(userId);
        }
        else {
            return commonService.getUserInformationByOriginalId(originalId);
        }
    }

    @Override
    public String toString()
    {
        if (userId != null) {
            return String.format(GetUserInformation.class.getSimpleName() + " (userId: %s)", userId);
        }
        else {
            return String.format(GetUserInformation.class.getSimpleName() + " (originalId: %s)", originalId);
        }
    }
}
