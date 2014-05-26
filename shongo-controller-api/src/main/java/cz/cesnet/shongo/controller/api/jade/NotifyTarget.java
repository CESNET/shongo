package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.jade.CommandException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link Service#notifyTarget}
 */
public class NotifyTarget extends ControllerCommand
{
    /**
     * @see Service.NotifyTargetType
     */
    private Service.NotifyTargetType targetType;

    /**
     * Identifier of target which should be notified.
     */
    private String targetId;

    /**
     * Map of notification titles by languages.
     */
    private Map<String, String> titles = new HashMap<String, String>();

    /**
     * Map of notification messages by languages.
     */
    private Map<String, String> messages = new HashMap<String, String>();

    public NotifyTarget()
    {
    }

    public NotifyTarget(Service.NotifyTargetType targetType)
    {
        this.targetType = targetType;
    }

    public NotifyTarget(Service.NotifyTargetType targetType, String targetId)
    {
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public NotifyTarget(Service.NotifyTargetType targetType, String targetId, String title, String message)
    {
        this.targetType = targetType;
        this.targetId = targetId;
        addMessage("en", title, message);
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

    public Map<String, String> getTitles()
    {
        return titles;
    }

    public void setTitles(Map<String, String> titles)
    {
        this.titles.clear();
        this.titles.putAll(titles);
    }

    public Map<String, String> getMessages()
    {
        return messages;
    }

    public void setMessages(Map<String, String> messages)
    {
        this.messages.putAll(messages);
    }

    public void addMessage(String language, String title, String message)
    {
        if (language == null) {
            throw new IllegalArgumentException("Langunage must not be null.");
        }
        this.titles.put(language, title);
        this.messages.put(language, message);
    }

    @Override
    public Object execute(Service commonService, String senderAgentName) throws CommandException
    {
        commonService.notifyTarget(senderAgentName, targetType, targetId, titles, messages);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(NotifyTarget.class.getSimpleName() + " (type: %s, id: %s)", targetType, targetId);
    }
}
