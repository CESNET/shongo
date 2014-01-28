package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.TodoImplementException;

import java.util.Collection;
import java.util.Locale;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MultipleRoomNotification extends ConfigurableNotification
{
    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        throw new TodoImplementException();
    }
}
