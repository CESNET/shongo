package cz.cesnet.shongo.measurement.launcher;

import cz.cesnet.shongo.measurement.common.Application;
import cz.cesnet.shongo.measurement.common.StreamMessageWaiter;
import cz.cesnet.shongo.measurement.launcher.xml.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.*;

public class FileLauncher {

    public static void launchFile(String file, Evaluator evaluator) {
        // Parse XML file
        Launcher launcher = null;
        try {
            JAXBContext ctx = JAXBContext.newInstance(new Class[]{Launcher.class});
            Unmarshaller um = ctx.createUnmarshaller();
            launcher = (Launcher) um.unmarshal(new File(file));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        if ( launcher == null )
            return;

        // Load variables to evaluator
        List<Variable> variableDefaults = launcher.getVariable();
        for ( Variable variable : variableDefaults ) {
            // Set default values if not defined
            if ( evaluator.hasVariable(variable.getName()) == false ) {
                String defaultValue = variable.getDefaultValue();
                if ( defaultValue == null || defaultValue.equals("null") )
                    defaultValue = "";
                evaluator.setVariable(variable.getName(), defaultValue);
            }
            // Set value if should be set
            if ( variable.getValue() != null && variable.getValue().equals("null") == false ) {
                String value = variable.getValue();
                evaluator.setVariable(variable.getName(), value);
            }
            // Set value by platform
            String platformType = evaluator.getVariable("platform");
            List<Platform> platforms = variable.getPlatform();
            for ( Platform platform : platforms ) {
                String[] platformTypes = platform.getType().split(",");
                for ( String type : platformTypes ) {
                    if ( type.equals(platformType) ) {
                        String value = platform.getValue();
                        evaluator.setVariable(variable.getName(), value);
                    }
                }
            }
        }

        // Get launcher instances
        List<Instance> instances = launcher.getInstance();

        // Wait for instances startup
        StreamMessageWaiter appStartedWaiter = new StreamMessageWaiter(Application.MESSAGE_STARTED,
                Application.MESSAGE_STARTUP_FAILED, instances.size());
        appStartedWaiter.startWatching();

        System.out.println("[LAUNCHER] Running instances....");

        // Run instances
        Map<String, LauncherInstance> launcherInstances = new HashMap<String, LauncherInstance>();
        for ( Instance instance : instances ) {
            String host = evaluator.evaluate(instance.getHost());
            LauncherInstance launcherInstance = null;
            if ( instance.getType().equals("local") )
                launcherInstance = new LauncherInstanceLocal(instance.getId());
            else if ( instance.getType().equals("remote") )
                launcherInstance = new LauncherInstanceRemote(instance.getId(), host);
            else
                throw new IllegalArgumentException("Unknown instance type: " + instance.getType());

            Evaluator evaluatorScoped = new Evaluator(evaluator);
            if ( host != null && host.equals("null") == false )
                evaluatorScoped.setVariable("host", host);

            String command = evaluatorScoped.evaluate(instance.getContent().trim());

            if ( instance.getRequire() != null ) {
                // Wait for all required instances to startup
                String[] require = instance.getRequire().split(",");
                for ( String requireItem : require ) {
                    if ( appStartedWaiter.isMessage(requireItem) )
                        continue;
                    System.out.println("[LAUNCHER:" + instance.getId() + "] Waiting for started event of [" + requireItem + "]");
                    while ( appStartedWaiter.isRunning() && appStartedWaiter.isMessage(requireItem) == false  ) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {}
                    }
                }
            }

            if ( !launcherInstance.run(command) ) {
                System.out.println("[LAUNCHER] Failed to run instance '" + instance.getId() + "'!");
                appStartedWaiter.stopWatching();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
                for ( LauncherInstance launchedInstance : launcherInstances.values() ) {
                    launchedInstance.perform("quit");
                    launchedInstance.exit();
                }
                return;
            }
            launcherInstances.put(launcherInstance.getId(), launcherInstance);
        }

        // Wait for instance to startup, and if some failed exit
        if ( appStartedWaiter.waitForMessages() == false ) {
            System.out.println("[LAUNCHER] Failed to run some instances!");
            return;
        }
        appStartedWaiter.stopWatchingSystem();

        System.out.println("[LAUNCHER] Instances successfully started!");

        // Perform commands
        List<Object> list = launcher.getCommandOrCycleOrEcho();
        for ( Object item : list ) {
            performItem(item, evaluator, launcherInstances);
        }

        // Exit instances
        for ( LauncherInstance launcherInstance : launcherInstances.values() )
            launcherInstance.exit();
    }

    public static void performItem(Object item, Evaluator evaluator, Map<String, LauncherInstance> launcherInstances)
    {
        Evaluator evaluatorScoped = new Evaluator(evaluator);
        // Cycle
        if ( item instanceof Cycle ) {
            Cycle cycle = (Cycle)item;
            for ( int index = 0; index < cycle.getCount().intValue(); index++ ) {
                evaluatorScoped.setVariable("index", new Integer(index).toString());
                List<Object> list = cycle.getCommandOrCycleOrEcho();
                for ( Object listItem : list ) {
                    performItem(listItem, evaluatorScoped, launcherInstances);
                }
            }
        }
        // Step
        else if ( item instanceof Command ) {
            Command command = (Command)item;
            String commandText = evaluatorScoped.evaluate(command.getContent().trim());
            // Command to all instances
            if ( command.getFor() == null || command.getFor().equals("*") ) {
                for ( LauncherInstance launcherInstance : launcherInstances.values() )
                    launcherInstance.perform(commandText);
            }
            // Command for specified instance
            else {
                LauncherInstance launcherInstance = launcherInstances.get(command.getFor());
                if ( launcherInstance != null )
                    launcherInstance.perform(commandText);
            }
        }
        // Echo
        else if ( item instanceof Echo) {
            Echo echo = (Echo)item;

            long durationBefore = 0;
            long durationAfter = 0;
            if ( echo.getSleep() != null ) {
                String sleep = evaluatorScoped.evaluate(echo.getSleep());
                durationBefore = Long.parseLong(sleep) / 2;
                durationAfter = durationBefore;
            }

            try {
                Thread.sleep(durationBefore);
            } catch (InterruptedException e) {}

            String value = evaluatorScoped.evaluate(echo.getValue());
            for ( LauncherInstance launcherInstance : launcherInstances.values() ) {
                launcherInstance.echo(value);
            }

            try {
                Thread.sleep(durationAfter);
            } catch (InterruptedException e) {}
        }
        // Sleep
        else if ( item instanceof Sleep) {
            Sleep sleep = (Sleep)item;
            try {
                long duration = Long.parseLong(evaluatorScoped.evaluate(sleep.getDuration()));
                System.out.println("[SLEEP:" + duration + "].");
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
