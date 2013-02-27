package cz.cesnet.shongo.connector.api.jade;

import cz.cesnet.shongo.api.jade.AbstractOntology;
import cz.cesnet.shongo.api.util.ClassHelper;
import jade.content.onto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ConnectorOntology extends AbstractOntology
{
    private static Logger logger = LoggerFactory.getLogger(ConnectorOntology.class);

    /**
     * Singleton of {@link ConnectorOntology}.
     */
    private static Ontology instance = new ConnectorOntology();

    /**
     * @return {@link #instance}
     */
    public static Ontology getInstance()
    {
        return instance;
    }

    /**
     * Constructor.
     */
    private ConnectorOntology()
    {
        super("shongo-ontology-connector");

        try {
            // add commands within this package
            String packageName = getClass().getPackage().getName();
            add(packageName);
            add(packageName + ".common");
            add(packageName + ".endpoint");
            add(packageName + ".multipoint.io");
            add(packageName + ".multipoint.rooms");
            add(packageName + ".multipoint.users");

            // Add all API classes
            for (String item : ClassHelper.getPackages()) {
                add(item);
            }
        }
        catch (BeanOntologyException exception) {
            logger.error("Creating the ontology failed.", exception);
        }
    }
}