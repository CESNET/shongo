package cz.cesnet.shongo.api.jade;

import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common ontology for all JADE agents.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonOntology extends AbstractOntology
{
    private static Logger logger = LoggerFactory.getLogger(CommonOntology.class);

    /**
     * Singleton of {@link CommonOntology}.
     */
    private static Ontology instance = new CommonOntology();

    /**
     * @return {@link #instance}
     */
    public static Ontology getInstance()
    {
        return instance;
    }

    private CommonOntology()
    {
        super("shongo-ontology-common");

        try {
            add(PingCommand.class);
        }
        catch (OntologyException exception) {
            throw new RuntimeException("Creating the ontology failed.", exception);
        }
    }
}
