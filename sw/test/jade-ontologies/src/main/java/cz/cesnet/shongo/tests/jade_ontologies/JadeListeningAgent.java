package cz.cesnet.shongo.tests.jade_ontologies;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;

/**
 * An abstract implementation of a Jade agent running a separate thread for listening the Jade messages.
 * <p/>
 * A normal Jade agent would block after starting. Extending this class instead of the base Agent class, the agent
 * does not block and is able to process user commands or other stuff.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeListeningAgent extends Agent
{
    private Thread listeningThread;

    private Behaviour listeningBehaviour;

    public JadeListeningAgent(Behaviour listeningBehaviour)
    {
        this.listeningBehaviour = listeningBehaviour;
    }


    @Override
    protected void setup()
    {
        // wrap the behaviour in a separate thread commands
        ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
        tbf.wrap(listeningBehaviour);

        // keep the listening thread to be able to interrupt it on exit
        listeningThread = tbf.getThread(listeningBehaviour);
    }

    @Override
    public void doDelete()
    {
        super.doDelete();
        listeningThread.interrupt();
    }

}
