package cz.cesnet.shongo.connector.api.jade;

import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.api.jade.AbstractOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
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
            add(packageName + ".multipoint");
            add(packageName + ".recording");

            // Add all API classes
            for (String item : ClassHelper.getPackages()) {
                add(item);
            }
        }
        catch (BeanOntologyException exception) {
            throw new RuntimeException("Creating the ontology failed.", exception);
        }
    }
}
