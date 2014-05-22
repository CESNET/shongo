package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ObjectRole;

/**
 * Represents a record in Shongo ACL which means that user with specified {@link #identityPrincipalId} has role
 * with specified {@link #role} for resource with specified {@link #objectId}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclEntry extends IdentifiedComplexType
{
    /**
     * @see AclIdentityType
     */
    private AclIdentityType identityType;

    /**
     * Principal-id of the {@link #identityType} for this {@link AclEntry}.
     */
    private String identityPrincipalId;

    /**
     * Identifier of object of some {@link cz.cesnet.shongo.controller.ObjectType} for which the user
     * gets granted the {@link #role}.
     */
    private String objectId;

    /**
     * {@link ObjectRole} which the user gets granted for the object with {@link #objectId}.
     */
    private ObjectRole role;

    /**
     * Specifies whether {@link AclEntry} is not referenced by another {@link AclEntry} and thus it can be deleted.
     */
    private boolean deletable;

    /**
     * Constructor.
     */
    public AclEntry()
    {
    }

    /**
     * Constructor.
     *
     * @param identityType        sets the {@link #identityType}
     * @param identityPrincipalId sets the {@link #identityPrincipalId}
     * @param objectId            sets the {@link #objectId}
     * @param role                sets the {@link #role}
     */
    public AclEntry(AclIdentityType identityType, String identityPrincipalId, String objectId, ObjectRole role)
    {
        this.identityType = identityType;
        this.identityPrincipalId = identityPrincipalId;
        this.objectId = objectId;
        this.role = role;
    }

    /**
     * Constructor.
     *
     * @param userId   sets the {@link #identityPrincipalId} and {@link #identityType} to {@link AclIdentityType#USER}
     * @param objectId sets the {@link #objectId}
     * @param role     sets the {@link #role}
     */
    public AclEntry(String userId, String objectId, ObjectRole role)
    {
        this(AclIdentityType.USER, userId, objectId, role);
    }

    /**
     * @return {@link #identityType}
     */
    public AclIdentityType getIdentityType()
    {
        return identityType;
    }

    /**
     * @param identityType sets thye {@link #identityType}
     */
    public void setIdentityType(AclIdentityType identityType)
    {
        this.identityType = identityType;
    }

    /**
     * @return {@link #identityPrincipalId}
     */
    public String getIdentityPrincipalId()
    {
        return identityPrincipalId;
    }

    /**
     * @param identityPrincipalId sets the {@link #identityPrincipalId}
     */
    public void setIdentityPrincipalId(String identityPrincipalId)
    {
        this.identityPrincipalId = identityPrincipalId;
    }

    /**
     * @return {@link #objectId}
     */
    public String getObjectId()
    {
        return objectId;
    }

    /**
     * @param objectId sets the {@link #objectId}
     */
    public void setObjectId(String objectId)
    {
        this.objectId = objectId;
    }

    /**
     * @return {@link #role}
     */
    public ObjectRole getRole()
    {
        return role;
    }

    /**
     * @param role {@link #role}
     */
    public void setRole(ObjectRole role)
    {
        this.role = role;
    }

    /**
     * @return {@link #deletable}
     */
    public boolean isDeletable()
    {
        return deletable;
    }

    /**
     * @param deletable sets the {@link #deletable}
     */
    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    @Override
    public String toString()
    {
        return String.format("AclEntry (user: %s, object: %s, role: %s)", identityPrincipalId, objectId, role);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AclEntry record = (AclEntry) o;

        if (objectId != null ? !objectId.equals(record.objectId) : record.objectId != null) {
            return false;
        }
        if (role != record.role) {
            return false;
        }
        if (identityPrincipalId != null ?
                !identityPrincipalId.equals(record.identityPrincipalId) : record.identityPrincipalId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = identityPrincipalId != null ? identityPrincipalId.hashCode() : 0;
        result = 31 * result + (objectId != null ? objectId.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }

    private static final String IDENTITY_TYPE = "identityType";
    private static final String IDENTITY_PRINCIPAL_ID = "identityPrincipalId";
    private static final String OBJECT_ID = "objectId";
    private static final String ROLE = "role";
    private static final String DELETABLE = "deletable";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(IDENTITY_TYPE, identityType);
        dataMap.set(IDENTITY_PRINCIPAL_ID, identityPrincipalId);
        dataMap.set(OBJECT_ID, objectId);
        dataMap.set(ROLE, role);
        dataMap.set(DELETABLE, deletable);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        identityType = dataMap.getEnumRequired(IDENTITY_TYPE, AclIdentityType.class);
        identityPrincipalId = dataMap.getString(IDENTITY_PRINCIPAL_ID, Controller.USER_ID_COLUMN_LENGTH);
        objectId = dataMap.getString(OBJECT_ID);
        role = dataMap.getEnum(ROLE, ObjectRole.class);
        deletable = dataMap.getBool(DELETABLE);
    }
}
