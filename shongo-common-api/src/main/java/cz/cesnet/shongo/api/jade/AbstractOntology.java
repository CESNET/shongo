package cz.cesnet.shongo.api.jade;

import cz.cesnet.shongo.api.util.DeviceAddress;
import jade.content.onto.CustomBeanOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.SerializableOntology;
import jade.content.schema.ObjectSchema;
import jade.domain.FIPAAgentManagement.ExceptionOntology;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.Period;

/**
 * Abstract {@link Ontology}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractOntology extends CustomBeanOntology
{
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
            SerializableOntology.getInstance().add(serializableSchema, Duration.class);
            SerializableOntology.getInstance().add(serializableSchema, DateTime.class);
            SerializableOntology.getInstance().add(serializableSchema, LocalDate.class);
            SerializableOntology.getInstance().add(serializableSchema, DeviceAddress.class);
        }
        catch (OntologyException exception) {
            throw new RuntimeException("Creating the ontology failed.", exception);
        }
    }
}
