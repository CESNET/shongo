package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;

import static junit.framework.Assert.*;

/**
 * Abstract controller test provides a {@link Controller} instance to extending classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractControllerTest extends AbstractDatabaseTest
{
    /**
     * {@link SecurityToken} for normal user.
     */
    protected static final SecurityToken SECURITY_TOKEN =
            new SecurityToken("18eea565098d4620d398494b111cb87067a3b6b9");

    /**
     * {@link SecurityToken} for admin.
     */
    protected static final SecurityToken SECURITY_TOKEN_ROOT =
            new SecurityToken("302be4e89def6d9de3021fd7566d3bc7131284ec");

    /**
     * @see Controller
     */
    private cz.cesnet.shongo.controller.Controller controller;

    /**
     * @see Authorization
     */
    private TestingAuthorization authorization;

    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see Preprocessor
     */
    private Preprocessor preprocessor;

    /**
     * @see Scheduler
     */
    private Scheduler scheduler;

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
     * @return {@link cz.cesnet.shongo.controller.api.rpc.AuthorizationService} from the {@link #controllerClient}
     */
    public AuthorizationService getAuthorizationService()
    {
        return controllerClient.getService(AuthorizationService.class);
    }

    /**
     * @return {@link cz.cesnet.shongo.controller.api.rpc.ResourceService} from the {@link #controllerClient}
     */
    public ResourceService getResourceService()
    {
        return controllerClient.getService(ResourceService.class);
    }

    /**
     * @return {@link cz.cesnet.shongo.controller.api.rpc.ReservationService} from the {@link #controllerClient}
     */
    public ReservationService getReservationService()
    {
        return controllerClient.getService(ReservationService.class);
    }

    /**
     * @param securityToken for which the user-id should be returned
     * @return user-id for given {@code securityToken}
     * @throws FaultException
     */
    public String getUserId(SecurityToken securityToken) throws FaultException
    {
        return authorization.getUserInformation(securityToken).getUserId();
    }

    /**
     * On controller initialized.
     */
    protected void onInit()
    {
        cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init(controller.getConfiguration());

        preprocessor = new Preprocessor();
        preprocessor.setCache(cache);
        preprocessor.setAuthorization(authorization);
        preprocessor.init();

        scheduler = new Scheduler();
        scheduler.setCache(cache);
        scheduler.setAuthorization(authorization);
        scheduler.setNotificationManager(controller.getNotificationManager());
        scheduler.init();

        controller.addRpcService(new AuthorizationServiceImpl());
        controller.addRpcService(new ResourceServiceImpl(cache));
        controller.addRpcService(new ReservationServiceImpl(cache));
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

        // Do not change default timezone by the controller
        System.setProperty(Configuration.TIMEZONE, "");

        // Change XML-RPC port
        System.setProperty(Configuration.RPC_PORT, "8484");
        System.setProperty(Configuration.JADE_PORT, "8585");

        // Create controller
        controller = new cz.cesnet.shongo.controller.Controller();
        controller.setDomain("cz.cesnet", "CESNET, z.s.p.o.");
        controller.setEntityManagerFactory(getEntityManagerFactory());

        // Create authorization
        authorization = TestingAuthorization.createInstance(controller.getConfiguration());
        controller.setAuthorization(authorization);

        onInit();

        // Start controller
        controller.start();
        controller.startRpc();

        // Start client
        controllerClient = new ControllerClient(controller.getRpcHost(), controller.getRpcPort());

        onStart();
    }

    @Override
    public void after()
    {
        controller.stop();
        controller.destroy();
        preprocessor.destroy();
        scheduler.destroy();

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
        preprocessor.run(interval, entityManagerForPreprocessor);
        entityManagerForPreprocessor.close();
    }

    /**
     * Run {@link Scheduler}.
     *
     * @param interval
     * @throws FaultException
     */
    protected void runScheduler(Interval interval) throws FaultException
    {
        EntityManager entityManagerForScheduler = getEntityManager();
        scheduler.run(interval, entityManagerForScheduler);
        entityManagerForScheduler.close();
    }

    /**
     * Run {@link Preprocessor} and {@link Scheduler}.
     *
     * @param interval
     * @throws FaultException
     */
    protected void runPreprocessorAndScheduler(Interval interval) throws FaultException
    {
        runPreprocessor(interval);
        runScheduler(interval);
    }

    /**
     * Run {@link Preprocessor}.
     *
     * @throws FaultException
     */
    protected void runPreprocessor() throws FaultException
    {
        runPreprocessor(Temporal.INTERVAL_INFINITE);
    }

    /**
     * Run {@link Scheduler}.
     *
     * @throws FaultException
     */
    protected void runScheduler() throws FaultException
    {
        runScheduler(Temporal.INTERVAL_INFINITE);
    }

    /**
     * Run {@link Preprocessor} and {@link Scheduler}.
     *
     * @throws FaultException
     */
    protected void runPreprocessorAndScheduler() throws FaultException
    {
        runPreprocessor();
        runScheduler();
    }

    /**
     * Check if {@link ReservationRequest} was successfully allocated.
     *
     * @param reservationRequestId for {@link ReservationRequest} to be checked
     * @return {@link Reservation}
     * @throws Exception
     */
    protected Reservation checkAllocated(String reservationRequestId) throws Exception
    {
        AbstractReservationRequest abstractReservationRequest =
                getReservationService().getReservationRequest(SECURITY_TOKEN, reservationRequestId);
        ReservationRequest reservationRequest;
        if (abstractReservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            assertEquals(1, reservationRequestSet.getReservationRequests().size());
            reservationRequest = reservationRequestSet.getReservationRequests().get(0);
        }
        else {
            reservationRequest = (ReservationRequest) abstractReservationRequest;
        }
        if (reservationRequest.getState() != ReservationRequestState.ALLOCATED) {
            System.err.println(reservationRequest.getStateReport());
            Thread.sleep(100);
        }
        assertEquals("Reservation request should be in ALLOCATED state.",
                ReservationRequestState.ALLOCATED, reservationRequest.getState());
        String reservationId = reservationRequest.getReservationId();
        assertNotNull(reservationId);
        Reservation reservation = getReservationService().getReservation(SECURITY_TOKEN, reservationId);
        assertNotNull("Reservation should be allocated for the reservation request.", reservation);
        return reservation;
    }

    /**
     * Check if {@link ReservationRequest}'s allocation failed.
     *
     * @param reservationRequestId for {@link ReservationRequest} to be checked
     * @return {@link Reservation}
     * @throws Exception
     */
    protected void checkAllocationFailed(String reservationRequestId) throws Exception
    {
        AbstractReservationRequest abstractReservationRequest = getReservationService().getReservationRequest(
                SECURITY_TOKEN, reservationRequestId);
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
                ReservationRequestState.ALLOCATION_FAILED, reservationRequest.getState());
        String reservationId = reservationRequest.getReservationId();
        assertNull("No reservation should be allocated for the reservation request.", reservationId);
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     * @return shongo-id of created or modified {@link ReservationRequest}
     * @throws Exception
     */
    protected String allocate(AbstractReservationRequest reservationRequest) throws Exception
    {
        String reservationRequestId;
        if (reservationRequest.getId() == null) {
            reservationRequestId = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        }
        else {
            reservationRequestId = reservationRequest.getId();
            getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        }
        if (reservationRequest instanceof ReservationRequestSet) {
            runPreprocessor();
        }
        runScheduler();
        return reservationRequestId;
    }

    /**
     * Reallocate given {@link AbstractReservationRequest} with given {@code reservationRequestId}.
     *
     * @param reservationRequestId to be allocated
     * @throws Exception
     */
    public void reallocate(String reservationRequestId) throws Exception
    {
        AbstractReservationRequest reservationRequest = getReservationService().getReservationRequest(SECURITY_TOKEN,
                reservationRequestId);
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        if (reservationRequest instanceof ReservationRequestSet) {
            runPreprocessor();
        }
        runScheduler();
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
        String reservationRequestId = allocate(reservationRequest);
        return checkAllocated(reservationRequestId);
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
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);
    }
}
