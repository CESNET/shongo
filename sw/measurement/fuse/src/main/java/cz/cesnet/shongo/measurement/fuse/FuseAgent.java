package cz.cesnet.shongo.measurement.fuse;

import cz.cesnet.shongo.measurement.common.Agent;
import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.servicemix.jbi.framework.ComponentContextImpl;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import javax.jbi.JBIException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jms.*;
import javax.xml.namespace.QName;

public class FuseAgent extends Agent implements MessageListener
{
    /** Application Context */
    AbstractApplicationContext context;

    /** Fuse-ESB container */
    SpringJBIContainer container;

    /** ActiveMQ address */
    private String activeMqUrl;

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
        String[] urls = activeMqUrl.split(",");
        StringBuilder failover = new StringBuilder();
        for ( String url : urls ) {
            if ( failover.length() > 0 )
                failover.append(",");
            failover.append("tcp://" + url);
        }

        System.getProperties().put("jms.url", failover.toString());
        System.getProperties().put("jms.queue", getName());

        context = new ClassPathXmlApplicationContext("servicemix.xml");
        container = (SpringJBIContainer) context.getBean("jbi");

        AgentService agentService = (AgentService)context.getBean("agentService");
        agentService.setAgent(this);

        logInfo("Started FUSE agent [" + getName() + "] at ActiveMQ [" + activeMqUrl +"]");
        return true;
    }

    /**
     * Implementation of Fuse agent finalization
     */
    @Override
    protected void stopImpl()
    {
        logInfo("Stopping FUSE agent [" + getName() + "]");
        try {
            container.shutDown();
        } catch (JBIException e) {
            e.printStackTrace();
        }
    }

    /**
     * Implementation of Fuse agent send message
     *
     * @param receiverName
     * @param messageText
     */
    @Override
    protected void sendMessageImpl(String receiverName, String messageText)
    {
        ComponentContextImpl componentContext = container.getComponent("servicemix-jms").getContext();
        try {
            ServiceEndpoint endpoint = componentContext.getEndpoint(new QName("jms-output"), "endpoint");
            assert(endpoint != null);
            DeliveryChannel channel = componentContext.getDeliveryChannel();
            InOnly exchange = exchange = channel.createExchangeFactory(endpoint).createInOnlyExchange();
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(new AgentService.Message(getName(), receiverName, messageText).toString()));
            exchange.setService(new QName("http://cesnet.cz/shongo/measurement", "jms-output"));
            exchange.setMessage(message, "in");
            channel.sendSync(exchange);
        } catch (MessagingException e) {
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
        activeMqUrl = arguments[0];
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
