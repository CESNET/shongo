/**
 * 2015-01-01: LocalStorageHandler modified for Windows (NTFS), so change of recording folder id is needed from name:id to name_id.
 */
BEGIN TRANSACTION;

UPDATE resource_room_endpoint_recording_folder_ids SET recording_folder_id = REGEXP_REPLACE(recording_folder_id, '(.*):(.*)', '\1_\2') WHERE  recording_folder_id LIKE '%:%';

COMMIT TRANSACTION;