BEGIN TRANSACTION;

/* Rename abstract_person to person */
ALTER TABLE abstract_person RENAME TO person;
ALTER TABLE resource_administrators RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE device_resource_permanent_persons RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE person_participant RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE endpoint_persons RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE endpoint_participant_persons RENAME COLUMN abstract_person_id TO person_id;

/* Refactorize room_specification.allocation_id back to abstract_reservation_request.reused_allocation_id */
ALTER TABLE room_specification ADD COLUMN reused_room boolean default false;
/* Copy allocation_id from room_specification to reused_allocation_id in abstract_reservation_request */
UPDATE abstract_reservation_request SET reused_allocation_id = (
    SELECT room_specification.allocation_id
    FROM abstract_reservation_request AS source
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE source.id = abstract_reservation_request.id
) WHERE abstract_reservation_request.id IN (
    SELECT abstract_reservation_request.id
    FROM abstract_reservation_request
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE room_specification.allocation_id IS NOT NULL
);
/* Set reused_room to room_specification */
UPDATE room_specification SET reused_room = true WHERE room_specification.id IN (
    SELECT room_specification.id
    FROM abstract_reservation_request
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE abstract_reservation_request.reused_allocation_id IS NOT NULL AND room_specification.id IS NOT NULL
);
ALTER TABLE room_specification DROP COLUMN allocation_id CASCADE;

COMMIT TRANSACTION;