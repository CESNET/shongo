package cz.cesnet.shongo.measurement.mule;

import cz.cesnet.shongo.measurement.common.Agent;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class MuleAgent extends Agent implements MessageListener
{
    /** ActiveMQ address */
    private String activeMqAdress;

    /**
     * Create Fuse agent
     *
     * @param id   Agent id
     * @param name Agent name
     */
    public MuleAgent(String id, String name)
    {
        super(id, name);
    }

    /**
     * Implementation of Fuse agent startup
     *
     * @return result
     */
    @Override
    protected boolean startImpl()
    {
        logger.info("Started MULE agent [" + getName() + "] at [" + activeMqAdress +"]");
        return true;
    }

    /**
     * Implementation of Fuse agent finalization
     */
    @Override
    protected void stopImpl()
    {
        logger.info("Stopping MULE agent [" + getName() + "]");
    }

    /**
     * Implementation of Fuse agent send message
     *
     * @param receiverName
     * @param message
     */
    @Override
    protected void sendMessageImpl(String receiverName, String message)
    {
    }

    /**
     * Process passed arguments to agent
     *
     * @param arguments
     */
    @Override
    protected void onProcessArguments(String[] arguments)
    {
        activeMqAdress = arguments[0];
    }

    /**
     * Through this method onReceiveMessage is invoked  for generic agent
     *
     * @param message
     */
    @Override
    public void onMessage(Message message)
    {
        try {
            // Invoke receive message event
            this.onReceiveMessage(
                    message.getStringProperty("from"),
                    message.getStringProperty("text")
            );
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
