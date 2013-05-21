package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.api.annotation.Transient;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Represents a relationship between old and new {@link AbstractReservationRequest}s for modifications.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ModifiedReservationRequest
{
    /**
     * @see PrimaryKey
     */
    @EmbeddedId
    private PrimaryKey primaryKey = new PrimaryKey();

    /**
     * @return {@link #primaryKey#oldReservationRequest}
     */
    @Transient
    public AbstractReservationRequest getOldReservationRequest()
    {
        return primaryKey.oldReservationRequest;
    }

    /**
     * @param oldReservationRequest sets the {@link #primaryKey#oldReservationRequest}
     */
    @Transient
    public void setOldReservationRequest(AbstractReservationRequest oldReservationRequest)
    {
        this.primaryKey.oldReservationRequest = oldReservationRequest;
    }

    /**
     * @return {@link #primaryKey#newReservationRequest}
     */
    @Transient
    public AbstractReservationRequest getNewReservationRequest()
    {
        return primaryKey.newReservationRequest;
    }

    /**
     * @param newReservationRequest sets the {@link #primaryKey#newReservationRequest}
     */
    @Transient
    public void setNewReservationRequest(AbstractReservationRequest newReservationRequest)
    {
        this.primaryKey.newReservationRequest = newReservationRequest;
    }

    /**
     * @return {@link #primaryKey#latestReservationRequest}
     */
    @Transient
    public AbstractReservationRequest getLatestReservationRequest()
    {
        return primaryKey.latestReservationRequest;
    }

    /**
     * @param latestReservationRequest sets the {@link #primaryKey#latestReservationRequest}
     */
    @Transient
    public void setLatestReservationRequest(AbstractReservationRequest latestReservationRequest)
    {
        this.primaryKey.latestReservationRequest = latestReservationRequest;
    }

    /**
     * Represents a primary key for the {@link ModifiedReservationRequest}.
     */
    @Embeddable
    public static class PrimaryKey implements Serializable
    {
        /**
         * Old version of {@link AbstractReservationRequest} which has been modified to {@link #newReservationRequest}.
         */
        @OneToOne(optional = false)
        @JoinColumn(name="old_reservation_request_id")
        private AbstractReservationRequest oldReservationRequest;

        /**
         * New version of {@link AbstractReservationRequest} which has been modified from {@link #oldReservationRequest}.
         */
        @OneToOne(optional = false)
        @JoinColumn(name="new_reservation_request_id")
        private AbstractReservationRequest newReservationRequest;

        /**
         * Current (latest) version of {@link AbstractReservationRequest} to which the {@link #newReservationRequest}
         * itself has been (recursively) modified or the {@link #newReservationRequest} if it has not been modified yet.
         */
        @ManyToOne(optional = false)
        @JoinColumn(name="latest_reservation_request_id")
        private AbstractReservationRequest latestReservationRequest;

        @Override
        public boolean equals(Object object)
        {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            PrimaryKey that = (PrimaryKey) object;
            if (!latestReservationRequest.equals(that.latestReservationRequest)) {
                return false;
            }
            if (!newReservationRequest.equals(that.newReservationRequest)) {
                return false;
            }
            if (!oldReservationRequest.equals(that.oldReservationRequest)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int result = oldReservationRequest.hashCode();
            result = 31 * result + newReservationRequest.hashCode();
            result = 31 * result + latestReservationRequest.hashCode();
            return result;
        }
    }
}
