package cz.cesnet.shongo.controller.authorization;

/**
 * TODO: Delete this class, it was only testing implementation of in-memory propagation of ACL records.
 * <p/>
 * Neni vhodne pouzivat postup implementovany v teto tride, protoze:
 * <p/>
 * Uzivatel si zarezervuje alias (napr. nazev mistnosti a H.323 cislo). Pote postupne vytvari rezervace na kapacitu
 * a casem techto rezervaci muze byt napr. 100. Uzivatel kazde rezervaci na kapacitu muze pridelit dalsi
 * vlastniky (klidne ruzne). Aby kazdy dalsi vlastnik byl schopen si rezervaci kapacity zobrazit musi mit roli READER
 * pro puvodni rezervaci aliasu.
 * Aby controller byl schopen zjistit, zda nejaky specificky uzivatel ma/nema pravo cteni pro puvodni rezervaci aliasu,
 * controller by musel projit vsech 100 rezervaci kapacit a zjistit jejich vlastniky, coz by bylo velmi pomale.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class InMemoryAclPropagationCache
{
    /*private static Logger logger = LoggerFactory.getLogger(EntityState.class);

    private EntityManagerFactory entityManagerFactory;

    private ExpirationMap<String, AclRecord> aclRecordCache = new ExpirationMap<String, AclRecord>();

    private ExpirationMap<EntityIdentifier, EntityState> entityStateById =
            new ExpirationMap<EntityIdentifier, EntityState>();

    private ExpirationMap<String, UserState> userStateById = new ExpirationMap<String, UserState>();

    public InMemoryAclPropagationCache(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void addAclRecord(AclRecord aclRecord) throws FaultException
    {
        String aclRecordId = aclRecord.getId();
        if (aclRecordId != null) {
            aclRecordCache.put(aclRecordId, aclRecord);
        }

        EntityIdentifier entityId = aclRecord.getEntityId();
        EntityState entityState = getEntityState(entityId, true);
        entityState.addAclRecord(aclRecord);

        String userId = aclRecord.getUserId();
        UserState userState = userStateById.get(userId);
        if (userState != null) {
            userState.addAclRecord(aclRecord);
        }
    }

    private EntityState getEntityState(EntityIdentifier entityId, boolean initialize) throws FaultException
    {
        // Find existing state
        EntityState entityState = entityStateById.get(entityId);
        if (entityState != null) {
            return entityState;
        }

        // Fetch new entity state
        entityState = new EntityState(entityId);
        for (AclRecord aclRecord : onListAclRecords(null, entityId, null)) {
            entityState.addAclRecord(aclRecord);
        }
        entityStateById.put(entityId, entityState);

        // Fetch all parent entity states (recursive)
        PersistentObject entity = getEntity(entityId);
        Map<PersistentObject, AclRecordPropagator> propagatorByParentEntity = getPropagatorByParentEntity(entity);
        for (PersistentObject parentEntity : propagatorByParentEntity.keySet()) {
            EntityIdentifier parentEntityId = new EntityIdentifier(parentEntity);
            EntityState parentEntityState = getEntityState(parentEntityId, false);
            AclRecordPropagator propagator = propagatorByParentEntity.get(parentEntity);
            entityState.parentEntityStates.put(parentEntityState, propagator);
            parentEntityState.childEntityStates.put(entityState, propagator);
        }

        // Initialize newly fetched state
        if (initialize) {
            for (EntityState parentEntityState : entityState.parentEntityStates.keySet()) {
                AclRecordPropagator propagator = entityState.parentEntityStates.get(parentEntityState);
                propagator.addAclRecords(parentEntityState, entityState);
            }
        }

        return entityState;
    }

    private PersistentObject getEntity(EntityIdentifier entityId) throws FaultException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
            if (entity == null) {
                ControllerReportSetHelper.throwEntityNotFoundFault(entityId);
            }
            return entity;
        }
        finally {
            entityManager.close();
        }
    }

    private Map<PersistentObject, AclRecordPropagator> getPropagatorByParentEntity(PersistentObject entity)
    {
        logger.debug("Get parent for {}...", new EntityIdentifier(entity));
        Map<PersistentObject, AclRecordPropagator> propagatorByParentEntity =
                new HashMap<PersistentObject, AclRecordPropagator>();
        if (entity instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) entity;
            ReservationRequestSet reservationRequestSet = reservationRequest.getReservationRequestSet();
            if (reservationRequestSet != null) {
                propagatorByParentEntity.put(reservationRequestSet, new AclRecordPropagator());
            }
        }
        else if (entity instanceof Reservation) {
            Reservation reservation = (Reservation) entity;
            ReservationRequest reservationRequest = reservation.getReservationRequest();
            if (reservationRequest != null) {
                propagatorByParentEntity.put(reservationRequest, new AclRecordPropagator());
            }

            Reservation parentReservation = reservation.getParentReservation();
            if (parentReservation != null) {
                propagatorByParentEntity.put(parentReservation, new AclRecordPropagator());
            }

            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                ReservationManager reservationManager = new ReservationManager(entityManager);
                Collection<ExistingReservation> existingReservations = reservationManager
                        .getExistingReservations(reservation);
                for (ExistingReservation existingReservation : existingReservations) {
                    propagatorByParentEntity.put(existingReservation, new AclRecordPropagator()
                    {
                        @Override
                        public void addAclRecord(EntityState targetEntityState, String userId, Role role)
                        {
                            if (role.equals(Role.OWNER) || role.equals(Role.RESERVATION_USER)) {
                                role = Role.READER;
                            }
                            super.addAclRecord(targetEntityState, userId, role);
                        }
                    });
                }
            }
            finally {
                entityManager.close();
            }
        }
        else if (entity instanceof Executable) {
            Executable executable = (Executable) entity;
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                ExecutableManager executableManager = new ExecutableManager(entityManager);
                Reservation reservation = executableManager.getReservation(executable);
                if (reservation != null) {
                    propagatorByParentEntity.put(reservation, new AclRecordPropagator());
                }
            }
            finally {
                entityManager.close();
            }
        }
        return propagatorByParentEntity;
    }

    public Collection<AclRecord> getAclRecords(String userId)
    {
        return null;
    }

    public Collection<AclRecord> getAclRecords(EntityIdentifier entityId)
    {
        return null;
    }

    public Collection<AclRecord> getAclRecords(String userId, EntityIdentifier entityId) throws FaultException
    {
        EntityState entityState = getEntityState(entityId, true);
        EntityUserState entityUserState = entityState.getUserState(userId);
        return entityUserState.aclRecords;
    }


    protected Collection<AclRecord> onListAclRecords(String userId, EntityIdentifier entityId, Role role)
            throws FaultException
    {
        return Collections.emptyList();
    }


    private class EntityState
    {
        private EntityIdentifier entityId;

        private Map<EntityState, AclRecordPropagator> parentEntityStates =
                new HashMap<EntityState, AclRecordPropagator>();

        private Map<EntityState, AclRecordPropagator> childEntityStates =
                new HashMap<EntityState, AclRecordPropagator>();

        private Map<String, EntityUserState> entityUserStateByUserId = new HashMap<String, EntityUserState>();

        private Map<Role, Set<String>> userIdsByRole = new HashMap<Role, Set<String>>();

        public EntityState(EntityIdentifier entityId)
        {
            this.entityId = entityId;
        }

        public boolean addAclRecord(AclRecord aclRecord)
        {
            if (!entityId.equals(aclRecord.getEntityId())) {
                throw new RuntimeException();
            }

            String userId = aclRecord.getUserId();
            EntityUserState entityUserState = getUserState(userId);
            if (!entityUserState.addAclRecord(aclRecord)) {
                return false;
            }

            // Propagate ACL to child entity states
            for (Map.Entry<EntityState, AclRecordPropagator> entry : childEntityStates.entrySet()) {
                entry.getValue().addAclRecord(entry.getKey(), userId, aclRecord.getRole());
            }

            return true;
        }

        public boolean hasRecord(String userId, Role role)
        {
            EntityUserState entityUserState = getUserState(userId);
            return entityUserState.roles.contains(role);
        }

        public EntityUserState getUserState(String userId)
        {
            EntityUserState entityUserState = entityUserStateByUserId.get(userId);
            if (entityUserState == null) {
                entityUserState = new EntityUserState();
                entityUserStateByUserId.put(userId, entityUserState);
            }
            return entityUserState;
        }
    }

    private static class EntityUserState
    {
        private Set<AclRecord> aclRecords = new HashSet<AclRecord>();

        private Set<Role> roles = new HashSet<Role>();

        private Set<Permission> permissions = new HashSet<Permission>();

        public boolean addAclRecord(AclRecord aclRecord)
        {
            roles.add(aclRecord.getRole());
            return aclRecords.add(aclRecord);
        }
    }

    private static class UserState
    {
        private Map<EntityType, Set<Long>> accessibleEntitiesByType = new HashMap<EntityType, Set<Long>>();

        public void addAclRecord(AclRecord aclRecord)
        {
            throw new TodoImplementException();
        }
    }

    private static class AclRecordPropagator
    {
        public void addAclRecord(EntityState targetEntityState, String userId, Role role)
        {
            if (targetEntityState.hasRecord(userId, role)) {
                return;
            }
            logger.debug("Adding dynamic ACL (user: {}, entity: {}, role: {})",
                    new Object[]{userId, targetEntityState.entityId, role});
            AclRecord aclRecord = new AclRecord(userId, targetEntityState.entityId, role);
            targetEntityState.addAclRecord(aclRecord);
        }

        public final void addAclRecords(EntityState sourceEntityState, EntityState targetEntityState)
        {
            for (EntityUserState entityUserState : sourceEntityState.entityUserStateByUserId.values()) {
                for (AclRecord aclRecord : entityUserState.aclRecords) {
                    addAclRecord(targetEntityState, aclRecord.getUserId(), aclRecord.getRole());
                }
            }
            for (Map.Entry<EntityState, AclRecordPropagator> entry : sourceEntityState.parentEntityStates.entrySet()) {
                entry.getValue().addAclRecords(entry.getKey(), sourceEntityState);
            }
        }
    }

    int getEntityCount()
    {
        return entityStateById.size();
    }

    int getAclRecordCount(EntityIdentifier entityId)
    {
        EntityState entityState = entityStateById.get(entityId);
        int aclRecordCount = 0;
        for (EntityUserState entityUserState : entityState.entityUserStateByUserId.values()) {
            aclRecordCount += entityUserState.aclRecords.size();
        }
        return aclRecordCount;
    }*/
}
