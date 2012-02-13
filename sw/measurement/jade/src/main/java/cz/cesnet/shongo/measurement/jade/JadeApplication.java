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
    private Mode mode = Mode.Platform;

    /** Use GUI for the platform? */
    private boolean useGui = false;

    /** The default local host to use if not specified. */
    public static final String defaultLocalHost = "127.0.0.1";

    /** The default local port to use if not specified. */
    public static final int defaultLocalPort = 1099;

    /**
     * The host the container of this application listens at.
     * The default value is 127.0.0.1 for working in a LAN.
     * When launching the application, the default may be overridden by the --platform-host parameter.
     */
    private String listeningHost = "127.0.0.1";

    /**
     * The port the container of this application listens on.
     * The default value is 1099 - the default port for Jade.
     * When launching the application, the default may be overridden by the --platform-port parameter.
     */
    private int listeningPort = 1099;

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
        Option pHost = OptionBuilder.withLongOpt("platform-host")
                .withDescription("Start the new platform on this port (" + listeningHost + " by default)")
                .hasArg()
                .create("l");
        options.addOption(pHost);

        Option pPort = OptionBuilder.withLongOpt("platform-port")
                .withDescription("Start the new platform on this port (" + listeningPort + " by default)")
                .hasArg()
                .create("p");
        options.addOption(pPort);

        Option backup = OptionBuilder.withLongOpt("backup")
                .withDescription("Start this application as a backup (on port given by the --platform-port switch) of a controller at given host:port (127.0.0.1:1099 by default)")
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

        if (commandLine.hasOption("platform-host")) {
            listeningHost = commandLine.getOptionValue("platform-host");
        }
        if (commandLine.hasOption("platform-port")) {
            listeningPort = Integer.parseInt(commandLine.getOptionValue("platform-port"));
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
                joinPort = Integer.parseInt(joinHost.substring(0, colonPos));
                joinHost = joinHost.substring(colonPos+1);
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
    
    static AgentContainer containerFactory(Mode mode, Profile profile) {
//        if (profile.getParameter(Profile.LOCAL_HOST, null) == null) {
//            profile.setParameter(Profile.LOCAL_HOST, defaultLocalHost);
//        }
//        if (profile.getParameter(Profile.LOCAL_PORT, null) == null) {
//            profile.setParameter(Profile.LOCAL_PORT, Integer.toString(defaultLocalPort));
//        }
        
        if (mode == Mode.Platform) {
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
        if (mode == Mode.Platform) {
            Profile profile = new ProfileImpl(listeningHost, listeningPort, null);
            container = containerFactory(mode, profile);
        }
        else if (mode == Mode.Container) {
            Profile profile = new ProfileImpl(listeningHost, listeningPort, null, false);
            profile.setParameter(Profile.MAIN_HOST, joinHost);
            profile.setParameter(Profile.MAIN_PORT, Integer.toString(joinPort));
            container = containerFactory(mode, profile);
        }
        else if (mode == Mode.Backup) {
            Profile profile = new ProfileImpl(listeningHost, listeningPort, null);
            profile.setParameter(Profile.MAIN_HOST, joinHost);
            profile.setParameter(Profile.MAIN_PORT, Integer.toString(joinPort));
            container = containerFactory(mode, profile);
        }
        else {
            throw new IllegalStateException("unknown JadeApplication mode");
        }

        if (useGui && mode == Mode.Platform) {
            try {
                AgentController rma = container.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
                rma.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        // initialize the default container for the agents within the same JVM
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
