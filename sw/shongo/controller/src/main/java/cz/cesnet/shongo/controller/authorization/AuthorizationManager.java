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
import cz.cesnet.shongo.fault.TodoImplementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
     * Create {@link AclRecordDependency} for given parameters.
     *
     * @param parentAclRecordId {@link AclRecordDependency#parentAclRecordId}
     * @param childAclRecordId  {@link AclRecordDependency#childAclRecordId}
     */
    private void createAclRecordDependency(String parentAclRecordId, String childAclRecordId)
    {
        AclRecordDependency aclRecordDependency = new AclRecordDependency();
        aclRecordDependency.setParentAclRecordId(parentAclRecordId);
        aclRecordDependency.setChildAclRecordId(childAclRecordId);
        entityManager.persist(aclRecordDependency);
        throw new TodoImplementException();
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

    public AclRecord executeAclRecordCreateRequest(AclRecordCreateRequest aclRecordCreateRequest)
    {
        entityManager.getTransaction().begin();

        entityManager.getTransaction().commit();

        return null;
    }

    public void executeAclRecordRequests()
    {
        entityManager.getTransaction().begin();


        //throw new TodoImplementException();
        entityManager.getTransaction().commit();
    }
}
