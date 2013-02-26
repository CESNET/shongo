package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Authorization;

/**
 * Implementation of {@link Service}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ServiceImpl implements Service
{
    @Override
    public UserInformation getUserInformation(String userId)
    {
        return Authorization.getInstance().getUserInformation(userId);
    }
}
