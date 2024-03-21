package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.api.UserInformation;
import lombok.Getter;
import javax.validation.constraints.NotNull;

/**
 * Configuration for a physical device which can make reservations for a particular resource.
 *
 * @author Michal Drobňák <mi.drobnak@gmail.com>
 */
@Getter
public final class ReservationDeviceConfig {
    private final String accessToken;
    private final String resourceId;
    private final UserData userData;

    public ReservationDeviceConfig(@NotNull String accessToken, @NotNull String resourceId) {
        this.accessToken = accessToken;
        this.resourceId = resourceId;
        this.userData = createUserData();
    }

    @Override
    public String toString() {
        return "ReservationDeviceConfig{" +
                "accessToken='" + accessToken + '\'' +
                ", resourceId='" + resourceId + '\'' +
                '}';
    }

    private UserData createUserData() {
        UserData userData = new UserData();
        UserInformation userInformation = userData.getUserInformation();
        UserAuthorizationData userAuthData = new UserAuthorizationData(0);

        String userId = "reservation-device-" + resourceId;
        String name = "Reservation Device For " + resourceId;

        userInformation.setUserId(userId);
        userInformation.setFullName(name);
        userData.setUserAuthorizationData(userAuthData);

        return userData;
    }
}
