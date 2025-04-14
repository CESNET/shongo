package cz.cesnet.shongo.controller.rest.models.report;

import cz.cesnet.shongo.controller.rest.models.users.SettingsModel;
import lombok.Data;
import org.joda.time.DateTimeZone;

/**
 * Represents meta data for {@link ReportModel}.
 */
@Data
public class MetaModel
{

    private DateTimeZone timeZone;
    private SettingsModel settings;
}
