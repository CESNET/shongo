package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import org.hibernate.annotations.Index;

import javax.persistence.*;

/**
 * Represents an single ACL record that an user has a role for an entity.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entity_id", "entity_type", "role"}))
@org.hibernate.annotations.Table(appliesTo = "acl_record", indexes = {
        @Index(name = "acl_record_entity_type", columnNames = {"entity_type"}),
        @Index(name = "acl_record_entity_id", columnNames = {"entity_id"})
})
public class AclRecord extends PersistentObject
{
    /**
     * User-id of the ACL.
     */
    private String userId;

    /**
     * {@link EntityIdentifier} of the ACL.
     */
    private EntityIdentifier entityId;

    /**
     * {@link Role} of the ACL.
     */
    private Role role;

    /**
     * Specifies whether the {@link AclRecord} has been deleted.
     */
    private boolean deleted;

    /**
     * @see PropagationState
     */
    private PropagationState propagationState;

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false)
    @Index(name = "acl_record_user")
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #entityId}
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "entityType", column = @Column(name = "entity_type", nullable = false)),
            @AttributeOverride(name = "persistenceId", column = @Column(name = "entity_Id", nullable = false))
    })
    public EntityIdentifier getEntityId()
    {
        return entityId;
    }

    /**
     * @param entityId sets the {@link #entityId}
     */
    @Index(name = "acl_record_entity_id", columnNames = {"entity_type"})
    public void setEntityId(EntityIdentifier entityId)
    {
        this.entityId = entityId;
    }

    /**
     * @return {@link #role}
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Index(name = "acl_record_role")
    public Role getRole()
    {
        return role;
    }

    /**
     * @param role sets the {@link #role}
     */
    public void setRole(Role role)
    {
        this.role = role;
    }

    /**
     * @return {@link #deleted}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isDeleted()
    {
        return deleted;
    }

    /**
     * @param deleted sets the {@link #deleted}
     */
    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    /**
     * @return {@link #propagationState}
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public PropagationState getPropagationState()
    {
        return propagationState;
    }

    /**
     * @param propagationState sets the {@link #propagationState}
     */
    public void setPropagationState(PropagationState propagationState)
    {
        this.propagationState = propagationState;
    }

    /**
     * @return {@link AclRecord} converted to {@link cz.cesnet.shongo.controller.api.AclRecord}
     */
    public cz.cesnet.shongo.controller.api.AclRecord toApi()
    {
        cz.cesnet.shongo.controller.api.AclRecord aclRecordApi = new cz.cesnet.shongo.controller.api.AclRecord();
        aclRecordApi.setId(getId().toString());
        aclRecordApi.setUserId(getUserId());
        aclRecordApi.setEntityId(getEntityId().toId());
        aclRecordApi.setRole(getRole());
        return aclRecordApi;
    }

    @PrePersist
    protected void onCreate()
    {
        if (propagationState == null) {
            if (entityId.getEntityType().isRolePropagatable(role) ) {
                propagationState = PropagationState.NOT_PROPAGATED;
            }
            else {
                propagationState = PropagationState.PROPAGATION_SKIPPED;
            }
        }
    }

    /**
     * State of the propagation to authorization server.
     */
    public static enum PropagationState
    {
        /**
         * {@link AclRecord} should not be propagated to authorization server.
         */
        PROPAGATION_SKIPPED,

        /**
         * {@link AclRecord} is prepared for propagation to authorization server.
         */
        NOT_PROPAGATED,

        /**
         * {@link AclRecord} has already been propagated to authorization server.
         */
        PROPAGATED
    }
}
