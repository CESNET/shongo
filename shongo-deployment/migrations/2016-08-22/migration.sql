/**
 * 2016-08-22: preparation for materializing views
 */
BEGIN TRANSACTION;

DROP VIEW IF EXISTS executable_summary;
DROP VIEW IF EXISTS specification_summary;

COMMIT TRANSACTION;