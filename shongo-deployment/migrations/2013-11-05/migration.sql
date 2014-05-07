/**
 * 2013-11-05: Replace "room_configuration_id" by "license_count" column in "room_reservation" table.
 * 1) add "license_count" column to "room_reservation"
 * 2) set "license_count" to value from corresponding "room_configuration"
 * 3) set "license_count" column as "NOT NULL"
 * 4) delete "room_configuration_id" column
 */
BEGIN TRANSACTION;

ALTER TABLE room_reservation ADD COLUMN license_count int4;
UPDATE room_reservation SET license_count = (SELECT room_configuration.license_count FROM room_configuration WHERE room_configuration.id = room_reservation.room_configuration_id);
ALTER TABLE room_reservation ALTER COLUMN license_count set not null;
ALTER TABLE room_reservation DROP COLUMN room_configuration_id;

COMMIT TRANSACTION;