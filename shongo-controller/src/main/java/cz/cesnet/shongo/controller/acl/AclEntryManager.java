package cz.cesnet.shongo.controller.acl;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.authorization.Authorization;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents a service for managing {@link cz.cesnet.shongo.controller.acl.AclEntry}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AclEntryManager extends AbstractManager
{
    /**
     * @see AclProvider
     */
    protected final AclProvider aclProvider;

    /**
     * Constructor.
     *
     * @param entityManager sets the {@link #entityManager}
     * @param aclProvider sets the {@link #aclProvider}
     */
    public AclEntryManager(EntityManager entityManager, AclProvider aclProvider)
    {
        super(entityManager);
        this.aclProvider = aclProvider;
    }

    /**
     * @param identity
     * @param objectIdentity
     * @param role
     * @return newly created {@link AclEntry}
     */
    public AclEntry createEntry(AclIdentity identity, AclObjectIdentity objectIdentity, String role)
    {
        try {
            return entityManager.createNamedQuery("AclEntry.find", AclEntry.class)
                    .setParameter("identity", identity)
                    .setParameter("objectIdentity", objectIdentity)
                    .setParameter("role", role)
                    .getSingleResult();
        }
        catch (NoResultException exception) {
            AclEntry entry = new AclEntry();
            entry.setIdentity(identity);
            entry.setObjectIdentity(objectIdentity);
            entry.setRole(role);
            entityManager.persist(entry);
            return entry;
        }
    }

    /**
     * @param entry to be deleted
     */
    public void deleteEntry(AclEntry entry)
    {
        entityManager.remove(entry);
    }

    /**
     * @param entryId
     * @return {@link AclEntry} with given {@code entryId}
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *          when {@link AclEntry} doesn't exist
     */
    public AclEntry getAclEntry(Long entryId) throws CommonReportSet.ObjectNotExistsException
    {
        AclEntry aclEntry = entityManager.find(AclEntry.class, entryId);
        if (aclEntry == null) {
            return ControllerReportSetHelper.throwObjectNotExistFault(AclEntry.class, entryId);
        }
        return aclEntry;
    }

    /**
     * @param identity
     * @param objectIdentity
     * @param role
     * @return {@link AclEntry} for given parameters or {@code null} if it doesn't exist
     */
    public AclEntry getAclEntry(AclIdentity identity, AclObjectIdentity objectIdentity, String role)
    {
        List<AclEntry> entries = entityManager.createNamedQuery("AclEntry.find", AclEntry.class)
                .setParameter("identity", identity)
                .setParameter("objectIdentity", objectIdentity)
                .setParameter("role", role)
                .getResultList();
        if (entries.size() == 1) {
            return entries.get(0);
        }
        else if (entries.size() == 0) {
            return null;
        }
        else {
            throw new RuntimeException(String.format("Multiple ACL entries (identity: %s, object: %s, role: %s) exist.",
                    identity, objectIdentity, role));
        }
    }

    /**
     * @param identity
     * @param objectIdentity
     * @param role
     * @return list of {@link cz.cesnet.shongo.controller.acl.AclEntry}s for given parameters
     */
    public List<AclEntry> listAclEntries(AclIdentity identity, AclObjectIdentity objectIdentity, String role)
    {
        return entityManager.createQuery("SELECT entry FROM AclEntry entry"
                + " WHERE (:identityNull = TRUE OR entry.identity = :identity)"
                + " AND (:objectIdentityNull = TRUE OR entry.objectIdentity = :objectIdentity)"
                + " AND (:roleNull = TRUE OR entry.role = :role)", AclEntry.class)
                .setParameter("identity", identity)
                .setParameter("identityNull", identity == null)
                .setParameter("objectIdentity", objectIdentity)
                .setParameter("objectIdentityNull", objectIdentity == null)
                .setParameter("role", role)
                .setParameter("roleNull", role == null)
                .getResultList();
    }

    /**
     * @param identities
     * @return list of {@link cz.cesnet.shongo.controller.acl.AclEntry} for given {@code identity}
     */
    public List<AclEntry> listAclEntries(Set<AclIdentity> identities)
    {
        return entityManager.createNamedQuery("AclEntry.findByIdentity", AclEntry.class)
                .setParameter("identities", identities)
                .getResultList();
    }

    /**
     * @param objectIdentity
     * @return list of {@link cz.cesnet.shongo.controller.acl.AclEntry}s for given {@code objectIdentity}
     */
    public List<AclEntry> listAclEntries(AclObjectIdentity objectIdentity)
    {
        if (objectIdentity == null) {
            return Collections.emptyList();
        }
        return entityManager.createNamedQuery("AclEntry.findByObjectIdentity", AclEntry.class)
                    .setParameter("objectIdentity", objectIdentity)
                    .getResultList();
    }
}
