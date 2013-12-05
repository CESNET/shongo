BEGIN TRANSACTION;

/**
 * Alter tables
 */
ALTER TABLE alias_provider_capability DROP COLUMN permanent_room;
ALTER TABLE alias_set_specification DROP COLUMN shared_executable;
ALTER TABLE alias_specification DROP COLUMN permanent_room;
DROP TABLE alias_specification_permanent_room_participants;
ALTER TABLE room_specification ALTER COLUMN participant_count DROP NOT NULL;
ALTER TABLE room_specification ADD COLUMN allocation_id int8;
ALTER TABLE room_specification ADD CONSTRAINT fk_room_specification_allocation_id foreign key (allocation_id) references allocation;


/**
 * UPDATE permanent rooms from alias_specifications
 *
 * Initialize */
ALTER TABLE room_specification ADD COLUMN alias_specification_id int8;
/* Create room_specifications from standalone alias_specifications */
INSERT INTO room_specification(id, alias_specification_id)
    SELECT alias_specification.id, nextval('hibernate_sequence') FROM abstract_reservation_request
    LEFT JOIN alias_specification ON alias_specification.id = abstract_reservation_request.specification_id
    WHERE alias_specification.id IS NOT NULL;
/* Create duplicates of alias_specifications */
INSERT INTO alias_specification (id, alias_provider_capability_id, value)
    SELECT room_specification.alias_specification_id, alias_provider_capability_id, value FROM alias_specification
    LEFT JOIN room_specification ON room_specification.id = alias_specification.id
    WHERE room_specification.alias_specification_id IS NOT NULL;
/* Create types and technologies to duplicates */
UPDATE alias_specification_alias_types SET alias_specification_id = (
    SELECT room_specification.alias_specification_id FROM room_specification
    WHERE room_specification.id = alias_specification_alias_types.alias_specification_id
)
WHERE alias_specification_alias_types.alias_specification_id IN (
    SELECT room_specification.id FROM room_specification
    WHERE room_specification.alias_specification_id IS NOT NULL
);
UPDATE alias_specification_alias_technologies SET alias_specification_id = (
    SELECT room_specification.alias_specification_id FROM room_specification
    WHERE room_specification.id = alias_specification_alias_technologies.alias_specification_id
)
WHERE alias_specification_alias_technologies.alias_specification_id IN (
    SELECT room_specification.id FROM room_specification
    WHERE room_specification.alias_specification_id IS NOT NULL
);
/* Delete old alias specifications */
DELETE FROM alias_specification WHERE alias_specification.id IN (SELECT room_specification.id FROM room_specification)
/* Move standalone alias_specifications into room_specifications */
INSERT INTO room_specification_alias_specifications(room_specification_id, alias_specification_id)
    SELECT room_specification.id, room_specification.alias_specification_id FROM room_specification
    WHERE room_specification.alias_specification_id IS NOT NULL;
/* De-initialize */
ALTER TABLE room_specification DROP COLUMN alias_specification_id;


/**
 * UPDATE permanent rooms from alias_set_specifications
 *
 * Create room_specifications from alias_set_specifications */
INSERT INTO room_specification(id)
    SELECT alias_set_specification.id FROM alias_set_specification;
/* Move alias_specifications from alias_set_specifications to room_specifications */
INSERT INTO room_specification_alias_specifications(room_specification_id, alias_specification_id)
    SELECT alias_set_specification_id, alias_specification_id  FROM alias_set_specification_alias_specifications;
DELETE FROM alias_set_specification_alias_specifications;
/* Delete alias_set_specifications */
DELETE FROM alias_set_specification;


/**
 * UPDATE permanent room capacities
 *
 * Copy reused_allocation_id from abstract_reservation_request to allocation_id in room_specification */
UPDATE room_specification SET allocation_id = (
    SELECT abstract_reservation_request.reused_allocation_id
    FROM abstract_reservation_request
    WHERE abstract_reservation_request.specification_id = room_specification.id
) WHERE room_specification.id IN (
    SELECT room_specification.id
    FROM abstract_reservation_request
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE abstract_reservation_request.reused_allocation_id IS NOT NULL AND room_specification.id IS NOT NULL
);
/* Clear reused_allocation_id in abstract_reservation_request */
UPDATE abstract_reservation_request SET reused_allocation_id = NULL WHERE abstract_reservation_request.id IN(
    SELECT abstract_reservation_request.id FROM abstract_reservation_request
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE abstract_reservation_request.reused_allocation_id IS NOT NULL AND room_specification.id IS NOT NULL
);

COMMIT TRANSACTION;