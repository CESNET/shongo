package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.CommandException;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link Service#notifyTarget}
 */
public class NotifyTarget extends ControllerCommand
{
    private Service.NotifyTargetType targetType;

    private String targetId;

    private String title;

    private String message;

    public NotifyTarget()
    {
    }

    public NotifyTarget(Service.NotifyTargetType targetType, String targetId, String title, String message)
    {
        this.targetType = targetType;
        this.targetId = targetId;
        this.title = title;
        this.message = message;
    }

    public Service.NotifyTargetType getTargetType()
    {
        return targetType;
    }

    public void setTargetType(Service.NotifyTargetType targetType)
    {
        this.targetType = targetType;
    }

    public String getTargetId()
    {
        return targetId;
    }

    public void setTargetId(String targetId)
    {
        this.targetId = targetId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    @Override
    public Object execute(Service commonService, String senderAgentName) throws CommandException
    {
        commonService.notifyTarget(senderAgentName, targetType, targetId, title, message);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(NotifyTarget.class.getSimpleName() + " (type: %s, id: %s, title: %s)",
                targetType, targetId, title);
    }
}
