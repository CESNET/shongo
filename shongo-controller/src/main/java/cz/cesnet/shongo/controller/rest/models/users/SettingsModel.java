package cz.cesnet.shongo.controller.rest.models.users;

import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.UserSettings;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Locale;

/**
 * Represents {@link UserSettings}.
 *
 * @author Filip Karnis
 */
@Data
@NoArgsConstructor
public class SettingsModel
{

    private Boolean useWebService;
    private Locale locale;
    private DateTimeZone homeTimeZone;
    private DateTimeZone currentTimeZone;
    private Boolean administrationMode;
    private List<SystemPermission> permissions;

    public SettingsModel(UserSettings userSettings, List<SystemPermission> permissions)
    {
        this.useWebService = userSettings.isUseWebService();
        this.locale = userSettings.getLocale();
        this.homeTimeZone = userSettings.getHomeTimeZone();
        this.currentTimeZone = userSettings.getCurrentTimeZone();
        this.administrationMode = userSettings.getAdministrationMode();
        this.permissions = permissions;
    }
}
