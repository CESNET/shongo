package cz.cesnet.shongo.measurement.jade;

import cz.cesnet.shongo.measurement.common.Application;
import jade.core.Profile;
import jade.core.ProfileException;
import jade.core.ProfileImpl;
import jade.core.Specifier;
import jade.core.messaging.TopicManagementService;
import jade.core.replication.AddressNotificationService;
import jade.core.replication.MainReplicationService;
import jade.util.leap.List;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import static cz.cesnet.shongo.measurement.jade.JadeApplication.Mode.Platform;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeApplication extends Application {

    /**
     * The mode of the application - specific for Jade.
     * The application may run as a platform, as a backup or as a regular container just for agents.
     */
    public enum Mode {
        Platform,
        Backup,
        Container,
    }

    /** Mode of this application */
    private Mode mode = Platform;

    /** Use GUI for the platform? */
    private boolean useGui = false;

    /**
     * The host the container of this application listens at.
     * The default value is 127.0.0.1 for working in a LAN.
     * When launching the application, the default may be overridden by the --localhost parameter.
     */
    private String localHost = "127.0.0.1";

    /**
     * The port the container of this application listens on.
     * The default value is 1099 - the default port for Jade.
     * When launching the application, the default may be overridden by the --localhost parameter.
     */
    private int localPort = 1099;

    /**
     * Hostname of the platform to join when in the Container mode or to backup when in the Backup mode.
     * When launching the application, the default 127.0.0.1 may be overridden by the --join (or --backup) argument.
     */
    private String joinHost;

    /**
     * Port of the platform to join when in the Container mode or to backup when in the Backup mode.
     * When launching the application, the default 1099 may be overridden by the --join (or --backup) argument.
     */
    private int joinPort = 1099;

    /**
     * The container holding all agents of this instance.
     */
    private ContainerController container;

    private static ContainerController defaultContainer;

    /**
     * Returns the default agent container.
     *
     * When a JadeApplication instance is run, it sets the default container to the created one
     * (either as a main or regular container).
     *
     * May return NULL when no JadeApplication has been run.
     */
    public static ContainerController getDefaultContainer() {
        return defaultContainer;
    }
    
    public static void main(String[] args) {
        Application.runApplication(args, new JadeApplication());
    }

    public JadeApplication() {
        super("jade");
    }

    @Override
    public Class getAgentClass() {
        return JadeAgent.class;
    }

    @Override
    protected void onInitOptions(Options options) {
        Option localhostOpt = OptionBuilder.withLongOpt("localhost")
                .withDescription("Run the container at the given host and port (" + localHost + ":" + localPort + " if not specified)")
                .hasArg()
                .create("l");
        options.addOption(localhostOpt);
        
        Option backup = OptionBuilder.withLongOpt("backup")
                .withDescription("Start this application as a backup of a controller at given host:port (127.0.0.1:1099 by default)")
                .hasOptionalArg()
                .create("b");
        options.addOption(backup);

        Option join = options.getOption("j");
        join.setDescription("Do not start a new platform, join the specified one (host:port, 127.0.0.1:1099 by default)");

        Option gui = OptionBuilder.withLongOpt("gui")
                .withDescription("Start GUI at the main container")
                .create("g");
        options.addOption(gui);
    }

    @Override
    protected String[] onProcessCommandLine(CommandLine commandLine) {
        useGui = commandLine.hasOption("gui");
        
        if (commandLine.hasOption("localhost")) {
            localHost = commandLine.getOptionValue("localhost");
            if (localHost.indexOf(':') != -1) {
                int colonPos = localHost.indexOf(':');
                localPort = Integer.parseInt(localHost.substring(colonPos+1));
                localHost = localHost.substring(0, colonPos);
            }
        }

        if (commandLine.hasOption("join") || commandLine.hasOption("backup")) {
            if (commandLine.hasOption("backup")) {
                mode = Mode.Backup;
                joinHost = commandLine.getOptionValue("backup", "127.0.0.1");
            }
            else {
                mode = Mode.Container;
                joinHost = commandLine.getOptionValue("join", "127.0.0.1");
            }
            if (joinHost.indexOf(':') != -1) {
                int colonPos = joinHost.indexOf(':');
                joinPort = Integer.parseInt(joinHost.substring(colonPos+1));
                joinHost = joinHost.substring(0, colonPos);
            }
        }

        return new String[0];
    }

    /**
     * Adds a service specifier to a profile for starting a container.
     * @param profile      profile to add the service specifier to
     * @param serviceClass class providing the service
     */
    static void addService(Profile profile, Class serviceClass) {
        Specifier spec = new Specifier();
        spec.setClassName(serviceClass.getName());

        try {
            List services = profile.getSpecifiers(Profile.SERVICES);
            services.add(spec);
            profile.setSpecifiers(Profile.SERVICES, services);
        } catch (ProfileException e) {
            logger.error("Could not add the service " + serviceClass.getName() + " (could not load the profile service specifiers)", e);
        }
    }
    
    static AgentContainer containerFactory(Mode mode, Profile profile, String localHost, int localPort) {
        // setup the local host and port explicitly to prevent Jade guessing it (wrongly)
        profile.setParameter(Profile.LOCAL_HOST, localHost);
        profile.setParameter(Profile.LOCAL_PORT, Integer.toString(localPort));

        if (mode == Platform) {
            addService(profile, TopicManagementService.class);
            addService(profile, MainReplicationService.class);
            addService(profile, AddressNotificationService.class);
            return jade.core.Runtime.instance().createMainContainer(profile);
        }
        else if (mode == Mode.Container) {
            addService(profile, TopicManagementService.class);
            addService(profile, AddressNotificationService.class);
            return jade.core.Runtime.instance().createAgentContainer(profile);
        }
        else if (mode == Mode.Backup) {
            profile.setParameter(Profile.LOCAL_SERVICE_MANAGER, Boolean.toString(true));
            addService(profile, TopicManagementService.class);
            addService(profile, MainReplicationService.class);
            addService(profile, AddressNotificationService.class);
            return jade.core.Runtime.instance().createMainContainer(profile);
        }
        else {
            throw new IllegalStateException("unknown JadeApplication mode");
        }
    }
    
    @Override
    protected boolean onRun() {
        // create the profile according to the operation mode
        Profile profile;
        switch (mode) {
            case Platform:
                profile = new ProfileImpl();
                break;
            case Container:
                profile = new ProfileImpl(joinHost, joinPort, null, false);
                break;
            case Backup:
                profile = new ProfileImpl(joinHost, joinPort, null);
                break;
            default:
                throw new IllegalStateException("unknown JadeApplication mode");
        }

        // create the container
        container = containerFactory(mode, profile, localHost, localPort);

        // GUI
        if (useGui && mode == Platform) {
            try {
                AgentController rma = container.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
                rma.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        // register the container as default for agents within the same JVM
        if (defaultContainer == null) {
            defaultContainer = container;
        }
        
        // upon exit, shut down the Jade threads (otherwise, the program would hang waiting for other threads)
        jade.core.Runtime.instance().setCloseVM(true);

        return true;
    }

    /**
     * A stub to be used when the issue of exiting the application is solved.
     */
    @Override
    protected void onExit() {
        try {
            container.kill();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
