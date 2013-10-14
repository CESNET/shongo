package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.Allocation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
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
        @Index(name = "acl_record_entity", columnNames = {"entity_id", "entity_type"})
})
public class AclRecord extends PersistentObject
{
    /**
     * User-id of the ACL.
     */
    private String userId;

    /**
     * @see EntityId
     */
    private EntityId entityId;

    /**
     * {@link Role} of the ACL.
     */
    private Role role;

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
    public EntityId getEntityId()
    {
        return entityId;
    }

    /**
     * @param entityId sets the {@link #entityId}
     */
    public void setEntityId(EntityId entityId)
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

    public static enum EntityType
    {
        /**
         * @see cz.cesnet.shongo.controller.EntityType#RESOURCE
         */
        RESOURCE(cz.cesnet.shongo.controller.EntityType.RESOURCE, Resource.class),

        /**
         * @see cz.cesnet.shongo.controller.EntityType#RESERVATION_REQUEST
         */
        ALLOCATION(cz.cesnet.shongo.controller.EntityType.RESERVATION_REQUEST, Allocation.class),

        /**
         * @see cz.cesnet.shongo.controller.EntityType#RESERVATION
         */
        RESERVATION(cz.cesnet.shongo.controller.EntityType.RESERVATION, Reservation.class),

        /**
         * @see cz.cesnet.shongo.controller.EntityType#EXECUTABLE
         */
        EXECUTABLE(cz.cesnet.shongo.controller.EntityType.EXECUTABLE, Executable.class);

        /**
         * @see cz.cesnet.shongo.controller.EntityType
         */
        private final cz.cesnet.shongo.controller.EntityType entityType;

        /**
         * Class of entities.
         */
        private Class<? extends PersistentObject> entityClass;

        /**
         * Constructor.
         *
         * @param entityType sets the {@link #entityType}
         */
        private EntityType(cz.cesnet.shongo.controller.EntityType entityType,
                Class<? extends PersistentObject> entityClass)
        {
            this.entityType = entityType;
            this.entityClass = entityClass;
        }

        /**
         * @return {@link #entityType}
         */
        public cz.cesnet.shongo.controller.EntityType getEntityType()
        {
            return entityType;
        }

        /**
         * @return {@link #entityClass}
         */
        public Class<? extends PersistentObject> getEntityClass()
        {
            return entityClass;
        }

        /**
         * @param entityType
         * @return {@link cz.cesnet.shongo.controller.EntityType} from given {@code entityType}
         */
        public static EntityType fromEntityType(cz.cesnet.shongo.controller.EntityType entityType)
        {
            switch (entityType) {
                case RESOURCE:
                    return RESOURCE;
                case RESERVATION_REQUEST:
                    return ALLOCATION;
                case RESERVATION:
                    return RESERVATION;
                case EXECUTABLE:
                    return EXECUTABLE;
                default:
                    throw new TodoImplementException(entityType);
            }
        }
    }

    @Embeddable
    public static class EntityId
    {
        /**
         * {@link EntityType} of the identifier.
         */
        private EntityType entityType;

        /**
         * Identifier value.
         */
        private Long persistenceId;

        /**
         * Constructor.
         */
        public EntityId()
        {
        }

        /**
         * Constructor.
         *
         * @param entityType    sets the {@link #entityType}
         * @param persistenceId sets the {@link #persistenceId}
         */
        public EntityId(EntityType entityType, Long persistenceId)
        {
            this.entityType = entityType;
            this.persistenceId = persistenceId;
        }

        /**
         * Constructor.
         *
         * @param persistentObject sets the {@link #entityType} and the {@link #persistenceId}
         */
        public EntityId(PersistentObject persistentObject)
        {
            if (persistentObject instanceof AbstractReservationRequest) {
                AbstractReservationRequest reservationRequest = (AbstractReservationRequest) persistentObject;
                this.entityType = EntityType.ALLOCATION;
                this.persistenceId = reservationRequest.getAllocation().getId();
            }
            else {
                this.persistenceId = persistentObject.getId();
                if (persistentObject instanceof Resource) {
                    this.entityType = EntityType.RESOURCE;
                }
                else if (persistentObject instanceof Reservation) {
                    this.entityType = EntityType.RESERVATION;
                }
                else if (persistentObject instanceof Executable) {
                    this.entityType = EntityType.EXECUTABLE;
                }
                else {
                    throw new TodoImplementException(persistentObject.getClass());
                }
            }
        }

        /**
         * @return {@link #entityType}
         */
        @Column
        @Enumerated(EnumType.STRING)
        public EntityType getEntityType()
        {
            return entityType;
        }

        /**
         * @param entityType sets the {@link #entityType}
         */
        public void setEntityType(EntityType entityType)
        {
            this.entityType = entityType;
        }

        /**
         * @return {@link #persistenceId}
         */
        @Column
        public Long getPersistenceId()
        {
            return persistenceId;
        }

        /**
         * @param persistenceId sets the {@link #persistenceId}
         */
        public void setPersistenceId(Long persistenceId)
        {
            this.persistenceId = persistenceId;
        }

        /**
         * @return {@link EntityType#getEntityClass()}
         */
        @Transient
        public Class<? extends PersistentObject> getEntityClass()
        {
            return entityType.getEntityClass();
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

            EntityId entityId = (EntityId) o;

            if (entityType != entityId.entityType) {
                return false;
            }
            if (!persistenceId.equals(entityId.persistenceId)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int result = entityType.hashCode();
            result = 31 * result + persistenceId.hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            return entityType + ":" + persistenceId;
        }
    }
}
