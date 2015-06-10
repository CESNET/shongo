/**
 * 2015-06-10: Drops NOT NULL for resource_id in resource_tag (added foreign resources).
 */
BEGIN TRANSACTION;
ALTER TABLE resource_tag ALTER COLUMN resource_id DROP NOT NULL;
COMMIT TRANSACTION;