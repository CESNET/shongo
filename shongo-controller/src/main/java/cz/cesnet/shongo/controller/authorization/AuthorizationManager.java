package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.acl.*;
import cz.cesnet.shongo.controller.acl.AclEntry;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.ObjectTypeResolver;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.resource.ResourceTag;
import cz.cesnet.shongo.controller.booking.resource.Tag;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.*;

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
     * @return {@link #authorization}
     */
    public Authorization getAuthorization()
    {
        return authorization;
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
    public void commitTransaction(SecurityToken securityToken)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }
        activeTransaction.commit(securityToken);
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
     * @param objectRole
     * @return list of user-ids with given {@code role} for given {@code object}
     */
    public UserIdSet getUserIdsWithRole(PersistentObject object, ObjectRole objectRole)
    {
        AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(object);
        List<AclEntry> entries = entityManager.createNamedQuery("AclEntry.findByObjectIdentityAndRole", AclEntry.class)
                .setParameter("objectIdentity", objectIdentity)
                .setParameter("role", objectRole.toString())
                .getResultList();
        UserIdSet userIds = new UserIdSet();
        for (AclEntry entry : entries) {
            userIds.addAll(authorization.getUserIds(entry.getIdentity()));
        }
        return userIds;
    }

    /**
     * @param object
     * @param objectRole
     * @return list of users with given {@code role} for given {@code object}
     */
    public Collection<UserInformation> getUsersWithRole(PersistentObject object, ObjectRole objectRole)
    {
        List<UserInformation> users = new LinkedList<UserInformation>();
        UserIdSet userIdSet = getUserIdsWithRole(object, objectRole);
        for (String userId : userIdSet.getUserIds()) {
            users.add(getUserInformation(userId));
        }
        return users;
    }

    /**
     * Create a new {@link AclEntry}.
     *
     * @param identityType        for which the ACL is created.
     * @param identityPrincipalId for which the ACL is created.
     * @param object              for which the ACL is created.
     * @param objectRole          which is created for given user and given {@code object}
     * @return new {@link AclEntry}
     */
    public AclEntry createAclEntry(AclIdentityType identityType, String identityPrincipalId, PersistentObject object,
            ObjectRole objectRole)
    {
        AclIdentity identity = aclProvider.getIdentity(identityType, identityPrincipalId);
        return createAclEntry(identity, object, objectRole);
    }

    /**
     * Create a new {@link AclEntry}.
     *
     * @param identity   for which the ACL is created.
     * @param object     for which the ACL is created.
     * @param objectRole which is created for given user and given {@code object}
     * @return new {@link AclEntry}
     */
    public AclEntry createAclEntry(AclIdentity identity, PersistentObject object, ObjectRole objectRole)
    {
        if (identity.getType().equals(AclIdentityType.USER) &&
                identity.getPrincipalId().equals(Authorization.ROOT_USER_ID)) {
            return null;
        }

        AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(object);
        ObjectType objectType = ObjectTypeResolver.getObjectType(objectIdentity);
        if (!objectType.allowsRole(objectRole)) {
            throw new ControllerReportSet.AclInvalidObjectRoleException(ObjectIdentifier.formatId(object),
                    objectRole.toString());
        }

        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclEntry aclEntry = activeTransaction.getAclEntry(identity, objectIdentity, objectRole.toString());
        if (aclEntry != null) {
            return aclEntry;
        }

        try {
            aclEntry = super.createEntry(identity, objectIdentity, objectRole.toString());

            activeTransaction.addAclEntry(aclEntry);

            afterAclEntryCreated(aclEntry, object);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(String.format("ACL entry creation failed (identity: %s, object: %s, role: %s)",
                    identity, objectIdentity, objectRole), throwable);
        }

        Controller.loggerAcl.info("ACL entry created (id: {}, identity: {}, object: {}, role: {})",
                new Object[]{aclEntry.getId(), identity, objectIdentity, objectRole});

        return aclEntry;
    }

    /**
     * Creates all {@link AclEntry}s from given {@code parentObject} to given {@code childObject} which the
     * {@code childObject} allows.
     *
     * @param parentObject from which should be fetch all existing {@link AclEntry}s
     * @param childObject  to which should be created new {@link AclEntry}s
     */
    public void createAclEntriesForChildEntity(PersistentObject parentObject, PersistentObject childObject)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclObjectIdentity parentObjectIdentity = aclProvider.getObjectIdentity(parentObject);
        AclObjectIdentity childObjectIdentity = aclProvider.getObjectIdentity(childObject);
        ObjectType childObjectType = ObjectTypeResolver.getObjectType(childObjectIdentity);
        for (AclEntry parentAclEntry : activeTransaction.getAclEntries(parentObjectIdentity)) {
            AclIdentity identity = parentAclEntry.getIdentity();
            ObjectRole objectRole = ObjectRole.valueOf(parentAclEntry.getRole());
            if (childObjectType.allowsRole(objectRole)) {
                createChildAclEntry(parentAclEntry, identity, childObject, objectRole,
                        AclEntryDependency.Type.DELETE_DETACH);
            }
        }
    }

    /**
     * Create a new child {@link AclEntry}.
     *
     * @param parentAclEntry
     * @param identity
     * @param childEntity
     * @param objectRole
     */
    private void createChildAclEntry(AclEntry parentAclEntry, AclIdentity identity, PersistentObject childEntity,
            ObjectRole objectRole, AclEntryDependency.Type dependencyType)
    {
        AclObjectIdentity parentObjectIdentity = parentAclEntry.getObjectIdentity();
        if (!ObjectTypeResolver.getObjectType(parentObjectIdentity).isRolePropagatable(objectRole)) {
            return;
        }

        AclEntry childAclEntry = createAclEntry(identity, childEntity, objectRole);

        List<AclEntryDependency> aclEntryDependencies =
                entityManager.createQuery("SELECT dependency FROM AclEntryDependency dependency"
                        + " WHERE dependency.parentAclEntry = :parent AND dependency.childAclEntry = :child",
                        AclEntryDependency.class)
                        .setParameter("parent", parentAclEntry)
                        .setParameter("child", childAclEntry)
                        .getResultList();
        if (aclEntryDependencies.size() == 0) {
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
    public void deleteAclEntry(AclEntry aclEntry)
    {
        deleteAclEntry(aclEntry, false);
    }

    /**
     * Delete all {@link AclEntry}s for given {@code object}.
     *
     * @param object
     */
    public void deleteAclEntriesForEntity(PersistentObject object)
    {
        AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(object);
        for (AclEntry aclEntry : activeTransaction.getAclEntries(objectIdentity)) {
            deleteAclEntry(aclEntry, true);
        }
    }

    /**
     * Delete inherited {@link AclEntry}s for given {@code childObject} from given {@code parentObject}.
     *
     * @param parentObject
     * @param childObject
     */
    public void deleteAclEntriesForChildEntity(PersistentObject parentObject, PersistentObject childObject)
    {
        AclObjectIdentity childObjectIdentity = aclProvider.getObjectIdentity(childObject);
        AclObjectIdentity parentObjectIdentity = aclProvider.getObjectIdentity(parentObject);

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<AclEntryDependency> query = criteriaBuilder.createQuery(AclEntryDependency.class);
        Root<AclEntryDependency> aclEntryDependencyRoot = query.from(AclEntryDependency.class);
        Join<AclEntryDependency,AclEntry> join = aclEntryDependencyRoot.join("childAclEntry");
        query.where(criteriaBuilder.equal(join.get("objectIdentity"), childObjectIdentity));

        TypedQuery<AclEntryDependency> typedQuery = entityManager.createQuery(query);
        List<AclEntryDependency> aclEntryDependencyList = typedQuery.getResultList();

        // delete dependency and childAclEntry if it has only one parent
        for (AclEntryDependency aclEntryDependency : aclEntryDependencyList) {
            if (aclEntryDependency.getParentAclEntry().getObjectIdentity().equals(parentObjectIdentity)) {
                entityManager.remove(aclEntryDependency);
                if (aclEntryDependencyList.size() == 1) {
                    deleteAclEntry(aclEntryDependency.getChildAclEntry());
                }
                return;
            }
        }
    }

    /**
     * Delete given {@code aclEntry},
     *
     * @param aclEntry
     * @param detachChildren
     */
    private void deleteAclEntry(AclEntry aclEntry, boolean detachChildren)
    {
        if (activeTransaction == null) {
            throw new IllegalStateException("No transaction is active.");
        }

        AclObjectIdentity objectIdentity = aclEntry.getObjectIdentity();
        Class<? extends PersistentObject> objectClass = ObjectTypeResolver.getObjectTypeClass(objectIdentity);
        if (objectClass.equals(AbstractReservationRequest.class)) {
            objectClass = Allocation.class;
        }
        PersistentObject entity = entityManager.find(objectClass, objectIdentity.getObjectId());
        if (entity == null) {
            throw new IllegalStateException("Entity " + objectIdentity + " referenced from ACL doesn't exist.");
        }

        // Delete ACL entry dependencies
        Collection<AclEntryDependency> parentAclEntryDependencies = entityManager.createQuery(
                "SELECT dependency FROM AclEntryDependency dependency"
                        + " WHERE dependency.childAclEntry = :aclEntry", AclEntryDependency.class)
                .setParameter("aclEntry", aclEntry)
                .getResultList();

        if (parentAclEntryDependencies.size() > 0) {
            if (detachChildren) {
                for (AclEntryDependency aclEntryDependency : parentAclEntryDependencies) {
                    entityManager.remove(aclEntryDependency);
                }
            }
            else {
                ControllerReportSetHelper.throwObjectNotDeletableReferencedFault(AclEntry.class, aclEntry.getId());
            }
        }

        // Refresh entry
        aclEntry = entityManager.merge(aclEntry);

        // Delete ACL entry dependencies
        Collection<AclEntryDependency> childAclEntryDependencies = entityManager.createQuery(
                "SELECT dependency FROM AclEntryDependency dependency"
                        + " WHERE dependency.parentAclEntry = :aclEntry", AclEntryDependency.class)
                .setParameter("aclEntry", aclEntry)
                .getResultList();
        for (AclEntryDependency aclEntryDependency : childAclEntryDependencies) {
            entityManager.remove(aclEntryDependency);
            if (!aclEntryDependency.getType().equals(AclEntryDependency.Type.DELETE_DETACH) || !detachChildren) {
                AclEntry childAclEntry = aclEntryDependency.getChildAclEntry();
                try {
                    deleteAclEntry(childAclEntry);
                }
                catch (CommonReportSet.ObjectNotDeletableReferencedException exception) {
                    Controller.loggerAcl.info(
                            "ACL entry (id: {}, identity: {}, object: {}, role: {}) cannot be deleted,"
                                    + " because it is referenced.", new Object[]{childAclEntry.getId(),
                            childAclEntry.getIdentity(), childAclEntry.getObjectIdentity(), childAclEntry.getRole()
                    });
                }
            }
        }

        // Delete ACL entry
        beforeAclEntryDeleted(aclEntry, entity);
        super.deleteEntry(aclEntry);
        activeTransaction.removeAclEntry(aclEntry);

        Controller.loggerAcl.info("Deleted ACL entry (id: {}, identity: {}, object: {}, role: {})",
                new Object[]{aclEntry.getId(), aclEntry.getIdentity(), objectIdentity, aclEntry.getRole()});
    }

    /**
     * Method which is called after new {@link AclEntry} is created.
     *
     * @param aclEntry
     * @param object
     */
    private void afterAclEntryCreated(AclEntry aclEntry, PersistentObject object)
    {
        AclIdentity identity = aclEntry.getIdentity();
        ObjectRole objectRole = ObjectRole.valueOf(aclEntry.getRole());

        // Create child ACL entries
        if (object instanceof AbstractReservationRequest) {
            AbstractReservationRequest reservationRequest = (AbstractReservationRequest) object;
            Allocation allocation = reservationRequest.getAllocation();

            // Child reservation requests
            for (ReservationRequest childReservationRequest : allocation.getChildReservationRequests()) {
                if (ObjectType.RESERVATION_REQUEST.allowsRole(objectRole)) {
                    createChildAclEntry(aclEntry, identity, childReservationRequest, objectRole,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }

            // Reservation requests which reuse this reservation request
            if (reservationRequest.getReusement().equals(ReservationRequestReusement.OWNED)) {
                ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
                List<AbstractReservationRequest> reservationRequestUsages =
                        reservationRequestManager.listReservationRequestActiveUsages(reservationRequest);
                for (AbstractReservationRequest reservationRequestUsage : reservationRequestUsages) {
                    if (ObjectType.RESERVATION_REQUEST.allowsRole(objectRole)) {
                        createChildAclEntry(aclEntry, identity, reservationRequestUsage, objectRole,
                                AclEntryDependency.Type.DELETE_DETACH);
                    }
                }
            }

            // Allocated reservations
            if (ObjectType.RESERVATION.allowsRole(objectRole)) {
                for (Reservation reservation : allocation.getReservations()) {
                    createChildAclEntry(aclEntry, identity, reservation, objectRole,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }
            else if (objectRole.equals(ObjectRole.RESERVATION_REQUEST_USER)) {
                for (Reservation reservation : allocation.getReservations()) {
                    createChildAclEntry(aclEntry, identity, reservation, ObjectRole.READER,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }
        }
        else if (object instanceof Reservation) {
            Reservation reservation = (Reservation) object;

            // Child reservations
            for (Reservation childReservation : reservation.getChildReservations()) {
                if (ObjectType.RESERVATION.allowsRole(objectRole)) {
                    createChildAclEntry(aclEntry, identity, childReservation, objectRole,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }

            // Executable
            Executable executable = reservation.getExecutable();
            if (reservation.getExecutable() != null) {
                if (ObjectType.EXECUTABLE.allowsRole(objectRole)) {
                    createChildAclEntry(aclEntry, identity, executable, objectRole,
                            AclEntryDependency.Type.DELETE_DETACH);
                }
            }

            // Reused reservation
            if (reservation instanceof ExistingReservation) {
                ExistingReservation existingReservation = (ExistingReservation) reservation;
                Reservation reusedReservation = existingReservation.getReusedReservation();
                createChildAclEntry(aclEntry, identity, reusedReservation, ObjectRole.READER,
                        AclEntryDependency.Type.DELETE_CASCADE);
            }
        }
        else if (object instanceof Executable) {
            // Update entities
            Executable executable = (Executable) object;
            if (executable.canBeModified()) {
                executable.setModified(true);
            }
        }
        else if (object instanceof Tag) {
            // Update tagged resources ACLs
            Tag tag = (Tag) object;
            ResourceManager resourceManager = new ResourceManager(entityManager);
            for (ResourceTag resourceTag : resourceManager.getResourceTagsByTag(tag.getId())) {
                PersistentObject persistentObject = null;
                if (resourceTag.getResource() != null) {
                    persistentObject = resourceTag.getResource();
                }
                else if (resourceTag.getForeignResources() != null) {
                    persistentObject = resourceTag.getForeignResources();
                }
                createChildAclEntry(aclEntry, identity, persistentObject, objectRole,
                        AclEntryDependency.Type.DELETE_CASCADE);
            }
        }
    }

    /**
     * Method which is called after existing {@link AclEntry} is deleted.
     *
     * @param aclEntry
     * @param object
     */
    private void beforeAclEntryDeleted(AclEntry aclEntry, PersistentObject object)
    {
        ObjectRole objectRole = ObjectRole.valueOf(aclEntry.getRole());
        if (object instanceof Executable && objectRole.equals(ObjectRole.OWNER)) {
            Executable executable = (Executable) object;
            if (executable.canBeModified()) {
                executable.setModified(true);
            }
        }
    }

    /**
     * @param userId
     * @param entity
     * @param role
     * @return list of {@link cz.cesnet.shongo.controller.acl.AclEntry}s for given parameters
     */
    public List<cz.cesnet.shongo.controller.acl.AclEntry> listAclEntries(String userId, PersistentObject entity,
            ObjectRole role)
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
        public void commit(SecurityToken securityToken)
        {
            if (authorization == null) {
                throw new IllegalArgumentException("Authorization must not be null.");
            }
            synchronized (authorization) {
                for (AclEntry aclEntry : removedAclEntries) {
                    authorization.removeAclEntryFromCache(aclEntry);
                }
                for (AclEntry aclEntry : addedAclEntries) {
                    authorization.addAclEntryToCache(aclEntry, securityToken);
                }
            }
        }

        /**
         * @param aclEntry to be added to the {@link Transaction}
         */
        public void addAclEntry(AclEntry aclEntry)
        {
            if (!removedAclEntries.remove(aclEntry)) {
                addedAclEntries.add(aclEntry);
            }
        }

        /**
         * @param aclEntry to be removed from the {@link Transaction}
         */
        public void removeAclEntry(AclEntry aclEntry)
        {
            if (!addedAclEntries.remove(aclEntry)) {
                removedAclEntries.add(aclEntry);
            }
        }

        /**
         * @param objectIdentity
         * @return collection of {@link AclEntry}s for given {@code objectIdentity}
         */
        public Collection<AclEntry> getAclEntries(AclObjectIdentity objectIdentity)
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
        public AclEntry getAclEntry(AclIdentity identity, AclObjectIdentity objectIdentity, String role)
        {
            AclEntry aclEntry = AuthorizationManager.this.getAclEntry(identity, objectIdentity, role);
            if (aclEntry == null) {
                // If the ACL entry is added in the transaction, return it
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
                // If the ACL entry is removed in the transaction, return null
                if (removedAclEntries.contains(aclEntry)) {
                    return null;
                }
            }
            return aclEntry;
        }
    }
}
