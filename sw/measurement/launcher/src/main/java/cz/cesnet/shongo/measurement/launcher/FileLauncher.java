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

    public static String replaceVariables(String command, Map<String, String> variables) {
        Iterator it = variables.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> variable = (Map.Entry<String, String>)it.next();
            command = command.replaceAll("\\{" + variable.getKey() + "\\}", variable.getValue());
        }
        return command;
    }

    public static void launchFile(String file, Map<String, String> variables) {
        // Parse file
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

        // Set values to variables
        List<Variable> variableDefaults = launcher.getVariable();
        for ( Variable variable : variableDefaults ) {
            // Set default values if not defined
            if ( variables.get(variable.getName()) == null ) {
                String defaultValue = variable.getDefaultValue();
                if ( defaultValue == null )
                    defaultValue = "";
                else
                    defaultValue = replaceVariables(defaultValue, variables);
                variables.put(variable.getName(), defaultValue);
            }
            // Set value if should be set
            if ( variable.getValue() != null ) {
                String value = variable.getValue();
                value = replaceVariables(value, variables);
                variables.put(variable.getName(), value);
            }
            // Set value by platform
            String platformType = variables.get("platform");
            List<Platform> platforms = variable.getPlatform();
            for ( Platform platform : platforms ) {
                if ( platform.getType().equals(platformType) ) {
                    String value = platform.getValue();
                    value = replaceVariables(value, variables);
                    variables.put(variable.getName(), value);
                }
            }
        }

        // Get launcher instances
        List<Instance> instances = launcher.getInstance();

        // Wait for instances startup
        StreamMessageWaiter appStartedWaiter = new StreamMessageWaiter(Application.MESSAGE_STARTED,
                Application.MESSAGE_STARTUP_FAILED, instances.size());
        appStartedWaiter.start();

        System.out.println("[LAUNCHER] Running instances....");

        // Run instances
        Map<String, LauncherInstance> launcherInstances = new HashMap<String, LauncherInstance>();
        for ( Instance instance : instances ) {
            LauncherInstance launcherInstance = null;
            if ( instance.getType().equals("local") )
                launcherInstance = new LauncherInstanceLocal(instance.getId());
            else if ( instance.getType().equals("remote") )
                launcherInstance = new LauncherInstanceRemote(instance.getId(), replaceVariables(instance.getHost(), variables));
            else
                throw new IllegalArgumentException("Unknown instance type: " + instance.getType());

            String command = replaceVariables(instance.getContent().trim(), variables);

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
                appStartedWaiter.stop();
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

        System.out.println("[LAUNCHER] Instances successfully started!");

        // Perform commands
        List<Object> list = launcher.getCommandOrSleepOrCycle();
        for ( Object item : list ) {
            performItem(item, variables, launcherInstances);
        }

        // Exit instances
        for ( LauncherInstance launcherInstance : launcherInstances.values() )
            launcherInstance.exit();
    }

    public static void performItem(Object item, Map<String, String> variables, Map<String, LauncherInstance> launcherInstances)
    {
        if ( item instanceof Cycle ) {
            Cycle cycle = (Cycle)item;
            for ( int index = 0; index < cycle.getCount().intValue(); index++ ) {
                variables.put("index", new Integer(index).toString());
                List<Object> list = cycle.getCommandOrSleep();
                for ( Object listItem : list ) {
                    performItem(listItem, variables, launcherInstances);
                }
            }
        }
        // Step
        else if ( item instanceof Command ) {
            Command command = (Command)item;
            String commandText = replaceVariables(command.getContent().trim(), variables);
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
        // Sleep
        else if ( item instanceof Sleep) {
            Sleep sleep = (Sleep)item;
            try {
                long duration = sleep.getDuration().longValue();
                System.out.println("[SLEEP:" + duration + "].");
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
