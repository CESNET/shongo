package cz.cesnet.shongo.jade.ontology;

import jade.content.AgentAction;

/**
 * Message agent action.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Message implements AgentAction
{
    String message;

    public Message(String message)
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }
}
