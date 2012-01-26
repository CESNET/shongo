package cz.cesnet.shongo.measurement.jade;
import cz.cesnet.shongo.measurement.common.StreamConnector;
import org.apache.commons.cli.*;

import java.io.IOException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeApplication {
    public static void main(String[] args) {
        /*Option help = new Option("h", "help", false, "Print this usage information");
        Option agent = OptionBuilder.withLongOpt("agent")
                .withArgName("name")
                .hasArg()
                .withDescription("Run agent")
                .create("a");
        Option agentCount = OptionBuilder.withLongOpt("count")
                .withArgName("count")
                .hasArg()
                .withDescription("Number of agents")
                .create("c");
        Option agentType = OptionBuilder.withLongOpt("type")
                .withArgName("type")
                .hasArg()
                .withDescription("Type of agents")
                .create("t");
        Option domainOption = OptionBuilder.withLongOpt("domain")
                .withArgName("domain")
                .hasArg()
                .withDescription("Domain to connect to")
                .create("d");

        // Create options
        Options options = new Options();
        options.addOption(help);
        options.addOption(agent);
        options.addOption(agentCount);
        options.addOption(agentType);
        options.addOption(domainOption);

        // Parse command line
        CommandLine commandLine = null;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, args);
        } catch ( ParseException e ) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // Print help
        if ( commandLine.hasOption("help") || commandLine.getOptions().length == 0 ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("jade", options);
            System.exit(0);
        }
        
        if (commandLine.hasOption("domain")) {
            // create a container connecting to a domain

        }
        else {
            // create a new domain
            int port = 1099;
            Profile mainProfile = new ProfileImpl(null, port, null);
            AgentContainer main = jade.core.Runtime.instance().createMainContainer(mainProf);

        }

        // Create agent
        if ( commandLine.hasOption("agent") ) {
            String agentName = commandLine.getOptionValue("agent");
            Class agentClass = Agent.class;

            // Get agent type
            if ( commandLine.hasOption("type") ) {
                String type = commandLine.getOptionValue("type");
                if ( type.equals("sender") )
                    agentClass = AgentSender.class;
                else if ( type.equals("receiver") )
                    agentClass = AgentReceiver.class;
                else
                    assert(false);
            }

            // Get agent number
            int number = 1;
            StringBuilder numberFormat = new StringBuilder();
            if ( commandLine.hasOption("count") ) {
                number = Integer.parseInt(commandLine.getOptionValue("count"));
                int places = (int)Math.round(Math.log10(number) + 1.0);
                for ( int index = 0; index < places; index++ )
                    numberFormat.append("0");
            }

            if ( number == 1 ) {
                Agent.runAgent(agentName, agentClass);
            } else {
                for ( int index = 0; index < number; index++ ) {
                    String agentNumber = new java.text.DecimalFormat(numberFormat.toString()).format(index + 1);
                    final String[] arguments = {agentName + agentNumber, agentClass.getName()};
                    cz.cesnet.shongo.measurement.common.JadeApplication.runProcess(agentName + agentNumber, Agent.class, arguments);
                }
            }
        }  */
    }

}
