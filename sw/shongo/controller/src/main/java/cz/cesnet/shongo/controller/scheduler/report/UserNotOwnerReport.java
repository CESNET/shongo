package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class UserNotOwnerReport extends Report
{
    /**
     * User-id.
     */
    private String userId;

    /**
     * Constructor.
     */
    public UserNotOwnerReport()
    {
    }

    /**
     * Constructor.
     *
     * @param userId ses the {@link #userId}
     */
    public UserNotOwnerReport(String userId)
    {
        setUserId(userId);
    }

    /**
     * @return {@link #userId}
     */
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

    @Override
    @Transient
    public String getText()
    {
        String text = formatUser(userId);
        return text.substring(0, 1).toUpperCase() + text.substring(1) + " is not resource owner.";
    }

    /**
     * @param userId
     * @return formatted resource
     */
    public static String formatUser(String userId)
    {

        return String.format("%s (id: %s)'", Authorization.UserInformation.getInstance(userId).getFullName(), userId);
    }
}
