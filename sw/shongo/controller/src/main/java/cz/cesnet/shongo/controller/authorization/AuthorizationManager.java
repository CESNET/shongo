package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link AbstractManager} for managing {@link AclRecord}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationManager extends AbstractManager
{
    /**
     * @see Transaction
     */
    private Transaction activeTransaction = null;

    /**
     * @see Authorization
     */
    private final Authorization authorization;

    /**
     * @see UserSettingsManager
     */
    private UserSettingsManager userSettingsManager = null;

    /**
     * Constructor.
     *
     * @param entityManager sets the {@link #entityManager}
     */
    public AuthorizationManager(EntityManager entityManager, Authorization authorization)
    {
        super(entityManager);

        this.authorization = authorization;
    }

    /**
     * @return {@link #entityManager}
     */
    public EntityManager getEntityManager()
    {
        return entityManager;
    }

    /**
     * @return {@link #userSettingsManager}
     */
    public UserSettingsManager getUserSettingsManager()
    {
        if (userSettingsManager == null) {
            userSettingsManager = new UserSettingsManager(entityManager, authorization);
        }
        return userSettingsManager;
    }

    /**
     * @param userId
     * @param entity
     * @param role
     * @return collection of {@link AclRecord} for given parameters
     */
    public Collection<AclRecord> listAclRecords(String userId, PersistentObject entity, Role role)
    {
        AclRecord.EntityId entityId = null;
        if (entity != null) {
            entityId = new AclRecord.EntityId(entity);
        }
        return entityManager.createQuery("SELECT acl FROM AclRecord acl"
                + " WHERE (:userId IS NULL OR acl.userId = :userId)"
                + " AND (:entityTypeNull = TRUE OR acl.entityId.entityType = :entityType)"
                + " AND (:entityId IS NULL OR acl.entityId.persistenceId = :entityId)"
                + " AND (:roleNull = TRUE OR acl.role = :role)", AclRecord.class)
                .setParameter("userId", userId)
                .setParameter("entityTypeNull", entityId == null || entityId.getEntityType() == null)
                .setParameter("entityType", (entityId != null ? entityId.getEntityType() : null))
                .setParameter("entityId", (entityId != null ? entityId.getPersistenceId() : null))
                .setParameter("roleNull", role == null)
                .setParameter("role", role)
                .getResultList();
    }

    /**
     * @param userId
     * @return collection of {@link AclRecord} for given {@code userId}
     */
    public Collection<AclRecord> listAclRecords(String userId)
    {
        return entityManager.createQuery("SELECT acl FROM AclRecord acl"
                + " WHERE acl.userId = :userId", AclRecord.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * @param entityId
     * @return collection of {@link AclRecord} for given {@code entityId}
     */
    public Collection<AclRecord> listAclRecords(AclRecord.EntityId entityId)
    {
        return entityManager.createQuery("SELECT acl FROM AclRecord acl"
                + " WHERE acl.entityId.entityType = :entityType"
                + " AND acl.entityId.persistenceId = :entityId", AclRecord.class)
                .setParameter("entityType", entityId.getEntityType())
                .setParameter("entityId", entityId.getPersistenceId())
                .getResultList();
    }

    /**
     * @param aclRecordId
     * @return {@link AclRecord} with given {@code aclRecordId}
     * @throws cz.cesnet.shongo.CommonReportSet.EntityNotExistsException
     *          when {@link AclRecord} doesn't exist
     */
    public AclRecord getAclRecord(Long aclRecordId) throws CommonReportSet.EntityNotExistsException
    {
        AclRecord aclRecord = entityManager.find(AclRecord.class, aclRecordId);
        if (aclRecord == null) {
            return ControllerReportSetHelper.throwEntityNotExistFault(AclRecord.class, aclRecordId);
        }
        return aclRecord;
    }

    /**
     * @param userId
     * @param entityId
     * @param role
     * @return collection of {@link AclRecord} for given parameters
     */
    public AclRecord getAclRecord(String userId, AclRecord.EntityId entityId, Role role)
    {
        List<AclRecord> aclRecords = entityManager.createQuery("SELECT acl FROM AclRecord acl"
                + " WHERE acl.userId = :userId"
                + " AND acl.entityId.entityType = :entityType"
                + " AND acl.entityId.persistenceId = :entityId"
                + " AND acl.role = :role", AclRecord.class)
                .setParameter("userId", userId)
                .setParameter("entityType", entityId.getEntityType())
                .setParameter("entityId", entityId.getPersistenceId())
                .setParameter("role", role)
                .getResultList();
        if (aclRecords.size() == 1) {
            return aclRecords.get(0);
        }
        else if (aclRecords.size() == 0) {
            return null;
        }
        else {
            throw new RuntimeException(
                    String.format("Multiple ACL (user: %s, entity: %s, role: %s) exist.", userId, entityId, role));
        }
    }

    /**
     * @param entity
     * @param role
     * @return list of user-ids with given {@code role} for given {@code entityId}
     */
    public Collection<String> getUserIdsWithRole(PersistentObject entity, Role role)
    {
        AclRecord.EntityId entityId = new AclRecord.EntityId(entity);
        return entityManager.createQuery("SELECT acl.userId FROM AclRecord acl"
                + " WHERE acl.entityId.entityType = :entityType"
                + " AND acl.entityId.persistenceId = :entityId"
                + " AND acl.role = :role", String.class)
                .setParameter("entityType", entityId.getEntityType())
                .setParameter("entityId", entityId.getPersistenceId())
                .setParameter("role", role)
                .getResultList();
    }

    /**
     * @param userId
     * @return {@link UserInformation} for given {@code userId}
     */
    public UserInformation getUserInformation(String userId)
    {
        if (authorization == null) {
            throw new IllegalArgumentException("Authorization must not be null.");
        }
        return authorization.getUserInformation(userId);
    }

    /**
     * Start new transaction for given {@code authorization} (for ACL cache).
     */
    public void beginTransaction()
    {
        if (activeTransaction != null) {
            throw new IllegalStateException("Another transaction is already active.");
        }
        activeTransaction = new Transaction();
    }

    /**
     * @return true whether any transaction is active,
     *         false otherwise
     */
    public boolean isTransactionActive()
    {
        return activeTransaction != null;
    }

    /**
     * Apply changes made during the active transaction (to ACL cache).
     */
    public void commitTransaction()
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }
        activeTransaction.commit();
        activeTransaction.destroy();
        activeTransaction = null;
    }

    /**
     * Revert changes made during the active transaction.
     */
    public void rollbackTransaction()
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }
        activeTransaction.destroy();
        activeTransaction = null;
    }

    /**
     * Create a new {@link AclRecord}.
     *
     * @param userId   of user for which the ACL is created.
     * @param entity   for which the ACL is created.
     * @param role     which is created for given user and given entity
     * @return new {@link AclRecord}
     */
    public AclRecord createAclRecord(String userId, PersistentObject entity, Role role)
    {
        if (userId.equals(Authorization.ROOT_USER_ID)) {
            return null;
        }

        AclRecord.EntityId entityId = new AclRecord.EntityId(entity);
        AclRecord.EntityType entityType = entityId.getEntityType();

        if (!entityType.getEntityType().allowsRole(role)) {
            throw new ControllerReportSet.AclInvalidRoleException(EntityIdentifier.formatId(entity), role.toString());
        }

        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclRecord aclRecord = activeTransaction.getAclRecord(userId, entityId, role);
        if (aclRecord != null) {
            return aclRecord;
        }

        try {
            aclRecord = new AclRecord();
            aclRecord.setUserId(userId);
            aclRecord.setEntityId(entityId);
            aclRecord.setRole(role);
            entityManager.persist(aclRecord);

            activeTransaction.addAclRecord(aclRecord);

            afterAclRecordCreated(aclRecord, entity);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(String.format("ACL Record creation failed (user: %s, entity: %s, role: %s)",
                    userId, entityId, role), throwable);
        }

        Controller.loggerAcl.info("ACL Record created (id: {}, user: {}, entity: {}, role: {})",
                new Object[]{aclRecord.getId(), userId, entityId, role});

        return aclRecord;
    }

    /**
     * Creates all {@link AclRecord}s from given {@code parentEntity} to given {@code childEntity} which the
     * {@code childEntity} allows.
     *
     * @param parentEntity from which should be fetch all existing {@link AclRecord}s
     * @param childEntity  to which should be created new {@link AclRecord}s
     */
    public void createAclRecordsForChildEntity(PersistentObject parentEntity, PersistentObject childEntity)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclRecord.EntityId parentEntityId = new AclRecord.EntityId(parentEntity);
        AclRecord.EntityId childEntityId = new AclRecord.EntityId(childEntity);
        EntityType childEntityType = childEntityId.getEntityType().getEntityType();
        Collection<AclRecord> parentAclRecords = activeTransaction.getAclRecords(parentEntityId);
        for (AclRecord parentAclRecord : parentAclRecords) {
            String userId = parentAclRecord.getUserId();
            Role role = parentAclRecord.getRole();
            if (childEntityType.allowsRole(role)) {
                createChildAclRecord(parentAclRecord, userId, childEntity, role,
                        AclRecordDependency.Type.DELETE_DETACH);
            }
        }
    }

    /**
     * Creates all {@link AclRecord}s from given {@code parentEntity} to given {@code childEntity} which the
     * {@code childEntity} allows.
     *
     * @param parentEntity from which should be fetch all existing {@link AclRecord}s and distributed to all childs
     */
    public void updateAclRecordsForChildEntities(PersistentObject parentEntity)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclRecord.EntityId parentEntityId = new AclRecord.EntityId(parentEntity);
        Collection<AclRecord> parentAclRecords = activeTransaction.getAclRecords(parentEntityId);
        for (AclRecord parentAclRecord : parentAclRecords) {
            afterAclRecordCreated(parentAclRecord, parentEntity);
        }
    }

    /**
     * Create a new child {@link AclRecord}.
     *
     * @param parentAclRecord
     * @param userId
     * @param childEntity
     * @param role
     */
    private void createChildAclRecord(AclRecord parentAclRecord, String userId, PersistentObject childEntity,
            Role role, AclRecordDependency.Type dependencyType)
    {
        AclRecord childAclRecord = createAclRecord(userId, childEntity, role);

        List<AclRecordDependency> aclRecordDependencies =
                entityManager.createQuery("SELECT dependency FROM AclRecordDependency dependency"
                        + " WHERE dependency.parentAclRecord = :parent AND dependency.childAclRecord = :child",
                        AclRecordDependency.class)
                        .setParameter("parent", parentAclRecord)
                        .setParameter("child", childAclRecord)
                        .getResultList();
        if (aclRecordDependencies.size() == 0) {
            AclRecordDependency aclRecordDependency = new AclRecordDependency();
            aclRecordDependency.setParentAclRecord(parentAclRecord);
            aclRecordDependency.setChildAclRecord(childAclRecord);
            aclRecordDependency.setType(dependencyType);
            entityManager.persist(aclRecordDependency);

            Controller.loggerAcl.info("Created ACL Dependency (parent: {}, child: {}, type: {})",
                    new Object[]{parentAclRecord.getId(), childAclRecord.getId(), dependencyType});
        }
    }

    /**
     * Delete given {@code aclRecord} and all corresponding {@link AclRecord}s from child entities.
     *
     * @param aclRecord
     */
    public void deleteAclRecord(AclRecord aclRecord)
    {
        deleteAclRecord(aclRecord, false);
    }

    /**
     * Delete all {@link AclRecord} for given {@code entity}.
     *
     * @param entity
     */
    public void deleteAclRecordsForEntity(PersistentObject entity)
    {
        AclRecord.EntityId entityId = new AclRecord.EntityId(entity);
        for (AclRecord aclRecord : activeTransaction.getAclRecords(entityId)) {
            deleteAclRecord(aclRecord, true);
        }
    }

    /**
     * Delete given {@code aclRecord},
     *
     * @param aclRecord
     * @param detachChildren
     */
    private void deleteAclRecord(AclRecord aclRecord, boolean detachChildren)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclRecord.EntityId entityId = aclRecord.getEntityId();
        PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
        if (entity == null) {
            throw new IllegalStateException("Entity " + entityId.toString() + " referenced from ACL doesn't exist.");
        }

        // Delete ACL record dependencies
        Collection<AclRecordDependency> parentAclRecordDependencies = entityManager.createQuery(
                "SELECT dependency FROM AclRecordDependency dependency"
                        + " WHERE dependency.childAclRecord = :aclRecord", AclRecordDependency.class)
                .setParameter("aclRecord", aclRecord)
                .getResultList();
        if (parentAclRecordDependencies.size() > 0) {
            if (detachChildren) {
                for (AclRecordDependency aclRecordDependency : parentAclRecordDependencies) {
                    entityManager.remove(aclRecordDependency);
                }
            }
            else {
                ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(AclRecord.class, aclRecord.getId());
            }
        }

        // Refresh record
        aclRecord = entityManager.merge(aclRecord);

        // Delete ACL record dependencies
        Collection<AclRecordDependency> childAclRecordDependencies = entityManager.createQuery(
                "SELECT dependency FROM AclRecordDependency dependency"
                        + " WHERE dependency.parentAclRecord = :aclRecord", AclRecordDependency.class)
                .setParameter("aclRecord", aclRecord)
                .getResultList();
        for (AclRecordDependency aclRecordDependency : childAclRecordDependencies) {
            entityManager.remove(aclRecordDependency);
            if (!aclRecordDependency.getType().equals(AclRecordDependency.Type.DELETE_DETACH) || !detachChildren) {
                AclRecord childAclRecord = aclRecordDependency.getChildAclRecord();
                try {
                    deleteAclRecord(childAclRecord);
                }
                catch (CommonReportSet.EntityNotDeletableReferencedException exception) {
                    Controller.loggerAcl.info(
                            "ACL Record (id: {}, user: {}, entity: {}, role: {}) cannot be deleted,"
                                    + " because it is referenced.", new Object[]{childAclRecord.getId(),
                            childAclRecord.getUserId(), childAclRecord.getEntityId(), childAclRecord.getRole()
                    });
                }
            }
        }

        // Delete ACL record
        beforeAclRecordDeleted(aclRecord, entity);
        entityManager.remove(aclRecord);
        activeTransaction.removeAclRecord(aclRecord);

        Controller.loggerAcl.info("Deleted ACL Record (id: {}, user: {}, entity: {}, role: {})",
                new Object[]{aclRecord.getId(), aclRecord.getUserId(), entityId, aclRecord.getRole()});
    }

    /**
     * Method which is called after new {@link AclRecord} is created.
     *
     * @param aclRecord
     * @param entity
     */
    private void afterAclRecordCreated(AclRecord aclRecord, PersistentObject entity)
    {
        String userId = aclRecord.getUserId();
        Role role = aclRecord.getRole();

        // Create child ACL records
        if (entity instanceof AbstractReservationRequest) {
            AbstractReservationRequest reservationRequest = (AbstractReservationRequest) entity;
            Allocation allocation = reservationRequest.getAllocation();

            // Child reservation requests
            for (ReservationRequest childReservationRequest : allocation.getChildReservationRequests()) {
                if (EntityType.RESERVATION_REQUEST.allowsRole(role)) {
                    createChildAclRecord(aclRecord, userId, childReservationRequest, role,
                            AclRecordDependency.Type.DELETE_DETACH);
                }
            }

            // Reservation requests which reuse this reservation request
            if (reservationRequest.getReusement().equals(ReservationRequestReusement.OWNED)) {
                ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
                List<AbstractReservationRequest> reservationRequestUsages =
                        reservationRequestManager.listReservationRequestActiveUsages(reservationRequest);
                for (AbstractReservationRequest reservationRequestUsage : reservationRequestUsages) {
                    if (EntityType.RESERVATION_REQUEST.allowsRole(role)) {
                        createChildAclRecord(aclRecord, userId, reservationRequestUsage, role,
                                AclRecordDependency.Type.DELETE_DETACH);
                    }
                }
            }

            // Allocated reservations
            if (EntityType.RESERVATION.allowsRole(role)) {
                for (Reservation reservation : allocation.getReservations()) {
                    createChildAclRecord(aclRecord, userId, reservation, role, AclRecordDependency.Type.DELETE_DETACH);
                }
            }
            else if (role.equals(Role.RESERVATION_REQUEST_USER)) {
                for (Reservation reservation : allocation.getReservations()) {
                    createChildAclRecord(aclRecord, userId, reservation, Role.READER, AclRecordDependency.Type.DELETE_DETACH);
                }
            }
        }
        else if (entity instanceof Reservation) {
            Reservation reservation = (Reservation) entity;

            // Child reservations
            for (Reservation childReservation : reservation.getChildReservations()) {
                if (EntityType.RESERVATION.allowsRole(role)) {
                    createChildAclRecord(aclRecord, userId, childReservation, role,
                            AclRecordDependency.Type.DELETE_DETACH);
                }
            }

            // Executable
            Executable executable = reservation.getExecutable();
            if (reservation.getExecutable() != null) {
                if (EntityType.EXECUTABLE.allowsRole(role)) {
                    createChildAclRecord(aclRecord, userId, executable, role, AclRecordDependency.Type.DELETE_DETACH);
                }
            }

            // Reused reservation
            if (reservation instanceof ExistingReservation) {
                ExistingReservation existingReservation = (ExistingReservation) reservation;
                Reservation reusedReservation = existingReservation.getReservation();
                createChildAclRecord(aclRecord, userId, reusedReservation, Role.READER,
                        AclRecordDependency.Type.DELETE_CASCADE);
            }
        }

        // Update entities
        if (entity instanceof Executable && role.equals(Role.OWNER)) {
            Executable executable = (Executable) entity;
            Executable.State state = executable.getState();
            if (state.equals(Executable.State.STARTED)) {
                executable.setState(Executable.State.MODIFIED);
            }
        }
    }

    /**
     * Method which is called after existing {@link AclRecord} is deleted.
     *
     * @param aclRecord
     * @param entity
     */
    private void beforeAclRecordDeleted(AclRecord aclRecord, PersistentObject entity)
    {
        Role role = aclRecord.getRole();

        if (entity instanceof Executable && role.equals(Role.OWNER)) {
            Executable executable = (Executable) entity;
            Executable.State state = executable.getState();
            if (state.equals(Executable.State.STARTED)) {
                executable.setState(Executable.State.MODIFIED);
            }
        }
    }

    /**
     * Represents a transaction for the {@link AuthorizationManager}.
     */
    private class Transaction
    {
        /**
         * Set of {@link AclRecord} which should be added to the {@link Authorization} cache.
         */
        private Set<AclRecord> addedAclRecords = new HashSet<AclRecord>();

        /**
         * Set of {@link AclRecord} which should be removed from the {@link Authorization} cache.
         */
        private Set<AclRecord> removedAclRecords = new HashSet<AclRecord>();

        /**
         * Constructor.
         */
        public Transaction()
        {
        }

        /**
         * Remove all recorded changes.
         */
        public void destroy()
        {
            addedAclRecords.clear();
            removedAclRecords.clear();
        }

        /**
         * Apply recorded changes.
         */
        public void commit()
        {
            if (authorization == null) {
                throw new IllegalArgumentException("Authorization must not be null.");
            }
            synchronized (authorization) {
                for (AclRecord aclRecord : removedAclRecords) {
                    authorization.removeAclRecordFromCache(aclRecord);
                }
                for (AclRecord aclRecord : addedAclRecords) {
                    authorization.addAclRecordToCache(aclRecord);
                }
            }
        }

        /**
         * @param aclRecord to be added to the {@link Transaction}
         */
        public void addAclRecord(AclRecord aclRecord)
        {
            if (!removedAclRecords.remove(aclRecord)) {
                addedAclRecords.add(aclRecord);
            }
        }

        /**
         * @param aclRecord to be added to the {@link Transaction}
         */
        public void updateAclRecord(AclRecord aclRecord)
        {
            removedAclRecords.add(aclRecord);
            addedAclRecords.add(aclRecord);
        }

        /**
         * @param aclRecord to be removed from the {@link Transaction}
         */
        public void removeAclRecord(AclRecord aclRecord)
        {
            if (!addedAclRecords.remove(aclRecord)) {
                removedAclRecords.add(aclRecord);
            }
        }

        /**
         * @param entityId
         * @return collection of {@link AclRecord}s for given {@code entityId}
         */
        public Collection<AclRecord> getAclRecords(AclRecord.EntityId entityId)
        {
            Set<AclRecord> aclRecords = new HashSet<AclRecord>();
            aclRecords.addAll(listAclRecords(entityId));
            for (AclRecord aclRecord : addedAclRecords) {
                if (entityId.equals(aclRecord.getEntityId())) {
                    aclRecords.add(aclRecord);
                }
            }
            for (AclRecord aclRecord : removedAclRecords) {
                if (entityId.equals(aclRecord.getEntityId())) {
                    aclRecords.remove(aclRecord);
                }
            }
            return aclRecords;
        }

        /**
         * @param userId
         * @param entityId
         * @param role
         * @return {@link AclRecord} for given parameters or null if doesn't exist
         */
        public AclRecord getAclRecord(String userId, AclRecord.EntityId entityId, Role role)
        {
            AclRecord aclRecord = AuthorizationManager.this.getAclRecord(userId, entityId, role);
            if (aclRecord == null) {
                // If the ACL record is added in the transaction, return it
                for (AclRecord addedAclRecord : addedAclRecords) {
                    if (!userId.equals(addedAclRecord.getUserId())) {
                        continue;
                    }
                    if (!entityId.equals(addedAclRecord.getEntityId())) {
                        continue;
                    }
                    if (!role.equals(addedAclRecord.getRole())) {
                        continue;
                    }
                    return addedAclRecord;
                }
            }
            else {
                // If the ACL record is removed in the transaction, return null
                if (removedAclRecords.contains(aclRecord)) {
                    return null;
                }
            }
            return aclRecord;
        }
    }
}
