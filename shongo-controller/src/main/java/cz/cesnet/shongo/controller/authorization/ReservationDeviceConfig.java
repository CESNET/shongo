package cz.cesnet.shongo.controller.authorization;

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

    public ReservationDeviceConfig(@NotNull String accessToken, @NotNull String resourceId) {
        this.accessToken = accessToken;
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return "ReservationDeviceConfig{" +
                "accessToken='" + accessToken + '\'' +
                ", resourceId='" + resourceId + '\'' +
                '}';
    }
}
