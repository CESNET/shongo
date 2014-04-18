package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.Predicate;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeviceError implements Predicate
{
    private String message;

    public DeviceError(String message)
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
}
