package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.scheduler.Preprocessor;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.jade.Container;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.*;

/**
 * {@link AbstractDatabaseTest} which provides a {@link Controller} instance to extending classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractControllerTest extends AbstractDatabaseTest
{
    private static Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);

    /**
     * Test ports.
     */
    public static final int TEST_RPC_PORT = 8484;
    public static final int TEST_JADE_PORT = 8585;

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
     * @see cz.cesnet.shongo.controller.scheduler.Preprocessor
     */
    private Preprocessor preprocessor;

    /**
     * @see cz.cesnet.shongo.controller.scheduler.Scheduler
     */
    private Scheduler scheduler;

    /**
     * Last {@link Scheduler.Result}.
     */
    private Scheduler.Result schedulerResult;

    /**
     * Specifies whether automatic execution of notifications while scheduling is enabled.
     */
    private boolean notificationExecutionEnabled = true;

    /**
     * @see ControllerClient
     */
    private ControllerClient controllerClient;

    /**
     * Working interval for {@link #runPreprocessor()} and {@link #runScheduler()}.
     */
    private Interval workingInterval = Temporal.INTERVAL_INFINITE;

    /**
     * @return {@link ControllerConfiguration} from the {@link #controller}
     */
    public Controller getController()
    {
        return controller;
    }
    /**
     * @return {@link ControllerConfiguration} from the {@link #controller}
     */
    public ControllerConfiguration getConfiguration()
    {
        return controller.getConfiguration();
    }

    /**
     * @return {@link #authorization}
     */
    public DummyAuthorization getAuthorization()
    {
        return authorization;
    }

    /**
     * @return {@link #schedulerResult}
     */
    public Scheduler.Result getSchedulerResult()
    {
        return schedulerResult;
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
     * @param securityToken for which the {@link UserInformation} should be returned
     * @return {@link UserInformation} for given {@code securityToken}
     */
    public UserInformation getUserInformation(SecurityToken securityToken)
    {
        return authorization.getUserInformation(securityToken);
    }

    /**
     * @param userIds for which the {@link UserInformation} should be returned
     * @return {@link UserInformation} for given {@code userIds}
     */
    public List<UserInformation> getUserInformation(Set<String> userIds)
    {
        List<UserInformation> userInformation = new LinkedList<UserInformation>();
        for (String userId : userIds) {
            userInformation.add(authorization.getUserInformation(userId));
        }
        return userInformation;
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
     * @param notificationExecutionEnabled sets the {@link #notificationExecutionEnabled}
     */
    public void setNotificationExecutionEnabled(boolean notificationExecutionEnabled)
    {
        this.notificationExecutionEnabled = notificationExecutionEnabled;
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
        preprocessor.init(controller.getConfiguration());

        scheduler = new Scheduler(cache, controller.getNotificationManager(), controller.getCalendarManager());
        scheduler.setAuthorization(authorization);
        scheduler.init(controller.getConfiguration());

        // Initialize Inter Domain agent
        if (getConfiguration().isInterDomainConfigured()) {
            InterDomainAgent.create(getEntityManagerFactory(), getConfiguration(), authorization, controller.getEmailSender(), cache);
        }

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

    private static Container jadeContainer;

    /**
     * Setup system properties for testing controller.
     */
    public void configureSystemProperties()
    {
        // Do not change default timezone by the controller
        System.setProperty(ControllerConfiguration.TIMEZONE, "");

        // Change XML-RPC and JADE port
        System.setProperty(ControllerConfiguration.RPC_PORT, String.valueOf(TEST_RPC_PORT));
        System.setProperty(ControllerConfiguration.JADE_PORT, String.valueOf(TEST_JADE_PORT));
    }

    @Override
    public void before() throws Exception
    {
        super.before();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        synchronized (AbstractControllerTest.class) {

            // Configure system properties
            configureSystemProperties();

            // Create controller
            controller = cz.cesnet.shongo.controller.Controller.create(new Controller(null)
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
                            addJadeAgent(configuration.getString(ControllerConfiguration.JADE_AGENT_NAME), jadeAgent);
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
            });
            controller.setDomain("cz.cesnet", "CESNET, z.s.p.o.");
            controller.setEntityManagerFactory(getEntityManagerFactory());

            // Enable throwing internal errors
            controller.setThrowInternalErrorsForTesting(true);

            // Create authorization
            authorization = DummyAuthorization.createInstance(controller.getConfiguration(), getEntityManagerFactory());
            controller.setAuthorization(authorization);

            onInit();

            // Start controller
            logger.debug("Starting controller for " + getClass().getName() + "...");
            controller.start();
            controller.startRpc();
            controller.startRESTApi();

            // Start client
            controllerClient = new ControllerClient(controller.getRpcHost(), controller.getRpcPort());

            onStart();
        }
    }

    @Override
    public void after() throws Exception
    {
        synchronized (AbstractControllerTest.class) {
            // Disable throwing internal errors
            controller.setThrowInternalErrorsForTesting(false);

            controller.stop();
            controller.destroy();
            preprocessor.destroy();
            scheduler.destroy();
            InterDomainAgent.destroy();
        }

        super.after();
    }

    /**
     * Run {@link Preprocessor}.
     *
     * @param interval
     */
    protected Preprocessor.Result runPreprocessor(Interval interval)
    {
        EntityManager entityManager = createEntityManager();
        Preprocessor.Result result = preprocessor.run(interval, entityManager);
        entityManager.close();
        return result;
    }

    /**
     * Run {@link Preprocessor}.
     *
     * @param dateTime representing now
     */
    protected void runPreprocessor(DateTime dateTime)
    {
        runPreprocessor(new Interval(dateTime, workingInterval.getEnd()));
    }

    /**
     * Run {@link cz.cesnet.shongo.controller.scheduler.Scheduler}.
     *
     * @param interval
     */
    protected Scheduler.Result runScheduler(Interval interval)
    {
        EntityManager entityManager = createEntityManager();
        schedulerResult = scheduler.run(interval, entityManager);
        if (notificationExecutionEnabled) {
            executeNotifications(entityManager);
        }
        entityManager.close();
        checkSpecificationSummaryConsistency();
        return schedulerResult;
    }

    /**
     * Invoke {@link NotificationManager#executeNotifications}
     *
     * @param entityManager to be used
     */
    protected void executeNotifications(EntityManager entityManager)
    {
        NotificationManager notificationManager = controller.getNotificationManager();
        notificationManager.executeNotifications(entityManager);
    }

    /**
     * Invoke {@link NotificationManager#executeNotifications}
     */
    protected final void executeNotifications()
    {
        EntityManager entityManager = createEntityManager();
        executeNotifications(entityManager);
        entityManager.close();
    }

    /**
     * Run {@link Scheduler}.
     *
     * @param dateTime representing now
     */
    protected final void runScheduler(DateTime dateTime)
    {
        runScheduler(new Interval(dateTime, workingInterval.getEnd()));
    }

    /**
     * Run {@link cz.cesnet.shongo.controller.scheduler.Preprocessor} and {@link Scheduler}.
     *
     * @param interval
     */
    protected final void runWorker(Interval interval)
    {
        runPreprocessor(interval);
        runScheduler(interval);
    }

    /**
     * Run {@link Preprocessor}.
     */
    protected Preprocessor.Result runPreprocessor()
    {
        return runPreprocessor(workingInterval);
    }

    /**
     * Run {@link Scheduler}.
     */
    protected Scheduler.Result runScheduler()
    {
        return runScheduler(workingInterval);
    }

    /**
     * Run {@link cz.cesnet.shongo.controller.scheduler.Preprocessor} and {@link Scheduler}.
     */
    protected final void runPreprocessorAndScheduler()
    {
        runPreprocessor();
        runScheduler();
    }

    /**
     * Run {@link Preprocessor} and {@link cz.cesnet.shongo.controller.scheduler.Scheduler}.
     *
     * @param interval
     */
    protected final void runPreprocessorAndScheduler(Interval interval)
    {
        runPreprocessor(interval);
        runScheduler(interval);
    }

    /**
     * @param resource to be created
     * @return new resource-id
     */
    public String createResource(Resource resource)
    {
        return getResourceService().createResource(SECURITY_TOKEN_ROOT, resource);
    }

    /**
     * @param resourceId
     * @return {@link Resource}
     */
    public <T extends Resource> T getResource(String resourceId, Class<T> resourceType)
    {
        return resourceType.cast(getResourceService().getResource(SECURITY_TOKEN_ROOT, resourceId));
    }

    /**
     * @param resource to be created
     * @return new resource-id
     */
    public String createResource(SecurityToken securityToken, Resource resource)
    {
        return getResourceService().createResource(securityToken, resource);
    }

    /**
     * @param reservationRequestId
     * @param reservationRequestClass
     * @return reservation request for given {@code reservationRequestId}
     */
    public <T extends cz.cesnet.shongo.controller.api.AbstractReservationRequest> T getReservationRequest(
            String reservationRequestId, Class<T> reservationRequestClass)
    {
        return reservationRequestClass.cast(
                getReservationService().getReservationRequest(SECURITY_TOKEN_ROOT, reservationRequestId));
    }

    /**
     * @return list of {@link ReservationRequestSummary}s
     */
    public List<ReservationRequestSummary> listReservationRequests()
    {
        return getReservationService().listReservationRequests(
                new ReservationRequestListRequest(SECURITY_TOKEN)).getItems();
    }

    /**
     * @return map of {@link ReservationRequestSummary}s by identifiers
     */
    public Map<String, ReservationRequestSummary> getReservationRequestMap()
    {
        List<ReservationRequestSummary> reservationRequests = getReservationService().listReservationRequests(
                new ReservationRequestListRequest(SECURITY_TOKEN)).getItems();
        Map<String, ReservationRequestSummary> reservationRequestMap = new HashMap<String, ReservationRequestSummary>();
        for (ReservationRequestSummary reservationRequest : reservationRequests) {
            reservationRequestMap.put(reservationRequest.getId(), reservationRequest);
        }
        return reservationRequestMap;
    }

    /**
     * @return list of {@link Reservation}s
     */
    public List<ReservationSummary> listReservations()
    {
        return getReservationService().listReservations(new ReservationListRequest(SECURITY_TOKEN_ROOT)).getItems();
    }

    /**
     * @param reservationId
     * @param reservationClass
     * @return reservation request for given {@code reservationRequestId}
     */
    public <T extends cz.cesnet.shongo.controller.api.Reservation> T getReservation(
            String reservationId, Class<T> reservationClass)
    {
        return reservationClass.cast(getReservationService().getReservation(SECURITY_TOKEN_ROOT, reservationId));
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
            ReservationRequestListRequest request = new ReservationRequestListRequest();
            request.setSecurityToken(SECURITY_TOKEN_ROOT);
            request.setParentReservationRequestId(reservationRequestId);
            List<ReservationRequestSummary> childReservationRequests =
                    getReservationService().listReservationRequests(request).getItems();
            Assert.assertTrue(childReservationRequests.size() > 0);
            for (ReservationRequestSummary reservationRequestSummary : childReservationRequests) {
                reservationRequests.add((ReservationRequest) getReservationService().getReservationRequest(
                        SECURITY_TOKEN_ROOT, reservationRequestSummary.getId()));
            }
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
                    AllocationState.NOT_ALLOCATED, reservationRequest.getAllocationState());
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
            if (!AllocationState.ALLOCATED.equals(reservationRequest.getAllocationState())) {
                System.err.println(reservationRequest.getAllocationStateReport());
                Thread.sleep(100);
            }
            Assert.assertEquals("Reservation request should be in ALLOCATED state.",
                    AllocationState.ALLOCATED, reservationRequest.getAllocationState());
            Assert.assertTrue("Reservation should be allocated.", reservationRequest.getReservationIds().size() > 0);
            String lastReservationId = reservationRequest.getLastReservationId();
            Assert.assertNotNull("Reservation should be allocated for the reservation request.", lastReservationId);
            reservation = getReservationService().getReservation(SECURITY_TOKEN_ROOT, lastReservationId);
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
            if (!AllocationState.ALLOCATION_FAILED.equals(reservationRequest.getAllocationState())) {
                System.err.println(reservationRequest.getAllocationStateReport());
                Thread.sleep(100);
            }
            Assert.assertEquals("Reservation request should be in ALLOCATION_FAILED state.",
                    AllocationState.ALLOCATION_FAILED, reservationRequest.getAllocationState());
        }
    }

    /**
     * @see #allocate(SecurityToken, AbstractReservationRequest, org.joda.time.DateTime)
     */
    protected String allocate(AbstractReservationRequest reservationRequest) throws Exception
    {
        return allocate(SECURITY_TOKEN, reservationRequest, workingInterval.getStart());
    }

    /**
     * @see #allocate(SecurityToken, AbstractReservationRequest, org.joda.time.DateTime)
     */
    protected String allocate(SecurityToken securityToken, AbstractReservationRequest reservationRequest) throws Exception
    {
        return allocate(securityToken, reservationRequest, workingInterval.getStart());
    }

    /**
     * @see #allocate(SecurityToken, AbstractReservationRequest, org.joda.time.DateTime)
     */
    protected String allocate(AbstractReservationRequest reservationRequest, DateTime dateTime) throws Exception
    {
        return allocate(SECURITY_TOKEN, reservationRequest, dateTime);
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param securityToken
     * @param reservationRequest to be allocated
     * @param dateTime
     * @return shongo-id of created or modified {@link ReservationRequest}
     * @throws Exception
     */
    protected String allocate(SecurityToken securityToken, AbstractReservationRequest reservationRequest,
            DateTime dateTime)
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
            runPreprocessor(dateTime);
        }
        runScheduler(dateTime);
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
     * @param objectId
     * @param role
     * @return {@link cz.cesnet.shongo.controller.api.AclEntry} with given parameters
     * @throws Exception
     */
    protected AclEntry getAclEntry(String userId, String objectId, ObjectRole role) throws Exception
    {
        ListResponse<AclEntry> aclEntries = getAuthorizationService().listAclEntries(
                new AclEntryListRequest(SECURITY_TOKEN, userId, objectId, role));

        if (aclEntries.getItemCount() == 0) {
            return null;
        }
        if (aclEntries.getItemCount() > 1) {
            throw new RuntimeException("Multiple " + new AclEntry(userId, objectId, role).toString() + ".");
        }
        return aclEntries.getItem(0);
    }

    /**
     * @param groupId
     * @param objectId
     * @param role
     * @return {@link cz.cesnet.shongo.controller.api.AclEntry} with given parameters
     * @throws Exception
     */
    protected AclEntry getAclEntryForGroup(String groupId, String objectId, ObjectRole role) throws Exception
    {
        AclEntryListRequest request = new AclEntryListRequest(SECURITY_TOKEN);
        request.addObjectId(objectId);
        request.addRole(role);
        ListResponse<AclEntry> aclEntries = getAuthorizationService().listAclEntries(request);

        for (AclEntry aclEntry : aclEntries) {
            if (AclIdentityType.GROUP.equals(aclEntry.getIdentityType()) && aclEntry.getIdentityPrincipalId().equals(groupId)) {
                return aclEntry;
            }
        }

        return null;
    }

    /**
     * Delete {@link cz.cesnet.shongo.controller.api.AclEntry} with given parameters.
     *
     * @param userId
     * @param objectId
     * @param role
     * @throws Exception
     */
    protected void deleteAclEntry(String userId, String objectId, ObjectRole role) throws Exception
    {
        AclEntry aclEntry = getAclEntry(userId, objectId, role);
        if (aclEntry == null) {
            throw new RuntimeException(new AclEntry(userId, objectId, role).toString() + " doesn't exist.");
        }
        getAuthorizationService().deleteAclEntry(SECURITY_TOKEN_ROOT, aclEntry.getId());
    }

    /**
     * Checks consistency of table specification_summary. For more see init.sql and entity {@link cz.cesnet.shongo.controller.booking.specification.Specification}.
     */
    protected void checkSpecificationSummaryConsistency()
    {
        EntityManager entityManager = getEntityManagerFactory().createEntityManager();
        try {
            String specificationQuery = NativeQuery.getNativeQuery(NativeQuery.SPECIFICATION_SUMMARY_CHECK);

            List<Object[]> specifications = entityManager.createNativeQuery(specificationQuery).getResultList();
            for (Object[] record : specifications) {
                logger.error("Uncached specification: " + Arrays.toString(record));
            }
            Assert.assertTrue("Some specifications has not been cached in table specification_summary.", specifications.isEmpty());
        } finally {
            entityManager.close();
        }
    }
}
