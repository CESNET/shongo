/**
 * 1) Add home_time_zone column to user_settings table
 * 2) Alter user_settings with czech locale to use web service for loading locale (default web service locale is czech)
 * 3) Set home_time_zone to value from time_zone
 * 4) Remove time_zone column from user_settings table.
 */
BEGIN TRANSACTION;

ALTER TABLE user_settings ADD COLUMN home_time_zone VARCHAR(255);
UPDATE user_settings SET locale = null, use_web_service = true WHERE locale = 'cs';
UPDATE user_settings SET home_time_zone = time_zone;
ALTER TABLE user_settings DROP COLUMN time_zone;

COMMIT TRANSACTION;