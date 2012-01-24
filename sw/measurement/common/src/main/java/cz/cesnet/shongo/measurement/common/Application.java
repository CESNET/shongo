package cz.cesnet.shongo.measurement.common;

import java.io.IOException;

public class Application {

    /**
     * Input stream connector
     */
    static StreamConnector streamConnectorInput = new StreamConnector(System.in);

    /**
     * Run new process from some class that has implemented main() function.
     * All output and errors from process will be printed to standard/error output.
     *
     * @param name      Process name
     * @param mainClass Process main class
     * @param arguments Process arguments
     * @return void
     */
    public static Process runProcess(final String name, final Class mainClass, final String[] arguments) {

        // Run process
        Process process = null;
        try {
            String classPath = "-Djava.class.path=" + System.getProperty("java.class.path");
            StringBuilder command = new StringBuilder();
            command.append("java ");
            command.append(classPath);
            command.append(" ");
            command.append(mainClass.getName());
            if ( arguments != null ) {
                for ( String argument : arguments ) {
                    command.append(" ");
                    command.append(argument);
                }
            }
            process = Runtime.getRuntime().exec(command.toString());
        } catch (IOException e) {
        }

        // Print standard output
        StreamConnector streamConnectorOutput = new StreamConnector(process.getInputStream(), System.out, name);
        streamConnectorOutput.start();

        // Print error output
        StreamConnector streamConnectorError = new StreamConnector(process.getErrorStream(), System.err, name);
        streamConnectorError.start();

        // Print standard output
        streamConnectorInput.addOutput(process.getOutputStream());
        streamConnectorInput.start();

        return process;
    }

}
