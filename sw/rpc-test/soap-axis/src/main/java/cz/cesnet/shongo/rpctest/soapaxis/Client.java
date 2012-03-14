package cz.cesnet.shongo.rpctest.soapaxis;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;

import javax.xml.namespace.QName;
import java.util.LinkedHashMap;
import java.util.Map;

public class Client
{
    public static void main(String[] args)
    {
        soapRequest("getMessage", null);

        String result = soapRequest("add", new LinkedHashMap<String, String>() {{
            put("x", "20");
            put("y", "30");
        }});
        System.out.println(result);
    }

    public static String soapRequest(String method, Map<String, String> arguments)
    {
        EndpointReference endpoint = new EndpointReference("http://127.0.0.1:" + (Server.port) + "/axis2/services/API/" + method);
        try {
            OMFactory fac = OMAbstractFactory.getOMFactory();
            OMNamespace ns = fac.createOMNamespace("http://cesnet.cz/shongo/rpc-test", "ns");
            OMElement payload = fac.createOMElement(method, ns);

            if ( arguments != null ) {
                for ( Map.Entry<String, String> entry : arguments.entrySet() ) {
                    OMElement argument = fac.createOMElement(entry.getKey(), ns);
                    argument.setText(entry.getValue());
                    payload.addChild(argument);
                }
            }

            Options options = new Options();
            ServiceClient client = new ServiceClient();
            options.setTo(endpoint);
            client.setOptions(options);
            OMElement result = client.sendReceive(payload);
            result = (OMElement)result.getChildrenWithName(new QName("return")).next();
            return result.getText();
        } catch (AxisFault axisFault) {
            System.err.println(axisFault.getDetail().getText());
        }
        return "Failed";
    }

}
