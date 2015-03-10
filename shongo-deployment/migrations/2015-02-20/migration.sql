/**
 * 2015-02-20: Adding is_calendar_public parameter for resources. Maybe not necessary (depends on last used commit).
 */
BEGIN TRANSACTION;

UPDATE resource SET calendar_public = false WHERE calendar_public IS NULL;

COMMIT TRANSACTION;