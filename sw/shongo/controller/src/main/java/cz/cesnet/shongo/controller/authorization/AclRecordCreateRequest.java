package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Role;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a request for creation of {@link AclRecord},
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AclRecordCreateRequest extends PersistentObject
{
    /**
     * {@link AclRecord#userId}
     */
    private String userId;

    /**
     * {@link AclRecord#entityId}
     */
    private String entityId;

    /**
     * {@link AclRecord#role}
     */
    private Role role;

    /**
     * Parent {@link AclRecord#id}
     */
    private String parentAclRecordId;

    /**
     * Parent {@link AclRecordCreateRequest}.
     */
    private AclRecordCreateRequest parentCreateRequest;

    /**
     * Child {@link AclRecordCreateRequest}s.
     */
    private List<AclRecordCreateRequest> childCreateRequests = new LinkedList<AclRecordCreateRequest>();

    /**
     * @return {@link #userId}
     */
    @Column
    @Index(name = "user_index")
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
    @Column
    @Index(name = "entity_index")
    public String getEntityId()
    {
        return entityId;
    }

    /**
     * @param entityId sets the {@link #entityId}
     */
    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }

    /**
     * @return {@link #role}
     */
    @Column
    @Enumerated(EnumType.STRING)
    @Index(name = "role_index")
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
     * @return {@link #parentAclRecordId}
     */
    @Column
    public String getParentAclRecordId()
    {
        return parentAclRecordId;
    }

    /**
     * @param parentAclRecordId sets the {@link #parentAclRecordId}
     */
    public void setParentAclRecordId(String parentAclRecordId)
    {
        this.parentAclRecordId = parentAclRecordId;
    }

    /**
     * @return {@link #parentCreateRequest}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public AclRecordCreateRequest getParentCreateRequest()
    {
        return parentCreateRequest;
    }

    /**
     * @param parentCreateRequest sets the {@link #parentCreateRequest}
     */
    public void setParentCreateRequest(AclRecordCreateRequest parentCreateRequest)
    {
        // Manage bidirectional association
        if (parentCreateRequest != this.parentCreateRequest) {
            if (this.parentCreateRequest != null) {
                AclRecordCreateRequest oldReservationRequestSet = this.parentCreateRequest;
                this.parentCreateRequest = null;
                oldReservationRequestSet.childCreateRequests.remove(this);
            }
            if (parentCreateRequest != null) {
                this.parentCreateRequest = parentCreateRequest;
                this.parentCreateRequest.childCreateRequests.add(this);
            }
        }
    }

    /**
     * @return {@link #childCreateRequests}
     */
    @OneToMany(mappedBy = "parentCreateRequest")
    @Access(AccessType.FIELD)
    public List<AclRecordCreateRequest> getChildCreateRequests()
    {
        return childCreateRequests;
    }
}
