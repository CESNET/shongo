package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.ControllerFaultSet;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.report.InternalErrorHandler;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.report.InternalErrorType;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestSet;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.fault.FaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationManager extends AbstractManager
{
    private static Logger logger = LoggerFactory.getLogger(AuthorizationManager.class);

    /**
     * @see Authorization
     */
    private Authorization authorization;

    /**
     * Constructor.
     *
     * @param entityManager sets the {@link #entityManager}
     */
    public AuthorizationManager(Authorization authorization, EntityManager entityManager)
    {
        super(entityManager);

        if (authorization == null) {
            throw new IllegalArgumentException("Authorization must not be null.");
        }
        this.authorization = authorization;
    }

    private void createRequest(PersistentObject request)
    {
        if (request instanceof AclRecordCreateRequest) {
            AclRecordCreateRequest aclRecordCreateRequest = (AclRecordCreateRequest) request;
            logger.debug("Storing Request to Create ACL (user: {}, entity: {}, role: {})", new Object[]{
                    aclRecordCreateRequest.getUserId(),
                    aclRecordCreateRequest.getEntityId(),
                    aclRecordCreateRequest.getRole()
            });
        }
        else if (request instanceof AclRecordDeleteRequest) {
            AclRecordDeleteRequest aclRecordDeleteRequest = (AclRecordDeleteRequest) request;
            logger.debug("Storing Request to Delete ACL (id: {})", new Object[]{
                    aclRecordDeleteRequest.getAclRecordId()
            });
        }
        entityManager.persist(request);
    }

    private void removeRequest(PersistentObject request)
    {
        if (request instanceof AclRecordCreateRequest) {
            AclRecordCreateRequest aclRecordCreateRequest = (AclRecordCreateRequest) request;
            logger.debug("Removing Request to Create ACL (user: {}, entity: {}, role: {})", new Object[]{
                    aclRecordCreateRequest.getUserId(),
                    aclRecordCreateRequest.getEntityId(),
                    aclRecordCreateRequest.getRole()
            });
        }
        else if (request instanceof AclRecordDeleteRequest) {
            AclRecordDeleteRequest aclRecordDeleteRequest = (AclRecordDeleteRequest) request;
            logger.debug("Removing Request to Delete ACL (id: {})", new Object[]{
                    aclRecordDeleteRequest.getAclRecordId()
            });
        }
        entityManager.remove(request);
    }

    /**
     * Create a new {@link AclRecord}.
     *
     * @param userId   of user for which the ACL is created.
     * @param entityId of entity for which the ACL is created.
     * @param role     which is created for given user and given entity
     * @return new {@link AclRecord}
     * @throws cz.cesnet.shongo.fault.FaultException
     *          when the creation failed.
     */
    public AclRecordCreateRequest createAclRecord(String userId, EntityIdentifier entityId, Role role)
            throws FaultException
    {
        if (userId.equals(Authorization.ROOT_USER_ID)) {
            return null;
        }
        PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
        if (entity == null) {
            ControllerFaultSet.throwEntityNotFoundFault(entityId);
        }
        EntityType entityType = entityId.getEntityType();
        if (!entityType.allowsRole(role)) {
            ControllerFaultSet.throwAclInvalidRoleFault(entityId.toId(), role.toString());
        }

        AclRecordCreateRequest aclRecordCreateRequest = new AclRecordCreateRequest();
        aclRecordCreateRequest.setUserId(userId);
        aclRecordCreateRequest.setEntityId(entityId.toId());
        aclRecordCreateRequest.setRole(role);
        createRequest(aclRecordCreateRequest);

        createChildAclRecords(aclRecordCreateRequest, userId, entity, role);

        return aclRecordCreateRequest;
    }

    /**
     * Create a new {@link AclRecord}.
     *
     * @param userId of user for which the ACL is created.
     * @param entity for which the ACL is created.
     * @param role   which is created for given user and given entity
     * @return new {@link AclRecord}
     * @throws FaultException when the creation failed.
     */
    public AclRecordCreateRequest createAclRecord(String userId, PersistentObject entity, Role role)
    {
        if (userId.equals(Authorization.ROOT_USER_ID)) {
            return null;
        }

        AclRecordCreateRequest aclRecordCreateRequest = new AclRecordCreateRequest();
        aclRecordCreateRequest.setUserId(userId);
        aclRecordCreateRequest.setEntityId(EntityIdentifier.formatId(entity));
        aclRecordCreateRequest.setRole(role);
        createRequest(aclRecordCreateRequest);

        createChildAclRecords(aclRecordCreateRequest, userId, entity, role);

        return aclRecordCreateRequest;
    }

    /**
     * Creates all {@link AclRecord}s from given {@code parentEntity} to given {@code childEntity} which the
     * {@code childEntity} allows.
     *
     * @param parentEntity from which should be fetch all existing {@link AclRecord}s
     * @param childEntity  to which should be created new {@link AclRecord}s
     * @throws FaultException
     */
    public void createAclRecordForChildEntity(PersistentObject parentEntity, PersistentObject childEntity)
            throws FaultException
    {
        EntityIdentifier parentEntityId = new EntityIdentifier(parentEntity);
        EntityIdentifier childEntityId = new EntityIdentifier(childEntity);
        for (AclRecordCreateRequest parentAclRecordCreateRequest : getAclRecordCreateRequests(parentEntityId)) {
            String userId = parentAclRecordCreateRequest.getUserId();
            Role role = parentAclRecordCreateRequest.getRole();
            EntityType childEntityType = childEntityId.getEntityType();
            if (childEntityType.allowsRole(role)) {
                AclRecordCreateRequest aclRecordCreateRequest = new AclRecordCreateRequest();
                aclRecordCreateRequest.setUserId(userId);
                aclRecordCreateRequest.setEntityId(childEntityId.toId());
                aclRecordCreateRequest.setRole(role);
                aclRecordCreateRequest.setParentCreateRequest(parentAclRecordCreateRequest);
                createRequest(aclRecordCreateRequest);
                createChildAclRecords(aclRecordCreateRequest, userId, childEntity, role);
            }
            else {
                createChildAclRecords(parentAclRecordCreateRequest, userId, childEntity, role);
            }
        }
        for (AclRecord parentAclRecord : authorization.getAclRecords(parentEntityId)) {
            String userId = parentAclRecord.getUserId();
            Role role = parentAclRecord.getRole();
            EntityType childEntityType = childEntityId.getEntityType();
            if (childEntityType.allowsRole(role)) {
                AclRecordCreateRequest aclRecordCreateRequest = new AclRecordCreateRequest();
                aclRecordCreateRequest.setUserId(userId);
                aclRecordCreateRequest.setEntityId(childEntityId.toId());
                aclRecordCreateRequest.setRole(role);
                aclRecordCreateRequest.setParentAclRecordId(parentAclRecord.getId());
                createRequest(aclRecordCreateRequest);
                createChildAclRecords(aclRecordCreateRequest, userId, childEntity, role);
            }
            else {
                createChildAclRecords(parentAclRecord.getId(), userId, childEntity, role);
            }
        }
    }

    /**
     * Delete given {@code aclRecord} and all corresponding {@link AclRecord}s from child entities.
     *
     * @param aclRecord to be deleted
     * @throws FaultException when the deletion failed
     */
    public void deleteAclRecord(AclRecord aclRecord) throws FaultException
    {
        AclRecordDeleteRequest aclRecordDeleteRequest = new AclRecordDeleteRequest();
        aclRecordDeleteRequest.setAclRecordId(aclRecord.getId());
        createRequest(aclRecordDeleteRequest);

        // Get child ACL records
        EntityIdentifier entityId = aclRecord.getEntityId();
        PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
        if (entity != null) {
            deleteChildAclRecords(aclRecord.getUserId(), entity, entityId, aclRecord.getRole());
        }
    }

    /**
     * Delete all given {@link AclRecord}s.
     *
     * @param aclRecords to be deleted
     * @throws FaultException
     */
    public void deleteAclRecords(Collection<AclRecord> aclRecords) throws FaultException
    {
        for (AclRecord aclRecord : aclRecords) {
            AclRecordDeleteRequest aclRecordDeleteRequest = new AclRecordDeleteRequest();
            aclRecordDeleteRequest.setAclRecordId(aclRecord.getId());
            createRequest(aclRecordDeleteRequest);
        }
    }

    /**
     * Create {@link AclRecord} for given parameters and also corresponding {@link AclRecord}s for child
     * entities (recursive). Each {@link AclRecord} is created only when the role make sense for it.
     *
     * @param parentAclRecordCreateRequest
     * @param userId                       of user for which the ACL is created.
     * @param entity                       entity for which the ACL is created.
     * @param role                         which is created for given user and given entity
     */
    private void createChildAclRecords(AclRecordCreateRequest parentAclRecordCreateRequest, String userId,
            PersistentObject entity, Role role)
    {
        for (Map.Entry<PersistentObject, Role> entry : getChildEntities(entity, role).entrySet()) {
            PersistentObject childEntity = entry.getKey();
            Role childRole = entry.getValue();

            // Create ACL record
            AclRecordCreateRequest aclRecordCreateRequest = new AclRecordCreateRequest();
            aclRecordCreateRequest.setUserId(userId);
            aclRecordCreateRequest.setEntityId(EntityIdentifier.formatId(childEntity));
            aclRecordCreateRequest.setRole(childRole);
            aclRecordCreateRequest.setParentCreateRequest(parentAclRecordCreateRequest);
            createRequest(aclRecordCreateRequest);

            // Recursion
            createChildAclRecords(aclRecordCreateRequest, userId, childEntity, childRole);
        }
    }

    /**
     * Create {@link AclRecord} for given parameters and also corresponding {@link AclRecord}s for child
     * entities (recursive). Each {@link AclRecord} is created only when the role make sense for it.
     *
     * @param parentAclRecordId
     * @param userId            of user for which the ACL is created.
     * @param entity            entity for which the ACL is created.
     * @param role              which is created for given user and given entity
     */
    private void createChildAclRecords(String parentAclRecordId, String userId, PersistentObject entity, Role role)
    {
        for (Map.Entry<PersistentObject, Role> entry : getChildEntities(entity, role).entrySet()) {
            PersistentObject childEntity = entry.getKey();
            Role childRole = entry.getValue();

            // Create ACL record
            AclRecordCreateRequest aclRecordCreateRequest = new AclRecordCreateRequest();
            aclRecordCreateRequest.setUserId(userId);
            aclRecordCreateRequest.setEntityId(EntityIdentifier.formatId(childEntity));
            aclRecordCreateRequest.setRole(childRole);
            aclRecordCreateRequest.setParentAclRecordId(parentAclRecordId);
            createRequest(aclRecordCreateRequest);

            // Recursion
            createChildAclRecords(aclRecordCreateRequest, userId, childEntity, childRole);
        }
    }

    /**
     * Retrieve child {@link AclRecord}s for given parameters (recursive).
     *
     * @param userId of the parent {@link AclRecord}
     * @param entity of the parent {@link AclRecord}
     * @param role   of the parent {@link AclRecord}
     * @throws FaultException
     */
    private void deleteChildAclRecords(String userId, PersistentObject entity, EntityIdentifier entityId,
            Role role) throws FaultException
    {
        for (Map.Entry<PersistentObject, Role> entry : getChildEntities(entity, role).entrySet()) {
            PersistentObject childEntity = entry.getKey();
            EntityIdentifier childEntityId = new EntityIdentifier(childEntity);
            Role childRole = entry.getValue();

            for (AclRecordCreateRequest aclRecordCreateRequest : getAclRecordCreateRequests(userId, entityId, role)) {
                removeRequest(aclRecordCreateRequest);
            }

            for (AclRecord aclRecord : authorization.getAclRecords(userId, childEntityId, childRole)) {
                AclRecordDeleteRequest aclRecordDeleteRequest = new AclRecordDeleteRequest();
                aclRecordDeleteRequest.setAclRecordId(aclRecord.getId());
                createRequest(aclRecordDeleteRequest);
            }

            // Recursion
            deleteChildAclRecords(userId, childEntity, childEntityId, childRole);
        }
    }

    private Collection<AclRecordCreateRequest> getAclRecordCreateRequests(String userId, EntityIdentifier entityId,
            Role role)
    {
        return entityManager.createQuery("SELECT request FROM AclRecordCreateRequest request"
                + " WHERE userId = :userId AND entityId = :entityId AND role = :role", AclRecordCreateRequest.class)
                .setParameter("userId", userId)
                .setParameter("entityId", entityId.toId())
                .setParameter("role", role)
                .getResultList();
    }

    private Collection<AclRecordCreateRequest> getAclRecordCreateRequests(EntityIdentifier entityId)
    {
        return entityManager.createQuery("SELECT request FROM AclRecordCreateRequest request"
                + " WHERE entityId = :entityId", AclRecordCreateRequest.class)
                .setParameter("entityId", entityId.toId())
                .getResultList();
    }

    /**
     * @param entity for which the child entities should be returned
     * @return child entities for given {@code entity}
     */
    private Map<PersistentObject, Role> getChildEntities(PersistentObject entity, Role role)
    {
        Map<PersistentObject, Role> childEntities = new HashMap<PersistentObject, Role>();
        if (entity instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) entity;
            for (ReservationRequest reservationRequest : reservationRequestSet.getReservationRequests()) {
                if (EntityType.RESERVATION_REQUEST.allowsRole(role)) {
                    childEntities.put(reservationRequest, role);
                }
            }
        }
        else if (entity instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) entity;
            Reservation reservation = reservationRequest.getReservation();
            if (reservation != null) {
                if (EntityType.EXECUTABLE.allowsRole(role)) {
                    childEntities.put(reservation, role);
                }
            }
        }
        else if (entity instanceof Reservation) {
            Reservation reservation = (Reservation) entity;

            // Child reservations
            for (Reservation childReservation : reservation.getChildReservations()) {
                if (EntityType.RESERVATION.allowsRole(role)) {
                    childEntities.put(childReservation, role);
                }
            }

            // Executable
            Executable executable = reservation.getExecutable();
            if (executable != null) {
                if (EntityType.EXECUTABLE.allowsRole(role)) {
                    childEntities.put(executable, role);
                }
            }

            // Reused reservation
            if (reservation instanceof ExistingReservation) {
                ExistingReservation existingReservation = (ExistingReservation) reservation;
                Reservation reusedReservation = existingReservation.getReservation();
                childEntities.put(reusedReservation, Role.READER);
            }
        }
        return childEntities;
    }

    /**
     * Execute all existing {@link AclRecordCreateRequest}s and {@link AclRecordDeleteRequest}s.
     */
    public void executeAclRecordRequests()
    {
        Collection<AclRecordCreateRequest> aclRecordCreateRequests = entityManager.createQuery(
                "SELECT request FROM AclRecordCreateRequest request ORDER BY id ASC", AclRecordCreateRequest.class)
                .getResultList();

        Collection<AclRecordDeleteRequest> aclRecordDeleteRequests = entityManager.createQuery(
                "SELECT request FROM AclRecordDeleteRequest request ORDER BY id ASC", AclRecordDeleteRequest.class)
                .getResultList();

        executeAclRecordCreateRequests(aclRecordCreateRequests);
        executeAclRecordDeleteRequests(aclRecordDeleteRequests);
    }

    /**
     *
     * @param aclRecordCreateRequest to be executed
     * @return {@link AclRecord} which has been created
     * @throws FaultException
     */
    public AclRecord executeAclRecordCreateRequest(AclRecordCreateRequest aclRecordCreateRequest) throws FaultException
    {
        List<AclRecordCreateRequest> aclRecordCreateRequests = new LinkedList<AclRecordCreateRequest>();
        fillAclRecordCreateRequests(aclRecordCreateRequest, aclRecordCreateRequests);

        Map<AclRecordCreateRequest, AclRecord> aclRecordByCreateRequest =
                executeAclRecordCreateRequests(aclRecordCreateRequests);
        return aclRecordByCreateRequest.get(aclRecordCreateRequest);
    }

    /**
     * Fill given {@code aclRecordCreateRequest} to given {@code aclRecordCreateRequests} (recursive).
     *
     * @param aclRecordCreateRequest to be added to given {@code aclRecordCreateRequests}
     * @param aclRecordCreateRequests to be filled
     */
    private void fillAclRecordCreateRequests(AclRecordCreateRequest aclRecordCreateRequest,
            Collection<AclRecordCreateRequest> aclRecordCreateRequests)
    {
        aclRecordCreateRequests.add(aclRecordCreateRequest);
        for (AclRecordCreateRequest childAclRecordCreateRequest : aclRecordCreateRequest.getChildCreateRequests()) {
            fillAclRecordCreateRequests(childAclRecordCreateRequest, aclRecordCreateRequests);
        }
    }

    /**
     * @param aclRecordCreateRequests to be executed
     * @return map of {@link AclRecord}s by {@link AclRecordCreateRequest} for which they have been created
     */
    private Map<AclRecordCreateRequest, AclRecord> executeAclRecordCreateRequests(
            Collection<AclRecordCreateRequest> aclRecordCreateRequests)
    {
        // Map of create requests by parent to which they are dependent
        Map<AclRecordCreateRequest, List<AclRecordCreateRequest>> createRequestsByParent =
                new HashMap<AclRecordCreateRequest, List<AclRecordCreateRequest>>();
        for (AclRecordCreateRequest createRequest : aclRecordCreateRequests) {
            AclRecordCreateRequest parentCreateRequest = createRequest.getParentCreateRequest();
            List<AclRecordCreateRequest> createRequests = createRequestsByParent.get(parentCreateRequest);
            if (createRequests == null) {
                createRequests = new LinkedList<AclRecordCreateRequest>();
                createRequestsByParent.put(parentCreateRequest, createRequests);
            }
            createRequests.add(createRequest);
        }

        // Map of created AclRecords for create requests
        Map<AclRecordCreateRequest, AclRecord> aclRecordByCreateRequest =
                new HashMap<AclRecordCreateRequest, AclRecord>();

        try {
            entityManager.getTransaction().begin();

            Collection<AclRecordCreateRequest> satisfiedParentCreateRequests = new LinkedList<AclRecordCreateRequest>();
            satisfiedParentCreateRequests.add(null);
            while (satisfiedParentCreateRequests.size() > 0) {
                AclRecordCreateRequest parentCreateRequest = satisfiedParentCreateRequests.iterator().next();
                satisfiedParentCreateRequests.remove(parentCreateRequest);

                List<AclRecordCreateRequest> createRequests = createRequestsByParent.get(parentCreateRequest);
                if (createRequests == null) {
                    continue;
                }
                createRequestsByParent.remove(parentCreateRequest);

                for (AclRecordCreateRequest createRequest : createRequests) {
                    String userId = createRequest.getUserId();
                    EntityIdentifier entityId = EntityIdentifier.parse(createRequest.getEntityId());
                    Role role = createRequest.getRole();
                    AclRecord aclRecord = authorization.createAclRecord(userId, entityId, role);

                    entityManager.remove(createRequest);

                    // Keep the created AclRecord for the create request
                    aclRecordByCreateRequest.put(createRequest, aclRecord);

                    // Determine parent ACL record identifier
                    String parentAclRecordId = createRequest.getParentAclRecordId();
                    if (parentCreateRequest != null) {
                        AclRecord parentAclRecord = aclRecordByCreateRequest.get(parentCreateRequest);
                        parentAclRecordId = parentAclRecord.getId();

                    }
                    // Create dependency to parent
                    if (parentAclRecordId != null) {
                        AclRecordDependencyId id = new AclRecordDependencyId(parentAclRecordId, aclRecord.getId());
                        AclRecordDependency aclRecordDependency = entityManager.find(AclRecordDependency.class, id);
                        if (aclRecordDependency == null) {
                            logger.debug("Creating ACL dependency (parent: {}, child: {})",
                                    parentAclRecordId, aclRecord.getId());
                            aclRecordDependency = new AclRecordDependency();
                            aclRecordDependency.setId(id);
                            entityManager.persist(aclRecordDependency);
                        }
                        else {
                            logger.debug("ACL dependency already exists (parent: {}, child: {})",
                                    parentAclRecordId, aclRecord.getId());
                        }
                    }

                    // Child create requests can now be also created
                    satisfiedParentCreateRequests.add(createRequest);
                }
            }

            // No requests should remain
            if (createRequestsByParent.size() > 0 ) {
                throw new IllegalStateException("Cycle detected in ACL record create requests.");
            }

            entityManager.getTransaction().commit();
        }
        catch (FaultException exception) {
            InternalErrorHandler.handle(InternalErrorType.AUTHORIZATION, "Creating ACL records", exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
        }
        return aclRecordByCreateRequest;
    }

    /**
     * @param aclRecordDeleteRequests to be executed
     */
    private void executeAclRecordDeleteRequests(Collection<AclRecordDeleteRequest> aclRecordDeleteRequests)
    {
        if(aclRecordDeleteRequests.size() == 0) {
            return;
        }
        try {
            entityManager.getTransaction().begin();

            // Map of parent ACL record count by child identifier
            Map<String, Integer> parentAclRecordCountByChildId = new HashMap<String, Integer>();

            // Set of ACL records which can be deleted
            Set<String> aclRecordIds = new HashSet<String>();

            // Build set and initialize map
            for (AclRecordDeleteRequest aclRecordDeleteRequest : aclRecordDeleteRequests) {
                String aclRecordId = aclRecordDeleteRequest.getAclRecordId();
                aclRecordIds.add(aclRecordId);
                parentAclRecordCountByChildId.put(aclRecordId, 0);
                entityManager.remove(aclRecordDeleteRequest);
            }

            // Get dependencies for ACL records
            Collection<AclRecordDependency> aclRecordDependencies = entityManager.createQuery(
                    "SELECT dependency FROM AclRecordDependency dependency WHERE dependency.id.childAclRecordId IN(:ids)",
                    AclRecordDependency.class)
                    .setParameter("ids", aclRecordIds)
                    .getResultList();

            // Map of ACL record identifiers by parent identifiers to which they are dependent
            Map<String, List<String>> childAclRecordIdsByParentId = new HashMap<String, List<String>>();

            // Build maps
            for (AclRecordDependency aclRecordDependency : aclRecordDependencies) {
                String parentAclRecordId = aclRecordDependency.getParentAclRecordId();

                // Add child ACL record to parent collection
                List<String> childAclRecordIds = childAclRecordIdsByParentId.get(parentAclRecordId);
                if (childAclRecordIds == null) {
                    childAclRecordIds = new LinkedList<String>();
                    childAclRecordIdsByParentId.put(parentAclRecordId, childAclRecordIds);
                }
                String childAclRecordId = aclRecordDependency.getChildAclRecordId();
                childAclRecordIds.add(childAclRecordId);

                // Update parent count for the child ACL record
                Integer parentCount = parentAclRecordCountByChildId.get(childAclRecordId);
                parentAclRecordCountByChildId.put(childAclRecordId, parentCount + 1);

                // Child ACL record has parent so it can't be deleted now
                aclRecordIds.remove(childAclRecordId);
            }

            // Delete all ACL records which can be deleted
            while (aclRecordIds.size() > 0) {
                String aclRecordId = aclRecordIds.iterator().next();
                aclRecordIds.remove(aclRecordId);

                // Delete the ACL record
                AclRecord aclRecord = authorization.getAclRecord(aclRecordId);
                authorization.deleteAclRecord(aclRecord);

                // Delete all dependencies
                entityManager.createQuery(
                        "DELETE FROM AclRecordDependency dependency WHERE dependency.id.parentAclRecordId = :id")
                        .setParameter("id", aclRecordId)
                        .executeUpdate();

                // Add all child ACL records which depends only to the deleted ACL record as deletable or decrease
                // it's parent record count
                List<String> childAclRecordIds = childAclRecordIdsByParentId.get(aclRecordId);
                if (childAclRecordIds != null) {
                    for (String childAclRecordId : childAclRecordIds) {
                        Integer parentCount = parentAclRecordCountByChildId.get(childAclRecordId);
                        if (parentCount == 1) {
                            aclRecordIds.add(childAclRecordId);
                        }
                        else {
                            parentAclRecordCountByChildId.put(childAclRecordId, parentCount - 1);
                        }
                    }
                    childAclRecordIdsByParentId.remove(aclRecordId);
                }
            }

            // Log all ACL records which cannot be deleted due to parent dependencies
            for (String parentAclRecordId : childAclRecordIdsByParentId.keySet()) {
                for (String childAclRecordId : childAclRecordIdsByParentId.get(parentAclRecordId)) {
                    logger.debug("ACL (id: {}) cannot be deleted because is referenced by parent ACL (id: {}).",
                            childAclRecordId, parentAclRecordId);
                }
            }
            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            InternalErrorHandler.handle(InternalErrorType.AUTHORIZATION, "Deleting ACL records", exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
        }
    }
}
