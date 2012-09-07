package cz.cesnet.shongo.jade.ontology;

import jade.content.AgentAction;

/**
 * An action instructing the agent to print a plaintext message on the terminal.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Message implements AgentAction
{
    String message;

    public Message()
    {
    }

    public Message(String message)
    {
        this.message = message;
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
    public String toString()
    {
        return String.format("Message agent action (%s)", message);
    }
}
