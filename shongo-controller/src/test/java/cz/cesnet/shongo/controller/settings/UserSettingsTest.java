package cz.cesnet.shongo.controller.settings;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.api.UserSettings;

import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link UserSettings}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSettingsTest extends AbstractControllerTest
{
    @Override
    protected void onInit()
    {
        getAuthorization().addAdministratorUserId(getUserId(SECURITY_TOKEN_USER1));
        super.onInit();
    }

    @Test
    public void test() throws Exception
    {
        // Disable web service
        UserSettings userSettings = new UserSettings();
        userSettings.setUseWebService(false);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN_USER1, userSettings);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN_USER2, userSettings);

        // Check admin mode enabled user
        userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN_USER1);
        Assert.assertEquals(null, userSettings.getLocale());
        Assert.assertEquals(null, userSettings.getHomeTimeZone());
        Assert.assertFalse(userSettings.getAdministrationMode());

        // Check normal user
        userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN_USER2);
        Assert.assertEquals(null, userSettings.getLocale());
        Assert.assertEquals(null, userSettings.getHomeTimeZone());
        Assert.assertFalse(userSettings.getAdministrationMode());

        // Check update locale
        userSettings.setLocale(UserSettings.LOCALE_CZECH);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN_USER2, userSettings);
        userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN_USER2);
        Assert.assertEquals(UserSettings.LOCALE_CZECH, userSettings.getLocale());
        Assert.assertEquals(null, userSettings.getHomeTimeZone());
        Assert.assertFalse(userSettings.getAdministrationMode());

        // Check update language
        userSettings.setHomeTimeZone(DateTimeZone.forID("+05:00"));
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN_USER2, userSettings);
        userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN_USER2);
        Assert.assertEquals(UserSettings.LOCALE_CZECH, userSettings.getLocale());
        Assert.assertEquals(DateTimeZone.forID("+05:00"), userSettings.getHomeTimeZone());
        Assert.assertFalse(userSettings.getAdministrationMode());

        // Check admin mode not working for normal user
        userSettings.setAdministrationMode(true);
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN_USER2, userSettings);
        userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN_USER2);
        Assert.assertFalse(userSettings.getAdministrationMode());

        // Test custom attribute
        userSettings.setAttribute("client.test", "value");
        getAuthorizationService().updateUserSettings(SECURITY_TOKEN_USER2, userSettings);
        userSettings = getAuthorizationService().getUserSettings(SECURITY_TOKEN_USER2);
        Assert.assertEquals("value", userSettings.getAttribute("client.test"));
    }
}
