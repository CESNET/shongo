package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.CommandException;
import jade.content.onto.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common ancestor for all controller API {@link cz.cesnet.shongo.api.jade.AgentAction}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ControllerAgentAction extends cz.cesnet.shongo.api.jade.AgentAction
{
    protected static Logger logger = LoggerFactory.getLogger(ControllerAgentAction.class);

    /**
     * Executes the controller agent action.
     *
     * @param commonService
     * @return the result of the action, or NULL if the action does not return anything; should be a concept or a Java
     *         class encapsulating a primitive type (e.g., Integer, ...)
     * @throws cz.cesnet.shongo.api.CommandException
     */
    public abstract Object execute(Service commonService) throws CommandException;

    @Override
    public Ontology getOntology()
    {
        return ControllerOntology.getInstance();
    }
}
