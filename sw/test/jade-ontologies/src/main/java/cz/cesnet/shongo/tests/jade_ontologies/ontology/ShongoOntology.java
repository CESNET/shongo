package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.onto.BasicOntology;
import jade.content.onto.CFReflectiveIntrospector;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.AgentActionSchema;
import jade.content.schema.PrimitiveSchema;
import jade.tools.introspector.Introspector;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ShongoOntology extends Ontology
{
    // ontology name
    public static final String NAME = "shongo-ontology";

    // VOCABULARY
    // concepts

    // actions
    public static final String MUTE = "MUTE";

    public static final String UNMUTE = "UNMUTE";

    public static final String SET_MICROPHONE_LEVEL = "SET-MICROPHONE-LEVEL";
    public static final String SET_MICROPHONE_LEVEL_LEVEL = "level";

    // predicates


    // singleton implementation
    private static Ontology theInstance = new ShongoOntology();

    public static Ontology getInstance()
    {
        return theInstance;
    }

    private ShongoOntology()
    {
        // passing the introspector to use java.util collections instead of the ones from Jade
        // see the book Developing Multi-Agent Systems with Jade, p. 86 for discussion
        super(NAME, BasicOntology.getInstance(), new CFReflectiveIntrospector());

        try {
            add(new AgentActionSchema(MUTE), Mute.class);
            add(new AgentActionSchema(UNMUTE), Unmute.class);
            add(new AgentActionSchema(SET_MICROPHONE_LEVEL), SetMicrophoneLevel.class);

            AgentActionSchema as = (AgentActionSchema) getSchema(SET_MICROPHONE_LEVEL);
            as.add(SET_MICROPHONE_LEVEL_LEVEL, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
        }
        catch (OntologyException ex) {
            ex.printStackTrace();
        }
    }
}
