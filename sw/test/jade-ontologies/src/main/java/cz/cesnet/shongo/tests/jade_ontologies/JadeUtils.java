package cz.cesnet.shongo.tests.jade_ontologies;

import jade.core.Profile;
import jade.core.ProfileException;
import jade.core.Specifier;
import jade.util.leap.List;
import jade.wrapper.AgentContainer;

/**
 * A utility class for common Jade usage.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeUtils
{
    /**
     * Default address if no address is specified.
     */
    public static final Address DEFAULT_ADDRESS = new Address("127.0.0.1", 1099);

    public static AgentContainer containerFactory(Profile profile, Address localAddress)
    {
        // setup the local host and port explicitly to prevent Jade guessing it (wrongly)
        profile.setParameter(Profile.LOCAL_HOST, localAddress.getHost());
        profile.setParameter(Profile.LOCAL_PORT, Integer.toString(localAddress.getPort()));

        if (profile.isMain()) {
            return jade.core.Runtime.instance().createMainContainer(profile);
        }
        else {
            return jade.core.Runtime.instance().createAgentContainer(profile);
        }
    }

    /**
     * Adds a service specifier to a profile for starting a container.
     *
     * @param profile      profile to add the service specifier to
     * @param serviceClass class providing the service
     */
    public static void addService(Profile profile, Class serviceClass)
    {
        Specifier spec = new Specifier();
        spec.setClassName(serviceClass.getName());

        try {
            List services = profile.getSpecifiers(Profile.SERVICES);
            services.add(spec);
            profile.setSpecifiers(Profile.SERVICES, services);
        }
        catch (ProfileException e) {
            System.err.println("Could not add the service " + serviceClass
                    .getName() + " (could not load the profile service specifiers)");
        }
    }

    /**
     * Instructs Jade to stop all its threads upon exit (otherwise, the program would hang waiting for other threads).
     */
    public static void treatShutDown()
    {
        jade.core.Runtime.instance().setCloseVM(true);
    }

}
