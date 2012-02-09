package cz.cesnet.shongo.measurement.launcher;

import cz.cesnet.shongo.measurement.common.StreamConnector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;

public class LauncherInstanceLocal extends LauncherInstance {

    Process process;
    Watcher watcher;
    boolean profile;

    LauncherInstanceLocal(String id, boolean profile) {
        super(id);
        this.profile = profile;
    }

    @Override
    public boolean run(String command) {
        System.out.println("[LOCAL:" + getId() + "] Run [" + command + "]");

        // Run process
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        int pid = 0;
        try {
            Field field = process.getClass().getDeclaredField("pid");
            field.setAccessible(true);
            pid = (Integer)field.get(process);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ( profile ) {
            System.out.println("[LAUNCHER:PROFILING] PID [" + pid + "]");
            watcher = new Watcher(pid);
            watcher.start();
        }

        String name = "    " + getId();

        // Print standard output
        StreamConnector streamConnectorOutput = new StreamConnector(process.getInputStream(), System.out, name);
        streamConnectorOutput.start();

        // Print error output
        StreamConnector streamConnectorError = new StreamConnector(process.getErrorStream(), System.err, name);
        streamConnectorError.start();

        return true;
    }

    @Override
    public void perform(String command) {
        System.out.println("[LOCAL:" + getId() + "] Perform [" + command + "]");
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
        if ( watcher != null ) {
            watcher.stop();
            System.out.println("[LAUNCHER:PROFILING] Result:");
            System.out.println();
            System.out.print(watcher.getOutput());
            System.out.println();
        }

        System.out.println("[LOCAL:" + getId() + "] Exit");
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Watcher extends Thread
    {
        private int pid;

        private StringBuilder builder = new StringBuilder();
        
        public Watcher(int pid)
        {
            this.pid = pid;
        }

        public String getOutput()
        {
            return builder.toString();
        }

        @Override
        public void run() {
            while ( true ) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}

                try {
                    Process process = Runtime.getRuntime().exec("./profile.sh " + pid);

                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()) );
                    String line;
                    while ((line = in.readLine()) != null) {
                        builder.append(line + "\n");
                    }
                    in.close();
                } catch (IOException e) {}

            }
        }
    }
}
