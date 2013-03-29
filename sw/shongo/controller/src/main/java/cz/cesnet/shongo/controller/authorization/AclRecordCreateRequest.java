package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.request.ReservationRequestSet;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
//@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entity_id", "role"}))
public class AclRecordCreateRequest extends PersistentObject
{
    private String userId;

    private String entityId;

    private Role role;

    private String parentAclRecordId;

    private AclRecordCreateRequest parentCreateRequest;

    private List<AclRecordCreateRequest> childCreateRequests = new LinkedList<AclRecordCreateRequest>();

    @Column
    @Index(name = "user_index")
    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    @Column
    @Index(name = "entity_index")
    public String getEntityId()
    {
        return entityId;
    }

    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }

    @Column
    @Enumerated(EnumType.STRING)
    @Index(name = "role_index")
    public Role getRole()
    {
        return role;
    }

    public void setRole(Role role)
    {
        this.role = role;
    }

    @OneToOne
    @Access(AccessType.FIELD)
    public AclRecordCreateRequest getParentCreateRequest()
    {
        return parentCreateRequest;
    }

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


    public String getParentAclRecordId()
    {
        return parentAclRecordId;
    }

    public void setParentAclRecordId(String parentAclRecordId)
    {
        this.parentAclRecordId = parentAclRecordId;
    }

    @OneToMany(mappedBy = "parentCreateRequest")
    @Access(AccessType.FIELD)
    public List<AclRecordCreateRequest> getChildCreateRequests()
    {
        return childCreateRequests;
    }
}
