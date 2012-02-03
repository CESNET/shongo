package cz.cesnet.shongo.measurement.fuse;

import cz.cesnet.shongo.measurement.common.Agent;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class FuseAgent extends Agent implements MessageListener
{
    /** JMS Connection */
    Connection connection;

    /** JMS Session */
    Session session;

    /** JMS Message consumer */
    MessageConsumer consumer;

    /** ActiveMQ address */
    private String activeMqAdress;

    /**
     * Create Fuse agent
     *
     * @param id   Agent id
     * @param name Agent name
     */
    public FuseAgent(String id, String name)
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
        // Create connection
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://" + activeMqAdress);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            e.printStackTrace();
            logger.error("Failed to start FUSE agent [" + getName() + "] at [" + activeMqAdress +"]");
        }
        if ( connection == null )
            return false;

        // Create consumer
        try {
            Destination destination = session.createQueue(getName());
            consumer = session.createConsumer(destination);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            e.printStackTrace();
        }
        logger.info("Started FUSE agent [" + getName() + "] at ActiveMQ [" + activeMqAdress +"]");
        return true;
    }

    /**
     * Implementation of Fuse agent finalization
     */
    @Override
    protected void stopImpl()
    {
        logger.info("Stopping FUSE agent [" + getName() + "]");
        if ( consumer != null ) {
            try {
                consumer.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
        if ( session != null ) {
            try {
                session.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
        if ( connection != null ) {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
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
        try {

            Destination destination = session.createQueue(receiverName);
            MessageProducer producer = session.createProducer(destination);
            Message producerMessage = session.createMessage();
            producerMessage.setStringProperty("from", getName());
            producerMessage.setStringProperty("text", message);
            producer.send(producerMessage);
        } catch (JMSException e) {
            e.printStackTrace();
        }
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
