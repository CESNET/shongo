package cz.cesnet.shongo.controller.acl;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.AclIdentityType;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import java.util.*;

/**
 * Represents a service for managing {@link AclEntry}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AclProvider
{
    private EntityManagerFactory entityManagerFactory;

    private Map<AclIdentityType, Map<String, AclIdentity>> identities =
            new HashMap<AclIdentityType, Map<String, AclIdentity>>();

    private Map<String, AclObjectClass> objectClasses =
            new HashMap<String, AclObjectClass>();

    private Map<AclObjectClass, Map<Long, AclObjectIdentity>> objectIdentities =
            new HashMap<AclObjectClass, Map<Long, AclObjectIdentity>>();

    public AclProvider(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    public synchronized AclIdentity getIdentity(AclIdentityType type, String principalId)
    {
        Map<String, AclIdentity> identitiesByPrincipalId = identities.get(type);
        if (identitiesByPrincipalId == null) {
            identitiesByPrincipalId = new HashMap<String, AclIdentity>();
            identities.put(type, identitiesByPrincipalId);
        }
        AclIdentity aclIdentity = identitiesByPrincipalId.get(principalId);
        if (aclIdentity == null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                try {
                    aclIdentity = entityManager.createNamedQuery("AclIdentity.find", AclIdentity.class)
                            .setParameter("type", type).setParameter("principalId", principalId).getSingleResult();
                }
                catch (NoResultException exception) {
                    aclIdentity = new AclIdentity();
                    aclIdentity.setType(type);
                    aclIdentity.setPrincipalId(principalId);
                    entityManager.getTransaction().begin();
                    entityManager.persist(aclIdentity);
                    entityManager.getTransaction().commit();
                }
            }
            finally {
                entityManager.close();
            }
            identitiesByPrincipalId.put(principalId, aclIdentity);
        }
        return aclIdentity;
    }

    public synchronized AclObjectClass getObjectClass(Class<? extends PersistentObject> objectClass)
    {
        String className = getObjectClassName(objectClass);
        AclObjectClass aclObjectClass = objectClasses.get(className);
        if (aclObjectClass == null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                try {
                    aclObjectClass = entityManager.createNamedQuery("AclObjectClass.find", AclObjectClass.class)
                            .setParameter("className", className).getSingleResult();
                }
                catch (NoResultException exception) {
                    aclObjectClass = new AclObjectClass();
                    aclObjectClass.setClassName(className);
                    entityManager.getTransaction().begin();
                    entityManager.persist(aclObjectClass);
                    entityManager.getTransaction().commit();
                }
            }
            finally {
                entityManager.close();
            }
            objectClasses.put(className, aclObjectClass);
        }
        return aclObjectClass;
    }

    public synchronized AclObjectIdentity getObjectIdentity(PersistentObject object)
    {
        AclObjectClass objectClass = getObjectClass(object.getClass());
        Long objectId = getObjectId(object);
        Map<Long, AclObjectIdentity> objectIdentitiesByObjectId = objectIdentities.get(objectClass);
        if (objectIdentitiesByObjectId == null) {
            objectIdentitiesByObjectId = new HashMap<Long, AclObjectIdentity>();
            objectIdentities.put(objectClass, objectIdentitiesByObjectId);
        }
        AclObjectIdentity aclObjectIdentity = objectIdentitiesByObjectId.get(objectId);
        if (aclObjectIdentity == null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                try {
                    aclObjectIdentity = entityManager
                            .createNamedQuery("AclObjectIdentity.find", AclObjectIdentity.class)
                            .setParameter("objectClass", objectClass).setParameter("objectId", objectId)
                            .getSingleResult();
                }
                catch (NoResultException exception) {
                    entityManager.getTransaction().begin();
                    aclObjectIdentity = new AclObjectIdentity();
                    aclObjectIdentity.setObjectClass(objectClass);
                    aclObjectIdentity.setObjectId(objectId);
                    entityManager.persist(aclObjectIdentity);
                    entityManager.getTransaction().commit();
                }
            }
            finally {
                entityManager.close();
            }
            objectIdentitiesByObjectId.put(objectId, aclObjectIdentity);
        }
        return aclObjectIdentity;
    }

    /**
     * @param objectClass
     * @return class name for given {@code objectClass}
     */
    protected abstract String getObjectClassName(Class<? extends PersistentObject> objectClass);

    /**
     * @param object
     * @return object identifier for given {@code objectClass}
     */
    protected abstract Long getObjectId(PersistentObject object);
}
