package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.acl.*;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.EntityTypeResolver;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link AbstractManager} for managing {@link AclEntry}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationManager extends AclEntryManager
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
        super(entityManager, authorization != null ? authorization.getAclProvider() : null);

        this.authorization = authorization;
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
     * @param object
     * @param entityRole
     * @return list of user-ids with given {@code role} for given {@code object}
     */
    public Set<String> getUserIdsWithRole(PersistentObject object, EntityRole entityRole)
    {
        AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(object);
        List<AclEntry> entries = entityManager.createNamedQuery("AclEntry.findByObjectIdentityAndRole", AclEntry.class)
                .setParameter("objectIdentity", objectIdentity)
                .setParameter("role", entityRole.toString())
                .getResultList();
        Set<String> userIds = new HashSet<String>();
        for (AclEntry entry : entries) {
            userIds.addAll(authorization.getUserIds(entry.getIdentity()));
        }
        return userIds;
    }

    /**
     * Create a new {@link AclEntry}.
     *
     * @param identityType        for which the ACL is created.
     * @param identityPrincipalId for which the ACL is created.
     * @param object              for which the ACL is created.
     * @param entityRole          which is created for given user and given {@code object}
     * @return new {@link AclEntry}
     */
    public AclEntry createAclRecord(AclIdentityType identityType, String identityPrincipalId, PersistentObject object,
            EntityRole entityRole)
    {
        AclIdentity identity = aclProvider.getIdentity(identityType, identityPrincipalId);
        return createAclRecord(identity, object, entityRole);
    }

    /**
     * Create a new {@link AclEntry}.
     *
     * @param identity   for which the ACL is created.
     * @param object     for which the ACL is created.
     * @param entityRole which is created for given user and given {@code object}
     * @return new {@link AclEntry}
     */
    public AclEntry createAclRecord(AclIdentity identity, PersistentObject object, EntityRole entityRole)
    {
        if (identity.getType().equals(AclIdentityType.USER) &&
                identity.getPrincipalId().equals(Authorization.ROOT_USER_ID)) {
            return null;
        }

        AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(object);
        EntityType entityType = EntityTypeResolver.getEntityType(objectIdentity);
        if (!entityType.allowsRole(entityRole)) {
            throw new ControllerReportSet.AclInvalidRoleException(EntityIdentifier.formatId(object),
                    entityRole.toString());
        }

        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclEntry aclEntry = activeTransaction.getAclRecord(identity, objectIdentity, entityRole.toString());
        if (aclEntry != null) {
            return aclEntry;
        }

        try {
            aclEntry = super.createEntry(identity, objectIdentity, entityRole.toString());

            activeTransaction.addAclRecord(aclEntry);

            afterAclRecordCreated(aclEntry, object);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(String.format("ACL Record creation failed (identity: %s, entity: %s, role: %s)",
                    identity, objectIdentity, entityRole), throwable);
        }

        Controller.loggerAcl.info("ACL Record created (id: {}, identity: {}, entity: {}, role: {})",
                new Object[]{aclEntry.getId(), identity, objectIdentity, entityRole});

        return aclEntry;
    }

    /**
     * Creates all {@link AclEntry}s from given {@code parentEntity} to given {@code childEntity} which the
     * {@code childEntity} allows.
     *
     * @param parentEntity from which should be fetch all existing {@link AclEntry}s
     * @param childEntity  to which should be created new {@link AclEntry}s
     */
    public void createAclRecordsForChildEntity(PersistentObject parentEntity, PersistentObject childEntity)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclObjectIdentity parentObjectIdentity = aclProvider.getObjectIdentity(parentEntity);
        AclObjectIdentity childObjectIdentity = aclProvider.getObjectIdentity(childEntity);
        EntityType childEntityType = EntityTypeResolver.getEntityType(childObjectIdentity);
        for (AclEntry parentAclEntry : activeTransaction.getAclRecords(parentObjectIdentity)) {
            AclIdentity identity = parentAclEntry.getIdentity();
            EntityRole entityRole = EntityRole.valueOf(parentAclEntry.getRole());
            if (childEntityType.allowsRole(entityRole)) {
                createChildAclRecord(parentAclEntry, identity, childEntity, entityRole,
                        AclEntryDependency.Type.DELETE_DETACH);
            }
        }
    }

    /**
     * Creates all {@link AclEntry}s from given {@code parentEntity} to given {@code childEntity} which the
     * {@code childEntity} allows.
     *
     * @param parentEntity from which should be fetch all existing {@link AclEntry}s and distributed to all childs
     */
    public void updateAclRecordsForChildEntities(PersistentObject parentEntity)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclObjectIdentity parentObjectIdentity = aclProvider.getObjectIdentity(parentEntity);
        Collection<AclEntry> parentAclEntries = activeTransaction.getAclRecords(parentObjectIdentity);
        for (AclEntry parentAclEntry : parentAclEntries) {
            afterAclRecordCreated(parentAclEntry, parentEntity);
        }
    }

    /**
     * Create a new child {@link AclEntry}.
     *
     * @param parentAclEntry
     * @param identity
     * @param childEntity
     * @param entityRole
     */
    private void createChildAclRecord(AclEntry parentAclEntry, AclIdentity identity, PersistentObject childEntity,
            EntityRole entityRole, AclEntryDependency.Type dependencyType)
    {
        AclEntry childAclEntry = createAclRecord(identity, childEntity, entityRole);

        List<AclEntryDependency> aclRecordDependencies =
                entityManager.createQuery("SELECT dependency FROM AclEntryDependency dependency"
                        + " WHERE dependency.parentAclEntry = :parent AND dependency.childAclEntry = :child",
                        AclEntryDependency.class)
                        .setParameter("parent", parentAclEntry)
                        .setParameter("child", childAclEntry)
                        .getResultList();
        if (aclRecordDependencies.size() == 0) {
            AclEntryDependency aclEntryDependency = new AclEntryDependency();
            aclEntryDependency.setParentAclEntry(parentAclEntry);
            aclEntryDependency.setChildAclEntry(childAclEntry);
            aclEntryDependency.setType(dependencyType);
            entityManager.persist(aclEntryDependency);

            Controller.loggerAcl.info("Created ACL Dependency (parent: {}, child: {}, type: {})",
                    new Object[]{parentAclEntry.getId(), childAclEntry.getId(), dependencyType});
        }
    }

    /**
     * Delete given {@code aclEntry} and all corresponding {@link AclEntry}s from child entities.
     *
     * @param aclEntry
     */
    public void deleteAclRecord(AclEntry aclEntry)
    {
        deleteAclRecord(aclEntry, false);
    }

    /**
     * Delete all {@link AclEntry}s for given {@code object}.
     *
     * @param object
     */
    public void deleteAclRecordsForEntity(PersistentObject object)
    {
        AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(object);
        for (AclEntry aclEntry : activeTransaction.getAclRecords(objectIdentity)) {
            deleteAclRecord(aclEntry, true);
        }
    }

    /**
     * Delete given {@code aclEntry},
     *
     * @param aclEntry
     * @param detachChildren
     */
    private void deleteAclRecord(AclEntry aclEntry, boolean detachChildren)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclObjectIdentity objectIdentity = aclEntry.getObjectIdentity();
        Class<? extends PersistentObject> objectClass = EntityTypeResolver.getEntityTypeClass(objectIdentity);
        if (objectClass.equals(AbstractReservationRequest.class)) {
            objectClass = Allocation.class;
        }
        PersistentObject entity = entityManager.find(objectClass, objectIdentity.getObjectId());
        if (entity == null) {
            throw new IllegalStateException("Entity " + objectIdentity + " referenced from ACL doesn't exist.");
        }

        // Delete ACL record dependencies
        Collection<AclEntryDependency> parentAclRecordDependencies = entityManager.createQuery(
                "SELECT dependency FROM AclEntryDependency dependency"
                        + " WHERE dependency.childAclEntry = :aclRecord", AclEntryDependency.class)
                .setParameter("aclRecord", aclEntry)
                .getResultList();
        if (parentAclRecordDependencies.size() > 0) {
            if (detachChildren) {
                for (AclEntryDependency aclEntryDependency : parentAclRecordDependencies) {
                    entityManager.remove(aclEntryDependency);
                }
            }
            else {
                ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(AclEntry.class, aclEntry.getId());
            }
        }

        // Refresh record
        aclEntry = entityManager.merge(aclEntry);

        // Delete ACL record dependencies
        Collection<AclEntryDependency> childAclRecordDependencies = entityManager.createQuery(
                "SELECT dependency FROM AclEntryDependency dependency"
                        + " WHERE dependency.parentAclEntry = :aclRecord", AclEntryDependency.class)
                .setParameter("aclRecord", aclEntry)
                .getResultList();
        for (AclEntryDependency aclEntryDependency : childAclRecordDependencies) {
            entityManager.remove(aclEntryDependency);
            if (!aclEntryDependency.getType().equals(AclEntryDependency.Type.DELETE_DETACH) || !detachChildren) {
                AclEntry childAclEntry = aclEntryDependency.getChildAclEntry();
                try {
                    deleteAclRecord(childAclEntry);
                }
                catch (CommonReportSet.EntityNotDeletableReferencedException exception) {
                    Controller.loggerAcl.info(
                            "ACL Record (id: {}, identity: {}, entity: {}, role: {}) cannot be deleted,"
                                    + " because it is referenced.", new Object[]{childAclEntry.getId(),
                            childAclEntry.getIdentity(), childAclEntry.getObjectIdentity(), childAclEntry.getRole()
                    });
                }
            }
        }

        // Delete ACL record
        beforeAclRecordDeleted(aclEntry, entity);
        super.deleteEntry(aclEntry);
        activeTransaction.removeAclRecord(aclEntry);

        Controller.loggerAcl.info("Deleted ACL Record (id: {}, identity: {}, entity: {}, role: {})",
                new Object[]{aclEntry.getId(), aclEntry.getIdentity(), objectIdentity, aclEntry.getRole()});
    }

    /**
     * Method which is called after new {@link AclEntry} is created.
     *
     * @param aclEntry
     * @param entity
     */
    private void afterAclRecordCreated(AclEntry aclEntry, PersistentObject entity)
    {
        AclIdentity identity = aclEntry.getIdentity();
        EntityRole entityRole = EntityRole.valueOf(aclEntry.getRole());

        // Create child ACL records
        if (entity instanceof AbstractReservationRequest) {
            AbstractReservationRequest reservationRequest = (AbstractReservationRequest) entity;
            Allocation allocation = reservationRequest.getAllocation();

            // Child reservation requests
            for (ReservationRequest childReservationRequest : allocation.getChildReservationRequests()) {
                if (EntityType.RESERVATION_REQUEST.allowsRole(entityRole)) {
                    createChildAclRecord(aclEntry, identity, childReservationRequest, entityRole,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }

            // Reservation requests which reuse this reservation request
            if (reservationRequest.getReusement().equals(ReservationRequestReusement.OWNED)) {
                ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
                List<AbstractReservationRequest> reservationRequestUsages =
                        reservationRequestManager.listReservationRequestActiveUsages(reservationRequest);
                for (AbstractReservationRequest reservationRequestUsage : reservationRequestUsages) {
                    if (EntityType.RESERVATION_REQUEST.allowsRole(entityRole)) {
                        createChildAclRecord(aclEntry, identity, reservationRequestUsage, entityRole,
                                AclEntryDependency.Type.DELETE_DETACH);
                    }
                }
            }

            // Allocated reservations
            if (EntityType.RESERVATION.allowsRole(entityRole)) {
                for (Reservation reservation : allocation.getReservations()) {
                    createChildAclRecord(aclEntry, identity, reservation, entityRole,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }
            else if (entityRole.equals(EntityRole.RESERVATION_REQUEST_USER)) {
                for (Reservation reservation : allocation.getReservations()) {
                    createChildAclRecord(aclEntry, identity, reservation, EntityRole.READER,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }
        }
        else if (entity instanceof Reservation) {
            Reservation reservation = (Reservation) entity;

            // Child reservations
            for (Reservation childReservation : reservation.getChildReservations()) {
                if (EntityType.RESERVATION.allowsRole(entityRole)) {
                    createChildAclRecord(aclEntry, identity, childReservation, entityRole,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }

            // Executable
            Executable executable = reservation.getExecutable();
            if (reservation.getExecutable() != null) {
                if (EntityType.EXECUTABLE.allowsRole(entityRole)) {
                    createChildAclRecord(aclEntry, identity, executable, entityRole,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }

            // Reused reservation
            if (reservation instanceof ExistingReservation) {
                ExistingReservation existingReservation = (ExistingReservation) reservation;
                Reservation reusedReservation = existingReservation.getReusedReservation();
                createChildAclRecord(aclEntry, identity, reusedReservation, EntityRole.READER,
                        AclEntryDependency.Type.DELETE_CASCADE);
            }
        }

        // Update entities
        if (entity instanceof Executable && entityRole.equals(EntityRole.OWNER)) {
            Executable executable = (Executable) entity;
            Executable.State state = executable.getState();
            if (state.equals(Executable.State.STARTED)) {
                executable.setState(Executable.State.MODIFIED);
            }
        }
    }

    /**
     * Method which is called after existing {@link AclEntry} is deleted.
     *
     * @param aclEntry
     * @param entity
     */
    private void beforeAclRecordDeleted(AclEntry aclEntry, PersistentObject entity)
    {
        EntityRole entityRole = EntityRole.valueOf(aclEntry.getRole());
        if (entity instanceof Executable && entityRole.equals(EntityRole.OWNER)) {
            Executable executable = (Executable) entity;
            Executable.State state = executable.getState();
            if (state.equals(Executable.State.STARTED)) {
                executable.setState(Executable.State.MODIFIED);
            }
        }
    }

    /**
     * @param userId
     * @param entity
     * @param role
     * @return list of {@link cz.cesnet.shongo.controller.acl.AclEntry}s for given parameters
     */
    public List<cz.cesnet.shongo.controller.acl.AclEntry> listAclRecords(String userId, PersistentObject entity,
            EntityRole role)
    {
        AclIdentity aclIdentity = aclProvider.getIdentity(AclIdentityType.USER, userId);
        AclObjectIdentity aclObjectIdentity = aclProvider.getObjectIdentity(entity);
        return listAclEntries(aclIdentity, aclObjectIdentity, role.toString());
    }

    /**
     * Represents a transaction for the {@link AuthorizationManager}.
     */
    private class Transaction
    {
        /**
         * Set of {@link AclEntry} which should be added to the {@link Authorization} cache.
         */
        private Set<AclEntry> addedAclEntries = new HashSet<AclEntry>();

        /**
         * Set of {@link AclEntry} which should be removed from the {@link Authorization} cache.
         */
        private Set<AclEntry> removedAclEntries = new HashSet<AclEntry>();

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
            addedAclEntries.clear();
            removedAclEntries.clear();
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
                for (AclEntry aclEntry : removedAclEntries) {
                    authorization.removeAclRecordFromCache(aclEntry);
                }
                for (AclEntry aclEntry : addedAclEntries) {
                    authorization.addAclRecordToCache(aclEntry);
                }
            }
        }

        /**
         * @param aclEntry to be added to the {@link Transaction}
         */
        public void addAclRecord(AclEntry aclEntry)
        {
            if (!removedAclEntries.remove(aclEntry)) {
                addedAclEntries.add(aclEntry);
            }
        }

        /**
         * @param aclEntry to be added to the {@link Transaction}
         */
        public void updateAclRecord(AclEntry aclEntry)
        {
            removedAclEntries.add(aclEntry);
            addedAclEntries.add(aclEntry);
        }

        /**
         * @param aclEntry to be removed from the {@link Transaction}
         */
        public void removeAclRecord(AclEntry aclEntry)
        {
            if (!addedAclEntries.remove(aclEntry)) {
                removedAclEntries.add(aclEntry);
            }
        }

        /**
         * @param objectIdentity
         * @return collection of {@link AclEntry}s for given {@code entityId}
         */
        public Collection<AclEntry> getAclRecords(AclObjectIdentity objectIdentity)
        {
            Set<AclEntry> aclEntries = new HashSet<AclEntry>();
            aclEntries.addAll(listAclEntries(objectIdentity));
            for (AclEntry aclEntry : addedAclEntries) {
                if (objectIdentity.equals(aclEntry.getObjectIdentity())) {
                    aclEntries.add(aclEntry);
                }
            }
            for (AclEntry aclEntry : removedAclEntries) {
                if (objectIdentity.equals(aclEntry.getObjectIdentity())) {
                    aclEntries.remove(aclEntry);
                }
            }
            return aclEntries;
        }

        /**
         * @param identity
         * @param objectIdentity
         * @param role
         * @return {@link AclEntry} for given parameters or null if doesn't exist
         */
        public AclEntry getAclRecord(AclIdentity identity, AclObjectIdentity objectIdentity, String role)
        {
            AclEntry aclEntry = AuthorizationManager.this.getAclRecord(identity, objectIdentity, role);
            if (aclEntry == null) {
                // If the ACL record is added in the transaction, return it
                for (AclEntry addedAclEntry : addedAclEntries) {
                    if (!identity.equals(addedAclEntry.getIdentity())) {
                        continue;
                    }
                    if (!objectIdentity.equals(addedAclEntry.getObjectIdentity())) {
                        continue;
                    }
                    if (!role.equals(addedAclEntry.getRole())) {
                        continue;
                    }
                    return addedAclEntry;
                }
            }
            else {
                // If the ACL record is removed in the transaction, return null
                if (removedAclEntries.contains(aclEntry)) {
                    return null;
                }
            }
            return aclEntry;
        }
    }
}
