package cz.cesnet.shongo.measurement.launcher;

import cz.cesnet.shongo.measurement.common.Application;
import cz.cesnet.shongo.measurement.common.StreamConnector;
import cz.cesnet.shongo.measurement.common.StreamMessageWaiter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;

/**
 * Instance of launcjer that runs on same computer as the instance of this class
 *
 * @author Martin Srom
 */
public class LauncherInstanceLocal extends LauncherInstance {

    /** Instance process */
    private Process process;

    /** Instance process pid */
    private int pid;

    /** Profiler that performs the profiling */
    private Profiler profiler;

    /** Command by which the instance was started */
    private String runCommand;

    /** Flag if instance is started */
    private boolean started = false;

    /** Application waiter */
    StreamMessageWaiter appWaiter = new StreamMessageWaiter();

    /**
     * Constructor
     *
     * @param id
     */
    LauncherInstanceLocal(String id)
    {
        super(id);
    }

    /**
     * Run local instance
     *
     * @param command
     * @return
     */
    @Override
    public boolean run(String command)
    {
        System.out.println("[LOCAL:" + getId() + "] Run [" + command + "]");

        // Run process
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        pid = 0;
        try {
            Field field = process.getClass().getDeclaredField("pid");
            field.setAccessible(true);
            pid = (Integer)field.get(process);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ( profiler != null ) {
            profiler.stop();
        }
        profiler = new Profiler(pid);
        profiler.start();

        String name = "    " + getId();

        // Print standard output
        StreamConnector streamConnectorOutput = new StreamConnector(process.getInputStream(), System.out, name);
        streamConnectorOutput.start();

        // Print error output
        StreamConnector streamConnectorError = new StreamConnector(process.getErrorStream(), System.err, name);
        streamConnectorError.start();

        runCommand = command;
        started = true;

        return true;
    }

    /**
     * Perform application start
     */
    private void performStart()
    {
        if ( started == true )
            return;
        System.out.println("[LOCAL:" + getId() + "] Starting application...");
        this.appWaiter.set(Application.MESSAGE_STARTED,
                Application.MESSAGE_STARTUP_FAILED, 1);
        this.appWaiter.startWatching();
        run(runCommand);
        this.appWaiter.waitForMessages();
    }

    /**
     * Perform application stop
     */
    private void performStop()
    {
        if ( started == false )
            return;

        // Quit application agents
        performInAgents("quit");

        // Close process output stream
        try {
            process.getOutputStream().close();
        } catch (IOException e) {}

        // Wait for stopping
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        started = false;
    }

    /**
     * Perform command in agents
     *
     * @param command
     */
    private void performInAgents(String command)
    {
        System.out.println("[LOCAL:" + getId() + "] Perform [" + command + "]");
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(process.getOutputStream());
        try {
            outputStreamWriter.write(command + "\n");
            outputStreamWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Perform command on local instance
     *
     * @param command
     */
    @Override
    public void perform(String command)
    {
        // Kill application
        if ( command.equals("kill") ) {
            if ( started == false )
                return;
            System.out.println("[LOCAL:" + getId() + "] Killing application...");
            try {
                Process process = Runtime.getRuntime().exec("scripts/kill.sh " + pid);
                process.waitFor();
                started = false;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Start application if not running
        else if ( command.equals("start") ) {
            performStart();
        }
        // Restart application
        else if ( command.equals("restart") ) {
            if ( started == false )
                return;
            System.out.println("[LOCAL:" + getId() + "] Restarting application...");
            performStop();
            performStart();
        }
        // Restart agents in application
        else if ( command.equals("restart:agent") ) {
            if ( started == false )
                return;
            System.out.println("[LOCAL:" + getId() + "] Restarting agents...");

            this.appWaiter.set(Application.MESSAGE_AGENTS_RESTARTED, null, 1);
            this.appWaiter.startWatching();
            performInAgents("restart");
            this.appWaiter.waitForMessages();
        }
        // Enable profiler
        else if ( command.equals("profiler:enable") ) {
            System.out.println("[LOCAL:" + getId() + "] Profiler Enabled");
            profiler.setEnabled(true);
        }
        // Disable profiler
        else if ( command.equals("profiler:disable") ) {
            System.out.println("[LOCAL:" + getId() + "] Profiler Disabled");
            profiler.setEnabled(false);
        }
        // Other commands pass to agents
        else if ( started ){
            performInAgents(command);
        }
    }

    /**
     * Echo value in local instance
     *
     * @param value
     */
    @Override
    public void echo(String value) {
       System.out.println("+----------------------------------------------------------------------------+");
       System.out.printf("| %-74s |\n", value);
       System.out.println("+----------------------------------------------------------------------------+");
    }

    /**
     * Exit local instance
     */
    @Override
    public void exit() {
        if ( profiler != null ) {
            profiler.stop();
            profiler.getProfilerResult().printResult();
        }

        System.out.println("[LOCAL:" + getId() + "] Exit");
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ProfilerResult profiler
     */
    private static class Profiler extends Thread
    {
        private int pid;
        private ProfilerResult profilerResult = new ProfilerResult();
        private boolean enabled = false;
        
        public Profiler(int pid)
        {
            this.pid = pid;
        }

        public ProfilerResult getProfilerResult()
        {
            return profilerResult;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }

        @Override
        public void run()
        {
            while ( true ) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}

                if ( enabled ) {
                    try {
                        Process process = Runtime.getRuntime().exec("scripts/profile.sh " + pid);

                        StringBuilder builder = new StringBuilder();
                        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()) );
                        String line;
                        while ((line = in.readLine()) != null) {
                            builder.append(line + "\n");
                        }
                        in.close();
                        getProfilerResult().add(builder.toString());
                    } catch (IOException e) {}
                }
            }
        }
    }
}
