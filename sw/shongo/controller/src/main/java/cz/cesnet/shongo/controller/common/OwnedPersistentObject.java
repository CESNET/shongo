package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a {@link cz.cesnet.shongo.PersistentObject} which is owned by shongo user.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@MappedSuperclass
public abstract class OwnedPersistentObject extends PersistentObject
{
    /**
     * User-id of an user who is owner of the {@link OwnedPersistentObject}.
     */
    private String userId;

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false)
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
}
