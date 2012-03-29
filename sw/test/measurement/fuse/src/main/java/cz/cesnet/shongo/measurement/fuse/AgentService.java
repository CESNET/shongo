package cz.cesnet.shongo.measurement.fuse;

import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.bean.Destination;
import org.apache.servicemix.bean.ExchangeTarget;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.*;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

public class AgentService implements MessageExchangeListener {

    @Resource
    private DeliveryChannel channel;

    @Resource
    ComponentContext context;

    private FuseAgent agent;

    public void setAgent(FuseAgent agent) {
        this.agent = agent;
    }

    @Override
    public void onMessageExchange(MessageExchange messageExchange) throws MessagingException {
        NormalizedMessage message = messageExchange.getMessage("in");
        StringSource messageContent = (StringSource)message.getContent();

        assert(agent != null);
        Message values = new Message(messageContent.getText());
        agent.onReceiveMessage(values.from, values.content);

        messageExchange.setStatus(ExchangeStatus.DONE);
        channel.send(messageExchange);
    }

    public static class Message
    {
        public String from;
        public String to;
        public String content;
        
        Message(String from, String to, String content)
        {
            this.from = from;
            this.to = to;
            this.content = content;
        }

        Message(String xml)
        {
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
                Element root = doc.getDocumentElement();
                from = root.getElementsByTagName("from").item(0).getTextContent();
                to = root.getElementsByTagName("to").item(0).getTextContent();
                content = root.getElementsByTagName("content").item(0).getTextContent();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            builder.append("<message>");
            builder.append("<from>" + from + "</from>");
            builder.append("<to>" + to + "</to>");
            builder.append("<content>" + content + "</content>");
            builder.append("</message>");
            return builder.toString();
        }
    }

}
