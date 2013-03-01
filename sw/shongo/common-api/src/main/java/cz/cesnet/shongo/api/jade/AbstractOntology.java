package cz.cesnet.shongo.api.jade;

import cz.cesnet.shongo.api.util.ChangesTracking;
import jade.content.onto.*;
import jade.content.schema.ObjectSchema;
import jade.domain.FIPAAgentManagement.ExceptionOntology;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract {@link Ontology}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractOntology extends CustomBeanOntology
{
    private static Logger logger = LoggerFactory.getLogger(AbstractOntology.class);

    /**
     * Constructor.
     *
     * @param ontologyName
     */
    protected AbstractOntology(String ontologyName)
    {
        super(ontologyName, new Ontology[]{ExceptionOntology.getInstance(), SerializableOntology.getInstance()});

        try {
            // Add Java classes to be serializable to the ontology
            ObjectSchema serializableSchema = getSchema(SerializableOntology.SERIALIZABLE);
            SerializableOntology.getInstance().add(serializableSchema, java.util.Map.class);
            SerializableOntology.getInstance().add(serializableSchema, java.util.HashMap.class);
            SerializableOntology.getInstance().add(serializableSchema, Period.class);
            SerializableOntology.getInstance().add(serializableSchema, DateTime.class);
            SerializableOntology.getInstance().add(serializableSchema, ChangesTracking.class);
        }
        catch (OntologyException exception) {
            logger.error("Creating the ontology failed.", exception);
        }
    }
}
