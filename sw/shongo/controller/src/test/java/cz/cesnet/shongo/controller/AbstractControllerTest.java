package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.jade.Container;
import org.joda.time.Interval;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link AbstractDatabaseTest} which provides a {@link Controller} instance to extending classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractControllerTest extends AbstractDatabaseTest
{
    private static Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);

    /**
     * {@link SecurityToken} for admin.
     */
    protected static final SecurityToken SECURITY_TOKEN_ROOT =
            new SecurityToken("302be4e89def6d9de3021fd7566d3bc7131284ec");

    /**
     * {@link SecurityToken} for normal user #1.
     */
    protected static final SecurityToken SECURITY_TOKEN_USER1 =
            new SecurityToken("18eea565098d4620d398494b111cb87067a3b6b9");

    /**
     * {@link SecurityToken} for normal user #2.
     */
    protected static final SecurityToken SECURITY_TOKEN_USER2 =
            new SecurityToken("8f474989e2b0011b8fa285c2346e5f6ca3dc809c");

    /**
     * {@link SecurityToken} for normal user #3.
     */
    protected static final SecurityToken SECURITY_TOKEN_USER3 =
            new SecurityToken("53a0bbcbb6086add8c232ff5eddf662035a02908");

    /**
     * @see #SECURITY_TOKEN_USER1
     */
    protected static final SecurityToken SECURITY_TOKEN = SECURITY_TOKEN_USER1;

    /**
     * @see Controller
     */
    private cz.cesnet.shongo.controller.Controller controller;

    /**
     * @see Authorization
     */
    private DummyAuthorization authorization;

    /**
     * @see cz.cesnet.shongo.controller.cache.Cache
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
     * Working interval for {@link #runPreprocessor()} and {@link #runScheduler()}.
     */
    private Interval workingInterval = Temporal.INTERVAL_INFINITE;

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
     */
    public String getUserId(SecurityToken securityToken)
    {
        return authorization.getUserInformation(securityToken).getUserId();
    }

    /**
     * @param workingInterval sets the {@link #workingInterval}
     */
    public void setWorkingInterval(Interval workingInterval)
    {
        this.workingInterval = workingInterval;
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
        controller.addRpcService(new ReservationServiceImpl());
    }

    /**
     * On after controller start.
     */
    protected void onStart()
    {
    }

    private static Container jadeContainer;

    @Override
    public void before() throws Exception
    {
        super.before();

        // Enable throwing internal errors
        Reporter.setThrowInternalErrorsForTesting(true);

        // Do not change default timezone by the controller
        System.setProperty(Configuration.TIMEZONE, "");

        // Change XML-RPC port
        System.setProperty(Configuration.RPC_PORT, "8484");
        System.setProperty(Configuration.JADE_PORT, "8585");

        // Create controller
        controller = new cz.cesnet.shongo.controller.Controller()
        {
            @Override
            public Container startJade()
            {
                synchronized (AbstractControllerTest.class) {
                    if (AbstractControllerTest.jadeContainer == null) {
                        AbstractControllerTest.jadeContainer = super.startJade();
                    }
                    else {
                        logger.info("Reusing JADE container...");
                        this.jadeContainer = AbstractControllerTest.jadeContainer;

                        // Add jade agent
                        addJadeAgent(configuration.getString(Configuration.JADE_AGENT_NAME), jadeAgent);
                    }
                    jadeContainer.waitForJadeAgentsToStart();
                    return AbstractControllerTest.jadeContainer;
                }
            }

            @Override
            public void stop()
            {
                synchronized (AbstractControllerTest.class) {
                    if (this.jadeContainer != null) {
                        logger.info("Stopping JADE agents...");
                        for (String agentName : new LinkedList<String>(this.jadeContainer.getAgentNames())) {
                            this.jadeContainer.removeAgent(agentName);
                        }
                        this.jadeContainer = null;
                    }
                    super.stop();
                }
            }
        };
        controller.setDomain("cz.cesnet", "CESNET, z.s.p.o.");
        controller.setEntityManagerFactory(getEntityManagerFactory());

        // Create authorization
        authorization = DummyAuthorization.createInstance(controller.getConfiguration());
        controller.setAuthorization(authorization);

        onInit();

        // Start controller
        logger.debug("Starting controller for " + getClass().getName() + "...");
        controller.start();
        controller.startRpc();

        // Start client
        controllerClient = new ControllerClient(controller.getRpcHost(), controller.getRpcPort());

        onStart();
    }

    @Override
    public void after() throws Exception
    {
        controller.stop();
        controller.destroy();
        preprocessor.destroy();
        scheduler.destroy();

        // Disable throwing internal errors
        Reporter.setThrowInternalErrorsForTesting(true);

        super.after();
    }

    /**
     * Run {@link Preprocessor}.
     *
     * @param interval
     */
    protected void runPreprocessor(Interval interval)
    {
        EntityManager entityManager = createEntityManager();
        preprocessor.run(interval, entityManager);
        entityManager.close();
    }

    /**
     * Run {@link Scheduler}.
     *
     * @param interval
     */
    protected void runScheduler(Interval interval)
    {
        EntityManager entityManager = createEntityManager();
        scheduler.run(interval, entityManager);
        entityManager.close();
    }

    /**
     * Run propagations to authorization server.
     */
    protected void runAuthorizationPropagation()
    {
        EntityManager entityManager = createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        authorizationManager.propagate(authorization);
        entityManager.close();
    }

    /**
     * Run {@link Preprocessor} and {@link Scheduler}.
     *
     * @param interval
     */
    protected void runWorker(Interval interval)
    {
        runPreprocessor(interval);
        runScheduler(interval);
        runAuthorizationPropagation();
    }

    /**
     * Run {@link Preprocessor}.
     */
    protected void runPreprocessor()
    {
        runPreprocessor(workingInterval);
    }

    /**
     * Run {@link Scheduler}.
     */
    protected void runScheduler()
    {
        runScheduler(workingInterval);
    }

    /**
     * Run {@link Preprocessor} and {@link Scheduler}.
     */
    protected void runPreprocessorAndScheduler()
    {
        runPreprocessor();
        runScheduler();
    }

    /**
     * @param reservationRequestId
     * @return collection of {@link ReservationRequest}s for given {@code reservationRequestId}
     */
    private Collection<ReservationRequest> getReservationRequests(String reservationRequestId)
    {
        List<ReservationRequest> reservationRequests = new LinkedList<ReservationRequest>();
        AbstractReservationRequest abstractReservationRequest =
                getReservationService().getReservationRequest(SECURITY_TOKEN_ROOT, reservationRequestId);
        if (abstractReservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            Assert.assertTrue(reservationRequestSet.getReservationRequests().size() > 0);
            reservationRequests.addAll(reservationRequestSet.getReservationRequests());
        }
        else {
            reservationRequests.add((ReservationRequest) abstractReservationRequest);
        }
        return reservationRequests;
    }

    /**
     * Check if {@link ReservationRequest} is not allocated.
     *
     * @param reservationRequestId for {@link ReservationRequest} to be checked
     * @throws Exception
     */
    protected void checkNotAllocated(String reservationRequestId) throws Exception
    {
        for (ReservationRequest reservationRequest : getReservationRequests(reservationRequestId)) {
            Assert.assertEquals("Reservation request should be in NOT_ALLOCATED state.",
                    ReservationRequestState.NOT_ALLOCATED, reservationRequest.getState());
        }
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
        Reservation reservation = null;
        for (ReservationRequest reservationRequest : getReservationRequests(reservationRequestId)) {
            if (reservationRequest.getState() != ReservationRequestState.ALLOCATED) {
                System.err.println(reservationRequest.getStateReport());
                Thread.sleep(100);
            }
            Assert.assertEquals("Reservation request should be in ALLOCATED state.",
                    ReservationRequestState.ALLOCATED, reservationRequest.getState());
            Assert.assertTrue("Reservation should be allocated.", reservationRequest.getReservationIds().size() > 0);
            for (String reservationId : reservationRequest.getReservationIds()) {
                reservation = getReservationService().getReservation(SECURITY_TOKEN_ROOT, reservationId);
                Assert.assertNotNull("Reservation should be allocated for the reservation request.", reservation);
            }
        }
        return reservation;
    }

    /**
     * Check if {@link ReservationRequest} has failed to be allocated.
     *
     * @param reservationRequestId for {@link ReservationRequest} to be checked
     * @throws Exception
     */
    protected void checkAllocationFailed(String reservationRequestId) throws Exception
    {
        for (ReservationRequest reservationRequest : getReservationRequests(reservationRequestId)) {
            Assert.assertEquals("Reservation request should be in ALLOCATION_FAILED state.",
                    ReservationRequestState.ALLOCATION_FAILED, reservationRequest.getState());
        }
    }

    /**
     * @see #allocate(SecurityToken, AbstractReservationRequest)
     */
    protected String allocate(AbstractReservationRequest reservationRequest) throws Exception
    {
        return allocate(SECURITY_TOKEN, reservationRequest);
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param securityToken
     * @param reservationRequest to be allocated
     * @return shongo-id of created or modified {@link ReservationRequest}
     * @throws Exception
     */
    protected String allocate(SecurityToken securityToken, AbstractReservationRequest reservationRequest)
            throws Exception
    {
        String reservationRequestId;
        if (reservationRequest.getId() == null) {
            reservationRequestId = getReservationService().createReservationRequest(securityToken, reservationRequest);
        }
        else {
            reservationRequestId = getReservationService().modifyReservationRequest(securityToken, reservationRequest);
        }
        if (reservationRequest instanceof ReservationRequestSet) {
            runPreprocessor();
        }
        runScheduler();
        runAuthorizationPropagation();
        return reservationRequestId;
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

    /**
     * Reallocate given {@link AbstractReservationRequest} with given {@code reservationRequestId}.
     *
     * @param reservationRequestId to be allocated
     * @throws Exception
     */
    public void reallocate(String reservationRequestId) throws Exception
    {
        AbstractReservationRequest reservationRequest = getReservationService().getReservationRequest(
                SECURITY_TOKEN_ROOT, reservationRequestId);
        getReservationService().updateReservationRequest(SECURITY_TOKEN_ROOT, reservationRequestId);
        if (reservationRequest instanceof ReservationRequestSet) {
            runPreprocessor();
        }
        runScheduler();
    }

    /**
     * @param userId
     * @param entityId
     * @param role
     * @return {@link AclRecord} with given parameters
     * @throws Exception
     */
    protected AclRecord getAclRecord(String userId, String entityId, Role role) throws Exception
    {
        ListResponse<AclRecord> aclRecords = getAuthorizationService().listAclRecords(
                new AclRecordListRequest(SECURITY_TOKEN, userId, entityId, role));

        if (aclRecords.getItemCount() == 0) {
            return null;
        }
        if (aclRecords.getItemCount() > 1) {
            throw new RuntimeException("Multiple " + new AclRecord(userId, entityId, role).toString() + ".");
        }
        return aclRecords.getItem(0);
    }

    /**
     * Delete {@link AclRecord} with given parameters.
     *
     * @param userId
     * @param entityId
     * @param role
     * @throws Exception
     */
    protected void deleteAclRecord(String userId, String entityId, Role role) throws Exception
    {
        AclRecord aclRecord = getAclRecord(userId, entityId, role);
        if (aclRecord == null) {
            throw new RuntimeException(new AclRecord(userId, entityId, role).toString() + " doesn't exist.");
        }
        getAuthorizationService().deleteAclRecord(SECURITY_TOKEN_ROOT, aclRecord.getId());
    }
}
