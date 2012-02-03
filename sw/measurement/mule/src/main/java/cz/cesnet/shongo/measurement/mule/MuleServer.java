package cz.cesnet.shongo.measurement.mule;

import cz.cesnet.shongo.measurement.common.ActiveMq;
import org.mule.DefaultMuleMessage;
import org.mule.api.*;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;

public class MuleServer {

    public static void main(String[] args)
    {
        String activeMqUrl = "tcp://localhost:61616";

        ActiveMq.createActiveMqServer(activeMqUrl);

        System.getProperties().put("jms.url", activeMqUrl);
        System.getProperties().put("jms.queue", "Queue");

        try {

            // Mule default configuration
            DefaultMuleConfiguration muleConfig = new DefaultMuleConfiguration();
            muleConfig.setId("ShongoMeasurement");

            // Configuration builder
            SpringXmlConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder("mule-config.xml");

            // Context builder
            MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            contextBuilder.setMuleConfiguration(muleConfig);

            // Create context
            MuleContextFactory contextFactory = new DefaultMuleContextFactory();
            MuleContext muleContext = contextFactory.createMuleContext(configBuilder, contextBuilder);

            // Start mule
            muleContext.start();
            
            String name = "MuleServer";

            // Send messages
            int count = 0;
            while ( true ) {
                count++;

                MuleClient client = muleContext.getClient();

                MuleMessage message = new DefaultMuleMessage("Message" + count, muleContext);
                message.setOutboundProperty("from", name);
                message.setOutboundProperty("to", "Agent");
                client.dispatch("jms-input", message);
                System.out.println(name + ": Send message [" + message.getPayloadAsString() + "]");

                message = client.request("jms://MuleServer", 1000);
                if ( message != null ) {
                    String from = message.getInboundProperty("from");
                    String text = message.getPayloadAsString();
                    System.out.println(name + ": Received message [" + text + "] from [" + from + "]");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
