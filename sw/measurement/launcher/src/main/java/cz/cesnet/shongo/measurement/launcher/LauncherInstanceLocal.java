package cz.cesnet.shongo.measurement.launcher;

import cz.cesnet.shongo.measurement.common.StreamConnector;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class LauncherInstanceLocal extends LauncherInstance {

    Process process;

    LauncherInstanceLocal(String id) {
        super(id);
    }

    @Override
    public void run(String command) {
        System.out.println("[LOCAL:" + getId() + "] Run {" + command + "}");

        // Run process
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        String name = "    " + getId();

        // Print standard output
        StreamConnector streamConnectorOutput = new StreamConnector(process.getInputStream(), System.out, name);
        streamConnectorOutput.start();

        // Print error output
        StreamConnector streamConnectorError = new StreamConnector(process.getErrorStream(), System.err, name);
        streamConnectorError.start();
    }

    @Override
    public void perform(String command) {
        System.out.println("[LOCAL:" + getId() + "] Perform {" + command + "}");
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(process.getOutputStream());
        try {
            outputStreamWriter.write(command + "\n");
            outputStreamWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exit() {
        System.out.println("[LOCAL:" + getId() + "] Exit");
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
