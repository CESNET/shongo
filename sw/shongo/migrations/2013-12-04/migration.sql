BEGIN TRANSACTION;

ALTER TABLE alias_provider_capability DROP COLUMN permanent_room;
ALTER TABLE alias_specification DROP COLUMN permanent_room;
ALTER TABLE alias_set_specification DROP COLUMN shared_executable;
DROP TABLE alias_specification_permanent_room_participants;
ALTER TABLE room_specification ALTER COLUMN participant_count DROP NOT NULL;
ALTER TABLE used_room_endpoint RENAME COLUMN room_endpoint_id TO reused_room_endpoint_id

COMMIT TRANSACTION;