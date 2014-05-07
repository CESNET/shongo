/**
 * 2013-12-18:
 */
BEGIN TRANSACTION;

/* Create recording capability */
CREATE TABLE recording_capability (license_count INT4, id INT8 NOT NULL, PRIMARY KEY (id));
ALTER TABLE recording_capability ADD CONSTRAINT FK1FEC8C268206B069 FOREIGN KEY (id) REFERENCES device_capability;
ALTER TABLE recording_capability OWNER TO shongo;

/* Create resource_room_endpoint_recording_folder_ids */
CREATE TABLE resource_room_endpoint_recording_folder_ids (resource_room_endpoint_id int8 NOT NULL, recording_folder_id VARCHAR(255) NOT NULL, recording_capability_id int8 NOT NULL, PRIMARY KEY (resource_room_endpoint_id, recording_capability_id));
ALTER TABLE resource_room_endpoint_recording_folder_ids ADD CONSTRAINT FKCF0984C1815AF60 FOREIGN KEY (recording_capability_id) REFERENCES recording_capability;
ALTER TABLE resource_room_endpoint_recording_folder_ids ADD CONSTRAINT FKCF0984CA35D7203 FOREIGN KEY (resource_room_endpoint_id) REFERENCES resource_room_endpoint;
ALTER TABLE resource_room_endpoint_recording_folder_ids OWNER TO shongo;

/* Copy room_endpoint_recording_folder_ids to resource_room_endpoint_recording_folder_ids */
INSERT INTO resource_room_endpoint_recording_folder_ids(resource_room_endpoint_id, recording_folder_id , recording_capability_id)
    SELECT
        COALESCE(used_room_endpoint.room_endpoint_id, room_endpoint.id),
        room_endpoint_recording_folder_ids.recording_folder_id,
        room_endpoint_recording_folder_ids.recording_capability_id
    FROM room_endpoint
    LEFT JOIN used_room_endpoint ON used_room_endpoint.id = room_endpoint.id
    LEFT JOIN room_endpoint_recording_folder_ids ON room_endpoint_recording_folder_ids.room_endpoint_id = room_endpoint.id
    WHERE room_endpoint_recording_folder_ids.recording_folder_id IS NOT NULL;

/* Drop room_endpoint_recording_folder_ids */
DROP TABLE room_endpoint_recording_folder_ids;

COMMIT TRANSACTION;