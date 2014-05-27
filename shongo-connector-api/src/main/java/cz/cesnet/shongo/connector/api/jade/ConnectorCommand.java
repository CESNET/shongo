package cz.cesnet.shongo.connector.api.jade;

import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.*;
import jade.content.onto.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common ancestor for all connector API {@link Command}s
 * <p/>
 * Offers some helper methods common to all {@link ConnectorCommand}s.
 * <p/>
 * Note that all {@link ConnectorCommand}s classes must have the default constructor
 * without any parameters for Jade to be able to construct the objects.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public abstract class ConnectorCommand<T> extends Command
{
    protected static Logger logger = LoggerFactory.getLogger(ConnectorCommand.class);

    /**
     * Executes the {@link ConnectorCommand} on a given connector.
     *
     * @param connector a connector on which the {@link ConnectorCommand} should be executed
     * @return the result of the {@link ConnectorCommand} (should be a concept or a Java class
     *         encapsulating a primitive type, e.g., Integer, ...),
     *         or NULL if it does not return anything
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    public abstract T execute(CommonService connector) throws CommandException, CommandUnsupportedException;

    /**
     * Returns the passed connector as a MonitoringService. Throws an exception if the typecast fails.
     *
     * @param connector a connector
     * @return connector typecast to an MonitoringService
     * @throws CommandUnsupportedException
     */
    protected static MonitoringService getMonitoring(CommonService connector)
            throws CommandException, CommandUnsupportedException
    {
        if (connector == null) {
            throw new CommandException("Not connected to the multipoint.");
        }
        if (!(connector instanceof MonitoringService)) {
            throw new CommandUnsupportedException("The command is implemented only on devices which can be monitored.");
        }
        return (MonitoringService) connector;
    }

    /**
     * Returns the passed connector as an EndpointService. Throws an exception if the typecast fails.
     *
     * @param connector a connector
     * @return connector typecast to an EndpointService
     * @throws CommandUnsupportedException
     */
    protected static EndpointService getEndpoint(CommonService connector)
            throws CommandException, CommandUnsupportedException
    {
        if (connector == null) {
            throw new CommandException("Not connected to the endpoint.");
        }
        if (!(connector instanceof EndpointService)) {
            throw new CommandUnsupportedException("The command is implemented only on an endpoint.");
        }
        return (EndpointService) connector;
    }

    /**
     * Returns the passed connector as a MultipointService. Throws an exception if the typecast fails.
     *
     * @param connector a connector
     * @return connector typecast to an MultipointService
     * @throws CommandUnsupportedException
     */
    protected static MultipointService getMultipoint(CommonService connector)
            throws CommandException, CommandUnsupportedException
    {
        if (connector == null) {
            throw new CommandException("Not connected to the multipoint.");
        }
        if (!(connector instanceof MultipointService)) {
            throw new CommandUnsupportedException("The command is implemented only on a multipoint.");
        }
        return (MultipointService) connector;
    }

    /**
     * Returns the passed connector as a RecordingService. Throws an exception if the typecast fails.
     *
     * @param connector a connector
     * @return connector typecast to an RecordingService
     * @throws CommandUnsupportedException
     */
    protected static RecordingService getRecording(CommonService connector)
            throws CommandException, CommandUnsupportedException
    {
        if (connector == null) {
            throw new CommandException("Not connected to the recording service.");
        }
        if (!(connector instanceof RecordingService)) {
            throw new CommandUnsupportedException("The command is implemented only on a recording service.");
        }
        return (RecordingService) connector;
    }

    @Override
    public Ontology getOntology()
    {
        return ConnectorOntology.getInstance();
    }
}
