package cz.cesnet.shongo.measurement.launcher;

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

        System.out.println("[LAUNCHER] Running instances....");

        // Run instances
        Map<String, LauncherInstance> launcherInstances = new HashMap<String, LauncherInstance>();
        List<Instance> instances = launcher.getInstance();
        for ( Instance instance : instances ) {
            LauncherInstance launcherInstance = null;
            if ( instance.getType().equals("local") )
                launcherInstance = new LauncherInstanceLocal(instance.getId());
            else if ( instance.getType().equals("remote") )
                launcherInstance = new LauncherInstanceRemote(instance.getId(), replaceVariables(instance.getHost(), variables));
            String command = replaceVariables(instance.getContent().trim(), variables);
            if ( launcherInstance.run(command) == false ) {
                System.out.println("[LAUNCHER] Failed to run instance '" + instance.getId() + "'!");
                for ( LauncherInstance launchedInstance : launcherInstances.values() )
                    launchedInstance.exit();
                return;
            }
            launcherInstances.put(launcherInstance.getId(), launcherInstance);
        }

        // Perform commands
        List<Object> list = launcher.getSleepOrStep();
        for ( Object item : list ) {
            // Step
            if ( item instanceof Step ) {
                Step step = (Step)item;
                // For all command in step
                for ( Command command : step.getCommand() ) {
                    // Command to all instances
                    if ( command.getFor() == null || command.getFor().equals("*") ) {
                        for ( LauncherInstance launcherInstance : launcherInstances.values() )
                            launcherInstance.perform(command.getContent().trim());
                    }
                    // Command for specified instance
                    else {
                        LauncherInstance launcherInstance = launcherInstances.get(command.getFor());
                        if ( launcherInstance == null )
                            continue;
                        launcherInstance.perform(command.getContent().trim());
                    }
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

        // Exit instances
        for ( LauncherInstance launcherInstance : launcherInstances.values() )
            launcherInstance.exit();
    }

}
