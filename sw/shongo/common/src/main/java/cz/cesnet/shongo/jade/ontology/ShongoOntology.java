package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.util.ClassHelper;
import jade.content.onto.*;
import jade.content.schema.ObjectSchema;
import jade.domain.FIPAAgentManagement.ExceptionOntology;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ShongoOntology extends BeanOntology
{
    private static final String NAME = "shongo-ontology";

    private static Logger logger = LoggerFactory.getLogger(ShongoOntology.class);

    // singleton implementation
    private static Ontology theInstance = new ShongoOntology();

    public static Ontology getInstance()
    {
        return theInstance;
    }

    private ShongoOntology()
    {
        super(NAME, new Ontology[]{ExceptionOntology.getInstance(), SerializableOntology.getInstance()});

        try {
            // add some Java classes to be serializable to the ontology
            ObjectSchema serializableSchema = getSchema(SerializableOntology.SERIALIZABLE);
            SerializableOntology.getInstance().add(serializableSchema, java.util.Map.class);
            SerializableOntology.getInstance().add(serializableSchema, java.util.HashMap.class);
            SerializableOntology.getInstance().add(serializableSchema, DateTime.class);

            // add commands within this package
            String packageName = getClass().getPackage().getName();
            add(packageName);

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
