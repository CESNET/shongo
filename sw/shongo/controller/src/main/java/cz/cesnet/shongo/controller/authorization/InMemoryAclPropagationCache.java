package cz.cesnet.shongo.controller.authorization;

/**
 * TODO: Delete this class, it was only testing implementation of in-memory propagation of ACL entries.
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

    private ExpirationMap<String, AclEntry> aclEntryCache = new ExpirationMap<String, AclEntry>();

    private ExpirationMap<EntityIdentifier, EntityState> entityStateById =
            new ExpirationMap<EntityIdentifier, EntityState>();

    private ExpirationMap<String, UserState> userStateById = new ExpirationMap<String, UserState>();

    public InMemoryAclPropagationCache(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void addAclEntry(AclEntry aclEntry) throws FaultException
    {
        String aclEntryId = aclEntry.getId();
        if (aclEntryId != null) {
            aclEntryCache.put(aclEntryId, aclEntry);
        }

        EntityIdentifier entityId = aclEntry.getObjectId();
        EntityState entityState = getEntityState(entityId, true);
        entityState.addAclEntry(aclEntry);

        String userId = aclEntry.getUserId();
        UserState userState = userStateById.get(userId);
        if (userState != null) {
            userState.addAclEntry(aclEntry);
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
        for (AclEntry aclEntry : onListAclEntrys(null, entityId, null)) {
            entityState.addAclEntry(aclEntry);
        }
        entityStateById.put(entityId, entityState);

        // Fetch all parent entity states (recursive)
        PersistentObject entity = getEntity(entityId);
        Map<PersistentObject, AclEntryPropagator> propagatorByParentEntity = getPropagatorByParentEntity(entity);
        for (PersistentObject parentEntity : propagatorByParentEntity.keySet()) {
            EntityIdentifier parentEntityId = new EntityIdentifier(parentEntity);
            EntityState parentEntityState = getEntityState(parentEntityId, false);
            AclEntryPropagator propagator = propagatorByParentEntity.get(parentEntity);
            entityState.parentEntityStates.put(parentEntityState, propagator);
            parentEntityState.childEntityStates.put(entityState, propagator);
        }

        // Initialize newly fetched state
        if (initialize) {
            for (EntityState parentEntityState : entityState.parentEntityStates.keySet()) {
                AclEntryPropagator propagator = entityState.parentEntityStates.get(parentEntityState);
                propagator.addAclEntrys(parentEntityState, entityState);
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
                ControllerReportSetHelper.throwEntityNotExistFault(entityId);
            }
            return entity;
        }
        finally {
            entityManager.close();
        }
    }

    private Map<PersistentObject, AclEntryPropagator> getPropagatorByParentEntity(PersistentObject entity)
    {
        logger.debug("Get parent for {}...", new EntityIdentifier(entity));
        Map<PersistentObject, AclEntryPropagator> propagatorByParentEntity =
                new HashMap<PersistentObject, AclEntryPropagator>();
        if (entity instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) entity;
            ReservationRequestSet reservationRequestSet = reservationRequest.getReservationRequestSet();
            if (reservationRequestSet != null) {
                propagatorByParentEntity.put(reservationRequestSet, new AclEntryPropagator());
            }
        }
        else if (entity instanceof Reservation) {
            Reservation reservation = (Reservation) entity;
            ReservationRequest reservationRequest = reservation.getReservationRequest();
            if (reservationRequest != null) {
                propagatorByParentEntity.put(reservationRequest, new AclEntryPropagator());
            }

            Reservation parentReservation = reservation.getParentReservation();
            if (parentReservation != null) {
                propagatorByParentEntity.put(parentReservation, new AclEntryPropagator());
            }

            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                ReservationManager reservationManager = new ReservationManager(entityManager);
                Collection<ExistingReservation> existingReservations = reservationManager
                        .getExistingReservations(reservation);
                for (ExistingReservation existingReservation : existingReservations) {
                    propagatorByParentEntity.put(existingReservation, new AclEntryPropagator()
                    {
                        @Override
                        public void addAclEntry(EntityState targetEntityState, String userId, Role role)
                        {
                            if (role.equals(Role.OWNER) || role.equals(Role.RESERVATION_REQUEST_USER)) {
                                role = Role.READER;
                            }
                            super.addAclEntry(targetEntityState, userId, role);
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
                    propagatorByParentEntity.put(reservation, new AclEntryPropagator());
                }
            }
            finally {
                entityManager.close();
            }
        }
        return propagatorByParentEntity;
    }

    public Collection<AclEntry> getAclEntries(String userId)
    {
        return null;
    }

    public Collection<AclEntry> getAclEntries(EntityIdentifier entityId)
    {
        return null;
    }

    public Collection<AclEntry> getAclEntries(String userId, EntityIdentifier entityId) throws FaultException
    {
        EntityState entityState = getEntityState(entityId, true);
        EntityUserState entityUserState = entityState.getUserState(userId);
        return entityUserState.aclEntrys;
    }


    protected Collection<AclEntry> onListAclEntrys(String userId, EntityIdentifier entityId, Role role)
            throws FaultException
    {
        return Collections.emptyList();
    }


    private class EntityState
    {
        private EntityIdentifier entityId;

        private Map<EntityState, AclEntryPropagator> parentEntityStates =
                new HashMap<EntityState, AclEntryPropagator>();

        private Map<EntityState, AclEntryPropagator> childEntityStates =
                new HashMap<EntityState, AclEntryPropagator>();

        private Map<String, EntityUserState> entityUserStateByUserId = new HashMap<String, EntityUserState>();

        private Map<Role, Set<String>> userIdsByRole = new HashMap<Role, Set<String>>();

        public EntityState(EntityIdentifier entityId)
        {
            this.entityId = entityId;
        }

        public boolean addAclEntry(AclEntry aclEntry)
        {
            if (!entityId.equals(aclEntry.getObjectId())) {
                throw new RuntimeException();
            }

            String userId = aclEntry.getUserId();
            EntityUserState entityUserState = getUserState(userId);
            if (!entityUserState.addAclEntry(aclEntry)) {
                return false;
            }

            // Propagate ACL to child entity states
            for (Map.Entry<EntityState, AclEntryPropagator> entry : childEntityStates.entrySet()) {
                entry.getValue().addAclEntry(entry.getKey(), userId, aclEntry.getRole());
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
        private Set<AclEntry> aclEntrys = new HashSet<AclEntry>();

        private Set<Role> roles = new HashSet<Role>();

        private Set<ObjectPermission> permissions = new HashSet<ObjectPermission>();

        public boolean addAclEntry(AclEntry aclEntry)
        {
            roles.add(aclEntry.getRole());
            return aclEntrys.add(aclEntry);
        }
    }

    private static class UserState
    {
        private Map<EntityType, Set<Long>> accessibleEntitiesByType = new HashMap<EntityType, Set<Long>>();

        public void addAclEntry(AclEntry aclEntry)
        {
            throw new TodoImplementException();
        }
    }

    private static class AclEntryPropagator
    {
        public void addAclEntry(EntityState targetEntityState, String userId, Role role)
        {
            if (targetEntityState.hasRecord(userId, role)) {
                return;
            }
            logger.debug("Adding dynamic ACL (user: {}, entity: {}, role: {})",
                    new Object[]{userId, targetEntityState.entityId, role});
            AclEntry aclEntry = new AclEntry(userId, targetEntityState.entityId, role);
            targetEntityState.addAclEntry(aclEntry);
        }

        public final void addAclEntrys(EntityState sourceEntityState, EntityState targetEntityState)
        {
            for (EntityUserState entityUserState : sourceEntityState.entityUserStateByUserId.values()) {
                for (AclEntry aclEntry : entityUserState.aclEntrys) {
                    addAclEntry(targetEntityState, aclEntry.getUserId(), aclEntry.getRole());
                }
            }
            for (Map.Entry<EntityState, AclEntryPropagator> entry : sourceEntityState.parentEntityStates.entrySet()) {
                entry.getValue().addAclEntrys(entry.getKey(), sourceEntityState);
            }
        }
    }

    int getEntityCount()
    {
        return entityStateById.size();
    }

    int getAclEntryCount(EntityIdentifier entityId)
    {
        EntityState entityState = entityStateById.get(entityId);
        int aclEntryCount = 0;
        for (EntityUserState entityUserState : entityState.entityUserStateByUserId.values()) {
            aclEntryCount += entityUserState.aclEntrys.size();
        }
        return aclEntryCount;
    }*/
}
