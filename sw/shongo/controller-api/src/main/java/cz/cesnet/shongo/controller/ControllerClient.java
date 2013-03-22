package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.rpc.RpcClient;
import cz.cesnet.shongo.controller.api.FaultSet;

/**
 * Client for a domain controller from Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerClient extends RpcClient
{
    /**
     * Constructor.
     */
    public ControllerClient()
    {
        setFaultSet(new FaultSet());
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
        setFaultSet(new FaultSet());
    }
}
