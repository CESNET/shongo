package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.UserInformation;

/**
 * Defines a set of API methods which controller provides through JADE middle-ware.
 * For each method is defined one {@link ControllerAgentAction} class which executes the method.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Service
{
    /**
     * @param userId for which the {@link UserInformation} should be returned
     * @return {@link UserInformation} for given {@code userId}
     */
    public UserInformation getUserInformation(String userId);
}
