/**
 * 1) Add home_time_zone column to user_settings table
 * 2) Alter user_settings with czech locale to use web service for loading locale (default web service locale is czech)
 * 3) Set home_time_zone to value from time_zone
 * 4) Remove time_zone column from user_settings table.
 */
BEGIN TRANSACTION;


ALTER TABLE alias_provider_capability DROP COLUMN permanent_room;
ALTER TABLE alias_specification DROP COLUMN permanent_room;
ALTER TABLE alias_set_specification DROP COLUMN shared_executable;
DROP TABLE alias_specification_permanent_room_participants;

COMMIT TRANSACTION;