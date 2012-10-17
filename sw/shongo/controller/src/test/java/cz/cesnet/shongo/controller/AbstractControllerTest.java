package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Abstract controller test provides a {@link Controller} instance to extending classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractControllerTest extends AbstractDatabaseTest
{
    /**
     * {@link SecurityToken} which is validated in the {@link #controller}
     */
    protected static final SecurityToken SECURITY_TOKEN =
            new SecurityToken("18eea565098d4620d398494b111cb87067a3b6b9");

    /**
     * @see Controller
     */
    private cz.cesnet.shongo.controller.Controller controller;

    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see ControllerClient
     */
    private ControllerClient controllerClient;

    /**
     * @return {@link #controllerClient}
     */
    public ControllerClient getControllerClient()
    {
        return controllerClient;
    }

    /**
     * @return {@link ResourceService} from the {@link #controllerClient}
     */
    public ResourceService getResourceService()
    {
        return controllerClient.getService(ResourceService.class);
    }

    /**
     * @return {@link ReservationService} from the {@link #controllerClient}
     */
    public ReservationService getReservationService()
    {
        return controllerClient.getService(ReservationService.class);
    }

    /**
     * Init the {@code controller}.
     *
     * @param controller to be inited
     */
    protected void onInitController(Controller controller)
    {
        cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        ResourceServiceImpl resourceService = new ResourceServiceImpl();
        resourceService.setCache(cache);
        controller.addService(resourceService);
        controller.addService(new ReservationServiceImpl());
    }

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
        controller.getAuthorization().setTestingAccessToken(SECURITY_TOKEN.getAccessToken());

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

    /**
     * Run {@link Preprocessor}.
     *
     * @throws FaultException
     */
    protected void runPreprocessor() throws FaultException
    {
        Interval interval = Interval.parse("0/9999");

        EntityManager entityManagerForPreprocessor = getEntityManager();
        Preprocessor.createAndRun(interval, entityManagerForPreprocessor);
        entityManagerForPreprocessor.close();

    }

    /**
     * Run {@link Scheduler}.
     *
     * @throws FaultException
     */
    protected void runScheduler() throws FaultException
    {
        Interval interval = Interval.parse("0/9999");

        EntityManager entityManagerForScheduler = getEntityManager();
        Scheduler.createAndRun(interval, entityManagerForScheduler, cache);
        entityManagerForScheduler.close();
    }

    /**
     * Check if {@link ReservationRequest} was successfully allocated.
     *
     * @param reservationRequestIdentifier
     * @return {@link Reservation}
     * @throws Exception
     */
    protected Reservation checkSuccessfulAllocation(String reservationRequestIdentifier) throws Exception
    {
        ReservationRequest reservationRequest = (ReservationRequest)
                getReservationService().getReservationRequest(SECURITY_TOKEN, reservationRequestIdentifier);
        assertEquals("Reservation request should be in ALLOCATED state.",
                ReservationRequest.State.ALLOCATED, reservationRequest.getState());
        String reservationIdentifier = reservationRequest.getReservationIdentifier();
        assertNotNull(reservationIdentifier);
        Reservation reservation = getReservationService().getReservation(SECURITY_TOKEN, reservationIdentifier);
        assertNotNull("Reservation should be allocated for the reservation request.", reservation);
        return reservation;
    }
}
