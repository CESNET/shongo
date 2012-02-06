package cz.cesnet.shongo.measurement.common;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;

import javax.jms.*;

/**
 * Helper class for manipulating with ActiveMQ server and clients
 *
 * @author Martin Srom
 */
public class ActiveMq {

    /** Default ActiveMQ broker url */
    public static final String BROKER_DEFAULT_URL = "localhost:61616";

    /**
     * Create ActiveMQ server
     *
     * @return server
     */
    public static BrokerService createActiveMqServer()
    {
        return  createServer(BROKER_DEFAULT_URL);
    }

    /**
     * Create ActiveMQ server
     *
     * @param brokerUrl Broker URL
     * @return server
     */
    public static BrokerService createServer(String brokerUrl)
    {
        try {
            BrokerService broker = new BrokerService();
            broker.addConnector("tcp://" + brokerUrl);
            broker.setBrokerName("Broker1");
            broker.setUseJmx(false);
            broker.start();

            broker.waitUntilStarted();

            return broker;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create ActiveMQ client
     *
     * @return client
     */
    public static Client createActiveMqClient()
    {
        return createActiveMqClient(BROKER_DEFAULT_URL);
    }

    /**
     * Create ActiveMQ client
     *
     * @param brokerUrl
     * @return client
     */
    public static Client createActiveMqClient(String brokerUrl)
    {
        Client client = new Client();
        if ( client.connect(brokerUrl) == false ) {
            return null;
        }
        return client;
    }

    /**
     * Class representing ActiveMQ Client
     *
     * @author Martin Srom
     */
    public static class Client
    {
        private static Connection connection;
        private static Session session;

        public boolean connect(String brokerUrl)
        {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://" + brokerUrl);
            try {
                connection = connectionFactory.createConnection();
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            } catch (JMSException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public void sendMessage(String queue, String text)
        {
            try {
                Destination destination = session.createQueue(queue);
                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                TextMessage message = session.createTextMessage(text);
                System.out.println("Sending message: " + message.getText());
                producer.send(message);
                producer.close();
            }
            catch (JMSException e) {
                e.printStackTrace();
            }
        }

        public String receiveMessage(String queue)
        {
            try {
                Destination destination = session.createQueue(queue);
                MessageConsumer consumer = session.createConsumer(destination);
                TextMessage message = (TextMessage)consumer.receive();
                consumer.close();
                return message.getText();
            }
            catch (JMSException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void setMessageListener(String queue, MessageListener messageListener)
        {
            try {
                Destination destination = session.createQueue(queue);
                MessageConsumer consumer = session.createConsumer(destination);
                consumer.setMessageListener(messageListener);
            }
            catch (JMSException e) {
                e.printStackTrace();
            }
        }

        public void close()
        {
            try {
                session.close();
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Run application that creates ActiveMQ server and connects to it by client,
     * which send message to it in loop
     *
     * @param args
     */
    public static void main(String[] args)
    {
        BrokerService server = createActiveMqServer();
        if ( server == null )
            return;

        Client client = createActiveMqClient();
        if ( client == null )
            return;

        /*client.setMessageListener("Queue", new MessageListener() {
            @Override
            public void onMessage(Message message) {
                TextMessage textMessage = (TextMessage)message;
                try {
                    System.out.println("Received message: " + textMessage.getText());
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });*/

        int count = 0;
        while ( true ) {
            count++;
            client.sendMessage("Queue", "Hello" + count);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }

}
