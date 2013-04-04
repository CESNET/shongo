package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.ControllerFaultSet;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executable;
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

    /**
     * Create a new {@link AclRecord}.
     *
     * @param userId of user for which the ACL is created.
     * @param entity for which the ACL is created.
     * @param role   which is created for given user and given entity
     * @return new {@link AclRecord}
     * @throws FaultException when the creation failed.
     */
    public AclRecord createAclRecord(String userId, PersistentObject entity, Role role) throws FaultException
    {
        if (userId.equals(Authorization.ROOT_USER_ID)) {
            return null;
        }
        return createAclRecord(userId, entity, new EntityIdentifier(entity), role);
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
    public AclRecord createAclRecord(String userId, EntityIdentifier entityId, Role role)
            throws FaultException
    {
        if (userId.equals(Authorization.ROOT_USER_ID)) {
            return null;
        }
        PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
        if (entity == null) {
            ControllerFaultSet.throwEntityNotFoundFault(entityId);
        }
        return createAclRecord(userId, entity, entityId, role);
    }

    /**
     * Creates all {@link AclRecord}s from given {@code parentEntity} to given {@code childEntity} which the
     * {@code childEntity} allows.
     *
     * @param parentEntity from which should be fetch all existing {@link AclRecord}s
     * @param childEntity  to which should be created new {@link AclRecord}s
     * @throws FaultException
     */
    public void createAclRecordsForChildEntity(PersistentObject parentEntity, PersistentObject childEntity)
            throws FaultException
    {
        EntityIdentifier parentEntityId = new EntityIdentifier(parentEntity);
        EntityIdentifier childEntityId = new EntityIdentifier(childEntity);
        for (AclRecord parentAclRecord : authorization.getAclRecords(parentEntityId)) {
            String userId = parentAclRecord.getUserId();
            Role role = parentAclRecord.getRole();
            EntityType childEntityType = childEntityId.getEntityType();
            if (childEntityType.allowsRole(role)) {
                createAclRecord(userId, childEntity, role);
            }
        }
    }

    /**
     * Create a new {@link AclRecord}.
     *
     * @param userId   of user for which the ACL is created.
     * @param entity   for which the ACL is created.
     * @param entityId of entity for which the ACL is created.
     * @param role     which is created for given user and given entity
     * @return new {@link AclRecord}
     * @throws cz.cesnet.shongo.fault.FaultException
     *          when the creation failed.
     */
    private AclRecord createAclRecord(String userId, PersistentObject entity, EntityIdentifier entityId, Role role)
            throws FaultException
    {
        if (userId.equals(Authorization.ROOT_USER_ID)) {
            return null;
        }
        EntityType entityType = entityId.getEntityType();
        if (!entityType.allowsRole(role)) {
            ControllerFaultSet.throwAclInvalidRoleFault(entityId.toId(), role.toString());
        }
        AclRecord aclRecord = authorization.getAclRecord(userId, entityId, role);
        if (aclRecord != null) {
            return aclRecord;
        }

        try {
            aclRecord = new AclRecord();
            aclRecord.setUserId(userId);
            aclRecord.setEntityId(entityId);
            aclRecord.setRole(role);
            entityManager.persist(aclRecord);

            // TODO: Update cache (take transaction into account)

            afterAclRecordCreated(aclRecord, entity);
        }
        catch (Throwable throwable) {
            throw new FaultException(throwable, "ACL Record creation failed (user: %s, entity: %s, role: %s)",
                    userId, entityId, role);
        }

        logger.debug("ACL Record created (id: {}, user: {}, entity: {}, role: {})",
                new Object[]{aclRecord.getId(), userId, entityId, role});

        return aclRecord;
    }

    /**
     * Create a new child {@link AclRecord}.
     *
     * @param parentAclRecord
     * @param userId
     * @param childEntity
     * @param role
     * @throws FaultException
     */
    private void createChildAclRecord(AclRecord parentAclRecord, String userId, PersistentObject childEntity,
            Role role, AclRecordDependency.Type dependencyType) throws FaultException
    {
        AclRecord childAclRecord = createAclRecord(userId, childEntity, role);

        AclRecordDependency aclRecordDependency = new AclRecordDependency();
        aclRecordDependency.setParentAclRecord(parentAclRecord);
        aclRecordDependency.setChildAclRecord(childAclRecord);
        aclRecordDependency.setType(dependencyType);
        entityManager.persist(aclRecordDependency);
    }

    /**
     * Delete given {@code aclRecord} and all corresponding {@link AclRecord}s from child entities.
     *
     * @throws FaultException when the deletion failed
     */
    public void deleteAclRecord(AclRecord aclRecord) throws FaultException
    {
        EntityIdentifier entityId = aclRecord.getEntityId();
        PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
        if (entity == null) {
            throw new IllegalStateException("Entity " + entityId.toString() + " referenced from ACL doesn't exist.");
        }

        beforeAclRecordDeleted(aclRecord, entity);

        entityManager.remove(aclRecord);

        // TODO: Update cache (take transaction into account)

        // TODO: Delete dependencies
    }

    /**
     * Delete all given {@link AclRecord}s.
     *
     * @param aclRecords to be deleted
     * @throws FaultException
     */
    public void deleteAclRecords(Collection<AclRecord> aclRecords) throws FaultException
    {
        // TODO: remove method and create method deleteAclRecordsForEntity()
        for (AclRecord aclRecord : aclRecords) {
            deleteAclRecord(aclRecord);
        }

    }

    private Collection<String> getChildAclRecords(String aclRecordId)
    {
        Collection<String> childAclRecordRecordIds = entityManager.createQuery(
                "SELECT dependency.id.childAclRecordId FROM AclRecordDependency dependency"
                        + " WHERE dependency.id.parentAclRecordId = :id", String.class)
                .setParameter("id", aclRecordId)
                .getResultList();
        return childAclRecordRecordIds;
    }

    /**
     * Method which is called after new {@link AclRecord} is created.
     *
     * @param aclRecord
     * @param entity
     */
    private void afterAclRecordCreated(AclRecord aclRecord, PersistentObject entity) throws FaultException
    {
        String userId = aclRecord.getUserId();
        Role role = aclRecord.getRole();

        // Create child ACL records
        if (entity instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) entity;
            for (ReservationRequest reservationRequest : reservationRequestSet.getReservationRequests()) {
                if (EntityType.RESERVATION_REQUEST.allowsRole(role)) {
                    createChildAclRecord(aclRecord, userId, reservationRequest, role,
                            AclRecordDependency.Type.DELETE_DETACH);
                }
            }
        }
        else if (entity instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) entity;
            Reservation reservation = reservationRequest.getReservation();
            if (reservation != null) {
                if (EntityType.RESERVATION.allowsRole(role)) {
                    createChildAclRecord(aclRecord, userId, reservation, role, AclRecordDependency.Type.DELETE_DETACH);
                }
            }
        }
        else if (entity instanceof Reservation) {
            Reservation reservation = (Reservation) entity;

            // Child reservations
            for (Reservation childReservation : reservation.getChildReservations()) {
                if (EntityType.RESERVATION.allowsRole(role)) {
                    createChildAclRecord(aclRecord, userId, childReservation,  role,
                            AclRecordDependency.Type.DELETE_DETACH);
                }
            }

            // Executable
            Executable executable = reservation.getExecutable();
            if (reservation.getExecutable() != null) {
                if (EntityType.EXECUTABLE.allowsRole(role)) {
                    createChildAclRecord(aclRecord, userId, executable, role, AclRecordDependency.Type.DELETE_DETACH);
                }
                else if (role.equals(Role.RESERVATION_USER)) {
                    createChildAclRecord(aclRecord, userId, executable, Role.READER,
                            AclRecordDependency.Type.DELETE_DETACH);
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
    private void beforeAclRecordDeleted(AclRecord aclRecord, PersistentObject entity) throws FaultException
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
}
