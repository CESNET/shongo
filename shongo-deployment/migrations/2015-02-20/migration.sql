/**
 * 2015-02-20: Adding is_calendar_public parameter for resources.
 */
BEGIN TRANSACTION;

UPDATE resource SET calendar_public = false WHERE calendar_public IS NULL;

COMMIT TRANSACTION;