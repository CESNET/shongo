package cz.cesnet.shongo.connector.api.ontology;

import cz.cesnet.shongo.api.jade.AbstractOntology;
import cz.cesnet.shongo.api.jade.CommonOntology;
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

    private ConnectorOntology()
    {
        super("shongo-ontology-connector");

        try {
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
        catch (BeanOntologyException exception) {
            logger.error("Creating the ontology failed.", exception);
        }
    }
}
