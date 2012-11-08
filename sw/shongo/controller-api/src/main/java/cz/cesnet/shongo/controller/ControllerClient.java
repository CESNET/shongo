package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.api.xmlrpc.RpcClient;

/**
 * Client for a domain controller from Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerClient extends RpcClient
{
    public ControllerClient()
    {
        setFault(new ControllerFault());
    }

    /**
     * Constructor. Automatically perform {@link #connect(String, int)}.
     *
     * @param host
     * @param port
     * @throws Exception
     */
    public ControllerClient(String host, int port) throws Exception
    {
        super(host, port);
        setFault(new ControllerFault());
    }
}
