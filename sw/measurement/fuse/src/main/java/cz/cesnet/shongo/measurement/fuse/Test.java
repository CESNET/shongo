package cz.cesnet.shongo.measurement.fuse;

import cz.cesnet.shongo.measurement.common.ActiveMq;
import org.apache.activemq.broker.BrokerService;
import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import javax.jbi.JBIException;

public class Test {

    public static void main(String[] args)
    {
        String agentName = "Agent2";
        System.getProperties().put("jms.url", "tcp://localhost:61616");
        System.getProperties().put("jms.queue", agentName);

        BrokerService activeMqServer = ActiveMq.createActiveMqServer();
        activeMqServer.waitUntilStarted();

        AbstractApplicationContext context = new ClassPathXmlApplicationContext("servicemix.xml");
        SpringJBIContainer container = (SpringJBIContainer) context.getBean("jbi");

        ActiveMq.Client client = ActiveMq.createActiveMqClient();
        client.sendMessage(agentName, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><message>Hello</message>");
        client.sendMessage(agentName, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><message>Hello3</message>");

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        String message = client.receiveMessage("Server");
        System.out.println("Server: Received message: " + message);
        String message2 = client.receiveMessage("Server");
        System.out.println("Server: Received message: " + message2);

        client.close();

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        try {
            container.shutDown();
        } catch (JBIException e) {
            e.printStackTrace();
        }
        context.destroy();

        try {
            activeMqServer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
