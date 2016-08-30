/**
 * 2016-08-22: preparation for materializing views
 */
BEGIN TRANSACTION;

DROP TABLE IF EXISTS executable_summary;
DROP TABLE IF EXISTS specification_summary;

DROP VIEW IF EXISTS executable_summary;
DROP VIEW IF EXISTS specification_summary;

COMMIT TRANSACTION;