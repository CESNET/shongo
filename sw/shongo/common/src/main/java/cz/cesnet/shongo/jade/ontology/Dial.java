package cz.cesnet.shongo.jade.ontology;

import jade.content.AgentAction;

/**
 * Command to dial a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Dial implements AgentAction
{
    private String number;

    public Dial()
    {
    }

    public Dial(String number)
    {
        this.number = number;
    }

    public String getNumber()
    {
        return number;
    }

    public void setNumber(String number)
    {
        this.number = number;
    }
}
