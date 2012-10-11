package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * Abstract controller test provides a {@link Controller} instance to extending classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractControllerTest extends AbstractDatabaseTest
{
    /**
     * @see Controller
     */
    private cz.cesnet.shongo.controller.Controller controller;

    /**
     * @see ControllerClient
     */
    private ControllerClient controllerClient;

    /**
     * {@link SecurityToken} which is validated in the {@link #controller}
     */
    protected static final SecurityToken TESTING_SECURITY_TOKEN =
            new SecurityToken("18eea565098d4620d398494b111cb87067a3b6b9");

    /**
     * @return {@link #controllerClient}
     */
    public ControllerClient getControllerClient()
    {
        return controllerClient;
    }

    /**
     * Init the {@code controller}.
     *
     * @param controller to be inited
     */
    protected abstract void onInitController(Controller controller);

    /**
     * Controller client is ready.
     */
    protected void onControllerClientReady(ControllerClient controllerClient)
    {
    }

    @Override
    public void before() throws Exception
    {
        super.before();

        // Change XML-RPC port
        System.setProperty(Configuration.RPC_PORT, "8484");

        // Start controller
        controller = new cz.cesnet.shongo.controller.Controller();
        controller.setDomain("cz.cesnet", "CESNET, z.s.p.o.");
        controller.setEntityManagerFactory(getEntityManagerFactory());

        onInitController(controller);

        controller.start();
        controller.startRpc();
        controller.getAuthorization().setTestingAccessToken(TESTING_SECURITY_TOKEN.getAccessToken());

        // Start client
        controllerClient = new ControllerClient(controller.getRpcHost(), controller.getRpcPort());

        onControllerClientReady(controllerClient);
    }

    @Override
    public void after()
    {
        controller.stop();

        super.after();
    }
}
