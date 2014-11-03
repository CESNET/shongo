/**
 * 2014-10-24: Change Enum AdobeConnectPermissions data type in database (check values first!)
 */
BEGIN TRANSACTION;

ALTER TABLE room_setting RENAME COLUMN access_mode TO access_mode_int;

ALTER TABLE room_setting ADD COLUMN access_mode VARCHAR(64);

UPDATE room_setting SET access_mode = 'VIEW' WHERE access_mode_int = 0;
UPDATE room_setting SET access_mode = 'PUBLIC' WHERE access_mode_int = 1;
UPDATE room_setting SET access_mode = 'PROTECTED' WHERE access_mode_int = 2;
UPDATE room_setting SET access_mode = 'PRIVATE' WHERE access_mode_int = 3;


SELECT id,dtype,access_mode,access_mode_int FROM room_setting;

ALTER TABLE room_setting DROP COLUMN access_mode_int;

COMMIT TRANSACTION;