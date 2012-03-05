package cz.cesnet.shongo.measurement.fuse;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jms.endpoints.DestinationChooser;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

public class JmsDestinationChooser implements DestinationChooser
{
    @Override
    public Object chooseDestination(MessageExchange messageExchange, Object o)
    {
        NormalizedMessage message = messageExchange.getMessage("in");
        StringSource messageContent = (StringSource)message.getContent();
        AgentService.Message values = new AgentService.Message(messageContent.getText());
        return values.to;
    }
}