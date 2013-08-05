/** Drop all views to be created */
DROP VIEW specification_summary IF EXISTS;
DROP VIEW reservation_request_summary IF EXISTS;
DROP VIEW reservation_request_state IF EXISTS;

/**
 * @see specification_summary in postgresql/init.sql
 */
CREATE VIEW specification_summary AS
SELECT     
    specification.id AS id,
    GROUP_CONCAT(specification_technologies.technologies SEPARATOR ',') AS technologies,
    CASE 
    WHEN room_specification.id IS NOT NULL THEN 'ROOM'
    WHEN alias_specification.id IS NOT NULL OR alias_set_specification.id IS NOT NULL THEN 'ALIAS'
    WHEN resource_specification.id IS NOT NULL THEN 'RESOURCE'
    ELSE 'OTHER'
    END AS type,
    NULL AS alias_room_name,
    room_specification.participant_count AS room_participant_count,
    resource_specification.resource_id AS resource_id
FROM specification
LEFT JOIN specification_technologies ON specification_technologies.specification_id = specification.id
LEFT JOIN room_specification ON room_specification.id = specification.id
LEFT JOIN resource_specification ON resource_specification.id = specification.id
LEFT JOIN alias_specification ON alias_specification.id = specification.id
LEFT JOIN alias_set_specification ON alias_specification.id = specification.id
GROUP BY 
    specification.id,    
    alias_specification.id,
    alias_set_specification.id,
    room_specification.id,
    room_specification.participant_count,
    resource_specification.id;

/**
 * @see reservation_request_state in postgresql/init.sql
 */
CREATE VIEW reservation_request_state AS
SELECT
    reservation_request.id AS id,
    reservation_request.allocation_state AS allocation_state,
    CASE
        WHEN reservation_request.allocation_state = 'ALLOCATED' THEN 'ALLOCATED'
        WHEN reservation_request.allocation_state = 'ALLOCATION_FAILED' THEN 'FAILED'
        ELSE 'NOT_ALLOCATED'
    END AS state
FROM reservation_request;

/**
 * @see reservation_request_summary in postgresql/init.sql
 */
CREATE VIEW reservation_request_summary AS
SELECT
    abstract_reservation_request.id AS id,
    abstract_reservation_request.created_at AS created_at,
    abstract_reservation_request.created_by AS created_by,
    abstract_reservation_request.updated_at AS updated_at,
    abstract_reservation_request.updated_by AS updated_by,
    abstract_reservation_request.description AS description,
    abstract_reservation_request.purpose AS purpose,
    abstract_reservation_request.state AS state,
    abstract_reservation_request.specification_id AS specification_id,
    abstract_reservation_request.provided_reservation_request_id AS provided_reservation_request_id,
    abstract_reservation_request.modified_reservation_request_id AS modified_reservation_request_id,
    abstract_reservation_request.allocation_id AS allocation_id,
    NULL AS child_id,
    reservation_request.slot_start AS slot_start,
    reservation_request.slot_end AS slot_end,
    reservation_request_state.state AS allocation_state
FROM abstract_reservation_request
LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
LEFT JOIN reservation_request_set ON reservation_request_set.id = abstract_reservation_request.id
LEFT JOIN reservation_request_state ON reservation_request_state.id = reservation_request.id;
