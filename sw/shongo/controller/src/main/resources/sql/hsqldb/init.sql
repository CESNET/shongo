/** Drop all views to be created */
DROP VIEW specification_summary IF EXISTS;
DROP VIEW reservation_request_summary IF EXISTS;
DROP VIEW reservation_request_state IF EXISTS;
DROP VIEW executable_summary IF EXISTS;

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
LEFT JOIN alias_set_specification ON alias_set_specification.id = specification.id
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
    NULL AS executable_state
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
    provided_allocation.abstract_reservation_request_id AS provided_reservation_request_id,
    abstract_reservation_request.modified_reservation_request_id AS modified_reservation_request_id,
    abstract_reservation_request.allocation_id AS allocation_id,
    NULL AS child_id,
    NULL AS future_child_count,
    reservation_request.slot_start AS slot_start,
    reservation_request.slot_end AS slot_end,
    reservation_request_state.allocation_state AS allocation_state,
    reservation_request_state.executable_state AS executable_state,
    NULL AS last_reservation_id,
    NULL AS usage_executable_state
FROM abstract_reservation_request
LEFT JOIN allocation AS provided_allocation ON provided_allocation.id = abstract_reservation_request.provided_allocation_id
LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
LEFT JOIN reservation_request_set ON reservation_request_set.id = abstract_reservation_request.id
LEFT JOIN reservation_request_state ON reservation_request_state.id = reservation_request.id;

/**
 * @see executable_summary in postgresql/init.sql
 */
CREATE VIEW executable_summary AS
SELECT
    executable.id AS id,
    CASE
        WHEN used_room_endpoint.id IS NOT NULL THEN 'USED_ROOM'
        WHEN room_endpoint.id IS NOT NULL THEN 'ROOM'
        ELSE 'OTHER'
    END AS type,
    executable.slot_start AS slot_start,
    executable.slot_end AS slot_end,
    executable.state AS state,
    NULL AS room_name,
    NULL AS room_technologies,
    room_configuration.license_count AS room_license_count,
    used_room_endpoint.room_endpoint_id AS room_id,
    0 AS room_usage_count,
    NULL AS room_usage_slot_start,
    NULL AS room_usage_slot_end,
    NULL AS room_usage_state,
    NULL AS room_usage_license_count
FROM executable
LEFT JOIN room_endpoint ON room_endpoint.id = executable.id
LEFT JOIN used_room_endpoint ON used_room_endpoint.id = executable.id
LEFT JOIN room_configuration ON room_configuration.id = room_endpoint.room_configuration_id