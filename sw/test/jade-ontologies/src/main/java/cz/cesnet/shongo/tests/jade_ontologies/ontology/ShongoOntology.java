package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.onto.*;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ShongoOntology extends BeanOntology
{
    public static final String NAME = "shongo-ontology";

    // singleton implementation
    private static Ontology theInstance = new ShongoOntology();

    public static Ontology getInstance()
    {
        return theInstance;
    }

    private ShongoOntology()
    {
        super(NAME);

        try {
            add(getClass().getPackage().getName());
        }
        catch (BeanOntologyException ex) {
            ex.printStackTrace();
        }
    }
}
