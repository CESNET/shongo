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

    @Override
    public Object onCall(MuleEventContext muleEventContext) throws Exception {
        MuleMessage message = muleEventContext.getMessage();
        String from = message.getInboundProperty("from");
        String text = message.getPayloadAsString();

        MuleAgent agent = (MuleAgent)muleEventContext.getMuleContext().getRegistry().lookupObject("agent");
        agent.onReceiveMessage(from, text);
        
        return null;
    }
}
