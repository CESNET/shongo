package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.util.ClassHelper;
import jade.content.onto.*;
import jade.content.schema.ObjectSchema;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        super(NAME, new Ontology[]{BasicOntology.getInstance(), SerializableOntology.getInstance()});

        try {
            // add Java classes to be serializable
            ObjectSchema serializableSchema = getSchema(SerializableOntology.SERIALIZABLE);
            SerializableOntology.getInstance().add(serializableSchema, java.util.Map.class);
            SerializableOntology.getInstance().add(serializableSchema, java.util.HashMap.class);
            SerializableOntology.getInstance().add(serializableSchema, DateTime.class);

            // add commands within this package
            add(getClass().getPackage().getName());

            // add any API classes
            for (String packageName : ClassHelper.getPackages()) {
                add(packageName);
            }
        }
        catch (OntologyException e) {
            logger.error("Error creating the ontology.", e);
        }
    }
}
