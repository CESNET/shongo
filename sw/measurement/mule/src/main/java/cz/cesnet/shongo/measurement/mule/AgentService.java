package cz.cesnet.shongo.measurement.mule;

/**
 * Mule Agent Service
 *
 * @author Martin Srom
 */
public class AgentService {

    public AgentService()
    {
        System.out.println("AgentService initializing!");
    }

    public String echo(String echo)
    {
        System.out.println("Received message [" + echo + "]!");
        return "AgentService [" + echo + "]";
    }

}
