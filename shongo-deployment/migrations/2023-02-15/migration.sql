/**
 * 2023-02-15: tag can now hold additional data
 */
BEGIN TRANSACTION;

ALTER TABLE tag
    ADD COLUMN type varchar(255) NOT NULL DEFAULT 'DEFAULT',
    ADD COLUMN data jsonb;

COMMIT TRANSACTION;
