package cz.cesnet.shongo.api.jade;

import jade.content.onto.Ontology;

/**
 * {@link Command}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PingCommand extends Command
{
    @Override
    public Ontology getOntology()
    {
        return CommonOntology.getInstance();
    }
}
