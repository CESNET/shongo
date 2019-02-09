/**
 * 2019-02-09: preparation for reservation_request_summary view upgrade
 */
BEGIN TRANSACTION;

DROP VIEW IF EXISTS reservation_request_summary;

COMMIT TRANSACTION;