package cz.cesnet.shongo.measurement.mule;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

/**
 * Mule Agent Service
 *
 * @author Martin Srom
 */
public class AgentService implements Callable {

    public AgentService()
    {
        System.out.println("AgentService initializing!");
    }

    @Override
    public Object onCall(MuleEventContext muleEventContext) throws Exception {
        MuleMessage message = muleEventContext.getMessage();
        String from = message.getInboundProperty("from");
        String text = message.getPayloadAsString();
        System.out.println("Agent: Received message [" + text + "] from [" + from + "]!");

        message = new DefaultMuleMessage("Answer to [" + text + "]", muleEventContext.getMuleContext());
        message.setPayload("Answer to [" + text + "]");
        message.setOutboundProperty("from", "Agent");
        message.setOutboundProperty("to", from);

        System.out.println("Agent: Send answer message [" + message.getPayloadAsString() + "] to [" + message.getOutboundProperty("to") + "]");
        muleEventContext.dispatchEvent(message, "jms-output");
        
        return null;
    }
}
