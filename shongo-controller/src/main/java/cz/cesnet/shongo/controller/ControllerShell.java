package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.api.rpc.RpcServerRequestLogger;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.scheduler.Preprocessor;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.Shell;
import cz.cesnet.shongo.util.ConsoleAppender;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an cmd-line shell for the {@link Controller}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerShell extends Shell
{
    private static Logger logger = LoggerFactory.getLogger(ControllerShell.class);

    /**
     * Constructor.
     *
     * @param controller
     */
    public ControllerShell(final Controller controller)
    {
        Container controllerContainer = controller.getJadeContainer();
        ControllerAgent controllerAgent = controller.getAgent();

        setPrompt("controller");
        setExitCommand("exit", "Shutdown the controller");
        addCommands(ContainerCommandSet.createContainerCommandSet(controllerContainer));
        if (controllerAgent != null) {
            addCommands(ContainerCommandSet.createContainerAgentCommandSet(
                    controllerContainer, controllerAgent.getLocalName()));
            addCommands(controllerAgent.createCommandSet());
        }
        addCommand("log", "[rpc|sql|sql-param] Toggle logging specified type", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length <= 1) {
                    return;
                }
                org.apache.log4j.Logger logger = null;
                Boolean enabled = null;
                if (args[1].equals("rpc")) {
                    enabled = !RpcServerRequestLogger.isEnabled();
                    RpcServerRequestLogger.setEnabled(enabled);
                    logger = org.apache.log4j.Logger.getLogger(
                            RpcServerRequestLogger.class);
                }
                else if (args[1].equals("sql")) {
                    logger = org.apache.log4j.Logger.getLogger("org.hibernate.SQL");
                }
                else if (args[1].equals("sql-param")) {
                    logger = org.apache.log4j.Logger.getLogger("org.hibernate.type");
                }
                if (logger == null) {
                    return;
                }
                if (enabled == null) {
                    enabled = logger.getLevel() == null || logger.getLevel().isGreaterOrEqual(Level.INFO);
                }
                if (enabled) {
                    ControllerShell.logger.info("Enabling '{}' logger.", args[1]);
                    logger.setLevel(Level.TRACE);
                }
                else {
                    ControllerShell.logger.info("Disabling '{}' logger.", args[1]);
                    logger.setLevel(Level.INFO);
                }
            }
        });
        addCommand("log-filter", "[*|<filter>] Filter logging by string (warning and errors are always not filtered)",
                new CommandHandler()
                {
                    @Override
                    public void perform(CommandLine commandLine)
                    {
                        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
                        ConsoleAppender consoleAppender = (ConsoleAppender) logger.getAppender("CONSOLE");
                        if (consoleAppender != null) {
                            String[] args = commandLine.getArgs();
                            String filter = null;
                            if (args.length > 1) {
                                filter = args[1].trim();
                                if (filter.equals("*")) {
                                    filter = null;
                                }
                            }
                            consoleAppender.setFilter(null);
                            if (filter != null) {
                                ControllerShell.logger.info("Enabling logger filter for '{}'.", filter);
                            }
                            else {
                                ControllerShell.logger.info("Disabling logger filter.");
                            }
                            consoleAppender.setFilter(filter);
                        }
                    }
                });
        addCommand("notification", "[on|off|redirect <email-address>] Configure notification executor", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length < 2) {
                    return;
                }
                NotificationManager notificationManager = controller.getNotificationManager();
                if (args[1].equals("on")) {
                    notificationManager.setEnabled(true);
                    notificationManager.setRedirectTo(null);
                    ControllerShell.logger.info("Notifications are enabled.");
                }
                else if (args[1].equals("off")) {
                    notificationManager.setEnabled(false);
                    notificationManager.setRedirectTo(null);
                    ControllerShell.logger.info("Notifications are disabled.");
                }
                else if (args[1].equals("redirect")) {
                    if (args.length < 3) {
                        return;
                    }
                    final String email = args[2];
                    notificationManager.setEnabled(true);
                    notificationManager.setRedirectTo(new PersonInformation()
                    {
                        @Override
                        public String getFullName()
                        {
                            return "Redirect";
                        }

                        @Override
                        public String getRootOrganization()
                        {
                            return "Unknown";
                        }

                        @Override
                        public String getPrimaryEmail()
                        {
                            return email;
                        }
                    });
                    ControllerShell.logger.info("Notifications are redirected to {}.", email);
                }
            }
        });
        addCommand("worker", "[on|off] Switch on/off preprocessor and scheduler", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length <= 1) {
                    return;
                }
                Preprocessor preprocessor = controller.getComponent(Preprocessor.class);
                Scheduler scheduler = controller.getComponent(Scheduler.class);
                if (args[1].equals("on")) {
                    ControllerShell.logger.info("Preprocessor and scheduler are enabled.");
                    preprocessor.setEnabled(true);
                    scheduler.setEnabled(true);
                }
                else if (args[1].equals("off")) {
                    ControllerShell.logger.info("Preprocessor and scheduler are disabled.");
                    preprocessor.setEnabled(false);
                    scheduler.setEnabled(false);
                }
            }
        });
        addCommand("executor", "[on|off] Switch on/off executor", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length <= 1) {
                    return;
                }
                Executor executor = controller.getComponent(Executor.class);
                if (args[1].equals("on")) {
                    ControllerShell.logger.info("Executor is enabled.");
                    executor.setEnabled(true);
                }
                else if (args[1].equals("off")) {
                    ControllerShell.logger.info("Executor is disabled.");
                    executor.setEnabled(false);
                }
            }
        });
    }
}
