package cz.cesnet.shongo.measurement.mule;

import cz.cesnet.shongo.measurement.common.Agent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;

import javax.jms.*;

public class MuleAgent extends Agent
{
    /** ActiveMQ url */
    private String activeMqUrl;

    /** Mule context */
    private MuleContext muleContext;

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
        try {
            String[] urls = activeMqUrl.split(",");
            StringBuilder failover = new StringBuilder();
            for ( String url : urls ) {
                if ( failover.length() > 0 )
                    failover.append(",");
                failover.append("tcp://" + url);
            }
            
            System.getProperties().put("jms.url", failover.toString());
            System.getProperties().put("jms.queue", getName());

            // Mule default configuration
            DefaultMuleConfiguration muleConfig = new DefaultMuleConfiguration();
            muleConfig.setId("ShongoMeasurementMuleServer:" + getName());

            // Configuration builder
            SpringXmlConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder("mule-config.xml");

            // Context builder
            MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            contextBuilder.setMuleConfiguration(muleConfig);

            // Create context
            MuleContextFactory contextFactory = new DefaultMuleContextFactory();
            muleContext = contextFactory.createMuleContext(configBuilder, contextBuilder);

            muleContext.getRegistry().registerObject("agent", this);

            // Start mule
            muleContext.start();
            logInfo("Started MULE agent [" + getName() + "] at ActiveMQ [" + activeMqUrl +"]");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Implementation of Fuse agent finalization
     */
    @Override
    protected void stopImpl()
    {
        try {
            logInfo("Stopping MULE agent [" + getName() + "]");
            muleContext.stop();
            muleContext.dispose();
        } catch (MuleException e) {
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
        try {
            MuleMessage message = new DefaultMuleMessage(messageText, muleContext);
            message.setOutboundProperty("from", getName());
            message.setOutboundProperty("to", receiverName);
            muleContext.getClient().dispatch("jms-output", message);
        } catch (MuleException e) {
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
}
