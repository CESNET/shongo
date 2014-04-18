package cz.cesnet.shongo.jade;

import jade.core.behaviours.DataStore;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ParallelResponderBehaviour extends ParallelBehaviour implements FIPANames.InteractionProtocol
{
    /**
     * {@link MessageTemplate} for matching requests.
     */
    private MessageTemplate messageTemplate;

    /**
     * Constructor.
     *
     * @param agent           to which the behaviour is added
     * @param messageTemplate sets the {@link #messageTemplate}
     */
    public ParallelResponderBehaviour(jade.core.Agent agent, MessageTemplate messageTemplate)
    {
        super(agent, WHEN_ALL);

        this.messageTemplate = messageTemplate;

        setDataStore(new DataStore());
        addSubBehaviour(new MessageReceiver());
    }

    /**
     * Handle given {@code requestMessage}.
     *
     * @param requestMessage
     */
    protected abstract void handleRequest(ACLMessage requestMessage);

    /**
     * Message receiver.
     */
    private class MessageReceiver extends SimpleBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage message = myAgent.receive(messageTemplate);
            if (message != null) {
                handleRequest(message);
            }
            else {
                block();
            }
        }

        @Override
        public boolean done()
        {
            return false;
        }
    }
}
