package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;

import java.util.List;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

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
     * @return {@link Configuration} from the {@link #controller}
     */
    public Controller getController()
    {
        return controller;
    }

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
     * On controller initialized.
     */
    protected void onInit()
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
     * On after controller start.
     */
    protected void onStart()
    {
    }

    @Override
    public void before() throws Exception
    {
        super.before();

        // Change XML-RPC port
        System.setProperty(Configuration.RPC_PORT, "8484");
        System.setProperty(Configuration.JADE_PORT, "8585");

        // Start controller
        controller = new cz.cesnet.shongo.controller.Controller();
        controller.setDomain("cz.cesnet", "CESNET, z.s.p.o.");
        controller.setEntityManagerFactory(getEntityManagerFactory());

        onInit();

        controller.start();
        controller.startRpc();
        controller.getAuthorization().setTestingAccessToken(SECURITY_TOKEN.getAccessToken());

        // Start client
        controllerClient = new ControllerClient(controller.getRpcHost(), controller.getRpcPort());

        onStart();
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
     * @param interval
     * @throws FaultException
     */
    protected void runPreprocessor(Interval interval) throws FaultException
    {
        EntityManager entityManagerForPreprocessor = getEntityManager();
        Preprocessor.createAndRun(interval, entityManagerForPreprocessor, cache);
        entityManagerForPreprocessor.close();
    }

    /**
     * Run {@link Preprocessor}.
     *
     * @throws FaultException
     */
    protected void runPreprocessor() throws FaultException
    {
        runPreprocessor(Interval.parse("0/9999"));
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
     * @param reservationRequestIdentifier for {@link ReservationRequest} to be checked
     * @return {@link Reservation}
     * @throws Exception
     */
    protected Reservation checkAllocated(String reservationRequestIdentifier) throws Exception
    {
        AbstractReservationRequest abstractReservationRequest =
                getReservationService().getReservationRequest(SECURITY_TOKEN, reservationRequestIdentifier);
        String reservationIdentifier = null;
        if (abstractReservationRequest instanceof NormalReservationRequest) {
            ReservationRequest reservationRequest;
            if (abstractReservationRequest instanceof ReservationRequestSet) {
                ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
                assertEquals(1, reservationRequestSet.getReservationRequests().size());
                reservationRequest = reservationRequestSet.getReservationRequests().get(0);
            }
            else {
                reservationRequest = (ReservationRequest) abstractReservationRequest;
            }
            if (reservationRequest.getState() != ReservationRequest.State.ALLOCATED) {
                System.err.println(reservationRequest.getStateReport());
                Thread.sleep(100);
            }
            assertEquals("Reservation request should be in ALLOCATED state.",
                    ReservationRequest.State.ALLOCATED, reservationRequest.getState());
            reservationIdentifier = reservationRequest.getReservationIdentifier();
        }
        else if (abstractReservationRequest instanceof PermanentReservationRequest) {
            PermanentReservationRequest permanentReservationRequest =
                    (PermanentReservationRequest) abstractReservationRequest;
            List<ResourceReservation> resourceReservations = permanentReservationRequest.getResourceReservations();
            assertTrue("Permament reservation request should have at least one resource reservation.",
                    resourceReservations.size() > 0);
            reservationIdentifier = resourceReservations.get(0).getIdentifier();
        }
        assertNotNull(reservationIdentifier);
        Reservation reservation = getReservationService().getReservation(SECURITY_TOKEN, reservationIdentifier);
        assertNotNull("Reservation should be allocated for the reservation request.", reservation);
        return reservation;
    }

    /**
     * Check if {@link ReservationRequest}'s allocation failed.
     *
     * @param reservationRequestIdentifier for {@link ReservationRequest} to be checked
     * @return {@link Reservation}
     * @throws Exception
     */
    protected void checkAllocationFailed(String reservationRequestIdentifier) throws Exception
    {
        AbstractReservationRequest abstractReservationRequest = getReservationService().getReservationRequest(
                SECURITY_TOKEN, reservationRequestIdentifier);
        ReservationRequest reservationRequest;
        if (abstractReservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            assertEquals(1, reservationRequestSet.getReservationRequests().size());
            reservationRequest = reservationRequestSet.getReservationRequests().get(0);
        }
        else {
            reservationRequest = (ReservationRequest) abstractReservationRequest;
        }
        assertEquals("Reservation request should be in ALLOCATION_FAILED state.",
                ReservationRequest.State.ALLOCATION_FAILED, reservationRequest.getState());
        String reservationIdentifier = reservationRequest.getReservationIdentifier();
        assertNull("No reservation should be allocated for the reservation request.", reservationIdentifier);
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     * @return identifier of created or modified {@link ReservationRequest}
     * @throws Exception
     */
    protected String allocate(AbstractReservationRequest reservationRequest) throws Exception
    {
        String identifier;
        if (reservationRequest.getIdentifier() == null) {
            identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        }
        else {
            identifier = reservationRequest.getIdentifier();
            getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        }
        if (reservationRequest instanceof ReservationRequestSet) {
            runPreprocessor();
        }
        runScheduler();
        return identifier;
    }

    /**
     * Allocate given {@code reservationRequest} and call {@link #checkAllocated(String)}.
     *
     * @param reservationRequest to be allocated and checked
     * @return allocated {@link Reservation}
     * @throws Exception
     */
    protected Reservation allocateAndCheck(AbstractReservationRequest reservationRequest) throws Exception
    {
        String identifier = allocate(reservationRequest);
        return checkAllocated(identifier);
    }

    /**
     * Allocate given {@code reservationRequest} and call {@link #checkAllocated(String)}.
     *
     * @param reservationRequest to be allocated and checked
     * @return allocated {@link Reservation}
     * @throws Exception
     */
    protected void allocateAndCheckFailed(AbstractReservationRequest reservationRequest) throws Exception
    {
        String identifier = allocate(reservationRequest);
        checkAllocationFailed(identifier);
    }
}
