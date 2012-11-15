package cz.cesnet.shongo.connector.api.ontology;

import cz.cesnet.shongo.api.util.ChangesTracking;
import cz.cesnet.shongo.api.util.ClassHelper;
import jade.content.onto.*;
import jade.content.schema.ObjectSchema;
import jade.domain.FIPAAgentManagement.ExceptionOntology;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ConnectorOntology extends BeanOntology
{
    private static final String NAME = "shongo-ontology";

    private static Logger logger = LoggerFactory.getLogger(ConnectorOntology.class);

    // singleton implementation
    private static Ontology theInstance = new ConnectorOntology();

    public static Ontology getInstance()
    {
        return theInstance;
    }

    private ConnectorOntology()
    {
        super(NAME, new Ontology[]{ExceptionOntology.getInstance(), SerializableOntology.getInstance()});

        try {
            // add some Java classes to be serializable to the ontology
            ObjectSchema serializableSchema = getSchema(SerializableOntology.SERIALIZABLE);
            SerializableOntology.getInstance().add(serializableSchema, java.util.Map.class);
            SerializableOntology.getInstance().add(serializableSchema, java.util.HashMap.class);
            SerializableOntology.getInstance().add(serializableSchema, Period.class);
            SerializableOntology.getInstance().add(serializableSchema, DateTime.class);
            SerializableOntology.getInstance().add(serializableSchema, ChangesTracking.class);

            // add commands within this package
            String packageName = getClass().getPackage().getName();
            add(packageName);
            add(packageName + ".actions.common");
            add(packageName + ".actions.endpoint");
            add(packageName + ".actions.multipoint.io");
            add(packageName + ".actions.multipoint.rooms");
            add(packageName + ".actions.multipoint.users");

            // add any API classes
            for (String item : ClassHelper.getPackages()) {
                add(item);
            }
        }
        catch (OntologyException e) {
            logger.error("Error creating the ontology.", e);
        }
    }
}
