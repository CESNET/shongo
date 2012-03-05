package cz.cesnet.shongo.measurement.common;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.network.DiscoveryNetworkConnector;
import org.apache.activemq.network.NetworkConnector;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.File;
import java.net.URI;

/**
 * Helper class for manipulating with ActiveMQ server and clients
 *
 * @author Martin Srom
 */
public class ActiveMq {

    /** Logger */
    static protected Logger logger = Logger.getLogger(Agent.class);

    /** Default ActiveMQ broker url */
    public static final String BROKER_DEFAULT_URL = "localhost:61616";

    /**
     * Create ActiveMQ server
     *
     * @return server
     */
    public static BrokerService createServer()
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
        String[] urls = brokerUrl.split(",");
        String serverUrl = urls[0];
        String dataPath = "activemq-data/" + serverUrl;

        logger.info("Starting ActiveMQ Server at [" + serverUrl +"]");

        try {
            FileUtils.deleteDirectory(new File(dataPath));

            BrokerService broker = new BrokerService();
            broker.addConnector("tcp://" + serverUrl);
            broker.setDataDirectory(dataPath);
            broker.setUseJmx(true);
            // Connect to other brokers
            if ( urls.length > 1 ) {
                StringBuilder network = new StringBuilder();
                for ( int index = 1; index < urls.length; index++ ) {
                    if ( network.length() > 0 )
                        network.append(",");
                    network.append("tcp://" + urls[index]);
                }
                logger.info("Connecting ActiveMQ Server to [" + network.toString() +"]");
                NetworkConnector networkConnector = new DiscoveryNetworkConnector(new URI("static:(" + network.toString() + ")"));
                networkConnector.setDuplex(true);
                broker.addNetworkConnector(networkConnector);
            }

            // Start
            broker.start();
            broker.waitUntilStarted();

            return broker;
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("ActiveMQ Server failed to start at [" + serverUrl +"]");

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
        BrokerService server = createServer();
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
