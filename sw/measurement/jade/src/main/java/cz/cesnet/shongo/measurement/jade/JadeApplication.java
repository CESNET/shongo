package cz.cesnet.shongo.measurement.jade;

import cz.cesnet.shongo.measurement.common.Application;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.util.leap.Properties;
import jade.wrapper.*;
import jade.wrapper.gateway.JadeGateway;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
        Backup, // FIXME: add support for platform backup
        Container,
    }

    /** Mode of this application */
    private Mode mode = Mode.Platform;

    /** Use GUI for the platform? */
    private boolean useGui = false;

    /**
     * If starting a new platform, it is listening on this host.
     * It is hardcoded right now, until there is a need to change it...
     */
    private final String platformHost = "127.0.0.1";

    /**
     * If starting a new platform, it is listening on this port.
     * The default value is 1099 - the default port for Jade.
     * When launching the application, the default may be overridden by the --platform-port parameter.
     */
    private int platformPort = 1099;

    /**
     * Hostname of the platform to join when in the Container mode.
     * When launching the application, the default 127.0.0.1 may be overridden by the --join parameter.
     */
    private String joinHost;

    /**
     * Port of the platform to join when in the Container mode.
     * When launching the application, the default 1099 may be overridden by the --join parameter.
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
        Option pport = OptionBuilder.withLongOpt("platform-port")
                .withDescription("Start the new platform on this port (" + platformPort + " by default)")
                .hasArg()
                .create("p");
        options.addOption(pport);

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
        
        if (commandLine.hasOption("platform-port")) {
            platformPort = Integer.parseInt(commandLine.getOptionValue("platform-port"));
        }
        
        if (commandLine.hasOption("join")) {
            mode = Mode.Container;
            joinHost = commandLine.getOptionValue("join", "127.0.0.1");
            if (joinHost.indexOf(':') != -1) {
                int colonPos = joinHost.indexOf(':');
                joinPort = Integer.parseInt(joinHost.substring(0, colonPos));
                joinHost = joinHost.substring(colonPos+1);
            }
        }

        return new String[0];
    }

    @Override
    protected void onRun() {
        if (mode == Mode.Platform) {
            Profile profile = new ProfileImpl(platformHost, platformPort, null);
            container = jade.core.Runtime.instance().createMainContainer(profile);
        }
        else if (mode == Mode.Container) {
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, joinHost);
            profile.setParameter(Profile.MAIN_PORT, Integer.toString(joinPort));
            container = jade.core.Runtime.instance().createAgentContainer(profile);
        }
        else if (mode == Mode.Backup) {
            throw new NotImplementedException();
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
        
        // upon exit, shut down the Jade threads (otherwise, the program would hang waiting for other threads)
        jade.core.Runtime.instance().setCloseVM(true);
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
