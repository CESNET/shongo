package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.jade.AbstractOntology;
import cz.cesnet.shongo.api.ClassHelper;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ControllerOntology extends AbstractOntology
{
    private static Logger logger = LoggerFactory.getLogger(ControllerOntology.class);

    /**
     * Singleton of {@link ControllerOntology}.
     */
    private static Ontology instance = new ControllerOntology();

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
    private ControllerOntology()
    {
        super("shongo-ontology-controller");

        try {
            add(GetUserInformation.class);
            add(GetRoom.class);
            add(NotifyTarget.class);
            add(GetRecordingFolderId.class);

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
