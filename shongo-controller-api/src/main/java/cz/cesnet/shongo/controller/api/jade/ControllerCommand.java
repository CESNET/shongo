package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.Command;
import jade.content.onto.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common ancestor for all controller API {@link Command}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ControllerCommand extends Command
{
    protected static Logger logger = LoggerFactory.getLogger(ControllerCommand.class);

    /**
     * Executes the {@link ControllerCommand}.
     *
     * @param commonService
     * @param senderAgentName
     * @return the result of the {@link ControllerCommand} (should be a concept or a Java class
     *         encapsulating a primitive type, e.g., Integer, ...),
     *         or NULL if it does not return anything
     * @throws cz.cesnet.shongo.api.jade.CommandException
     *
     */
    public abstract Object execute(Service commonService, String senderAgentName) throws CommandException;

    @Override
    public Ontology getOntology()
    {
        return ControllerOntology.getInstance();
    }
}
