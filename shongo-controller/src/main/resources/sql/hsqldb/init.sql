/** Drop all views to be created */
DROP TABLE executable_summary IF EXISTS;
DROP TABLE specification_summary IF EXISTS;

DROP VIEW resource_summary IF EXISTS;
DROP VIEW specification_summary_view IF EXISTS;
DROP VIEW reservation_request_summary IF EXISTS;
DROP VIEW reservation_request_state IF EXISTS;
DROP VIEW reservation_summary IF EXISTS;
DROP VIEW executable_summary_view IF EXISTS;

/**
 * @see resource_summary in postgresql/init.sql
 */
CREATE VIEW resource_summary AS
SELECT
    resource.id AS id,
    resource.resource_id AS parent_resource_id,
    resource.user_id AS user_id,
    resource.name AS name,
    resource.allocatable AS allocatable,
    resource.allocation_order AS allocation_order,
    resource.description AS description,
    resource.calendar_public AS calendar_public,
    resource.calendar_uri_key AS calendar_uri_key,
    resource.confirm_by_owner AS confirm_by_owner,
    GROUP_CONCAT(device_resource_technologies.technologies SEPARATOR ',') AS technologies,
    CASE
      WHEN (SELECT resource_id FROM capability INNER JOIN room_provider_capability on room_provider_capability.id = capability.id WHERE resource_id = resource.id) IS NOT NULL THEN 'ROOM_PROVIDER'
      WHEN (SELECT resource_id FROM capability INNER JOIN recording_capability on recording_capability.id = capability.id WHERE resource_id = resource.id) IS NOT NULL THEN 'RECORDING_SERVICE'
      ELSE 'RESOURCE'
    END AS type,
    GROUP_CONCAT(CONCAT(tag.id, ',', tag.name, ',', tag.type, ',', tag.data) SEPARATOR '|') AS tags
FROM resource
LEFT JOIN device_resource ON device_resource.id = resource.id
LEFT JOIN device_resource_technologies ON device_resource_technologies.device_resource_id = device_resource.id
LEFT JOIN resource_tag ON resource.id = resource_tag.resource_id
LEFT JOIN tag ON resource_tag.tag_id = tag.id
GROUP BY resource.id;

/**
 * @see specification_summary in postgresql/init.sql
 */
CREATE VIEW specification_summary_view AS
SELECT     
    specification.id AS id,
    GROUP_CONCAT(specification_technologies.technologies SEPARATOR ',') AS technologies,
    CASE 
        WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NULL THEN 'PERMANENT_ROOM'
        WHEN room_specification.id IS NOT NULL AND room_specification.reused_room THEN 'USED_ROOM'
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
    parent_allocation.abstract_reservation_request_id AS parent_reservation_request_id,
    abstract_reservation_request.created_at AS created_at,
    abstract_reservation_request.created_by AS created_by,
    abstract_reservation_request.updated_at AS updated_at,
    abstract_reservation_request.updated_by AS updated_by,
    abstract_reservation_request.description AS description,
    abstract_reservation_request.purpose AS purpose,
    abstract_reservation_request.state AS state,
    abstract_reservation_request.specification_id AS specification_id,
    reused_allocation.abstract_reservation_request_id AS reused_reservation_request_id,
    abstract_reservation_request.modified_reservation_request_id AS modified_reservation_request_id,
    abstract_reservation_request.allocation_id AS allocation_id,
    abstract_reservation_request.aux_data AS aux_data,
    NULL AS child_id,
    NULL AS future_child_count,
    reservation_request.slot_start AS slot_start,
    reservation_request.slot_end AS slot_end,
    NULL AS slot_nearness_priority,
    NULL AS slot_nearness_value,
    reservation_request_state.allocation_state AS allocation_state,
    reservation_request_state.executable_state AS executable_state,
    NULL AS last_reservation_id,
    NULL AS last_executable_id,
    NULL AS usage_executable_state,
    NULL AS allowCache
FROM abstract_reservation_request
LEFT JOIN allocation AS reused_allocation ON reused_allocation.id = abstract_reservation_request.reused_allocation_id
LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
LEFT JOIN allocation AS parent_allocation ON parent_allocation.id = reservation_request.parent_allocation_id
LEFT JOIN reservation_request_set ON reservation_request_set.id = abstract_reservation_request.id
LEFT JOIN reservation_request_state ON reservation_request_state.id = reservation_request.id;

/**
 * @see reservation_summary in postgresql/init.sql
 */
CREATE VIEW reservation_summary AS
SELECT
    reservation.id AS id,
    reservation.user_id AS user_id,
    NULL AS reservation_request_id,
    CASE
        WHEN resource_reservation.id IS NOT NULL THEN 'RESOURCE'
        WHEN room_reservation.id IS NOT NULL THEN 'ROOM'
        WHEN alias_reservation.id IS NOT NULL THEN 'ALIAS'
        WHEN value_reservation.id IS NOT NULL THEN 'VALUE'
        WHEN recording_service_reservation.id IS NOT NULL THEN 'RECORDING_SERVICE'
        ELSE 'OTHER'
    END AS type,
    reservation.slot_start AS slot_start,
    reservation.slot_end AS slot_end,
    ISNULL(resource_reservation.resource_id, room_provider_capability.resource_id) AS resource_id,
    foreign_resource_reservation.foreign_resources_id as foreign_resources_id,
    room_reservation.license_count AS room_license_count,
    NULL AS room_name,
    NULL AS alias_types,
    value_reservation.value AS value,
    NULL AS reservation_request_description,
    NULL AS parent_reservation_request_id
FROM reservation
LEFT JOIN resource_reservation ON resource_reservation.id = reservation.id
LEFT JOIN foreign_resource_reservation ON foreign_resource_reservation.id = reservation.id
LEFT JOIN room_reservation ON room_reservation.id = reservation.id
LEFT JOIN capability AS room_provider_capability ON room_provider_capability.id = room_reservation.room_provider_capability_id
LEFT JOIN alias_reservation ON alias_reservation.id = reservation.id
LEFT JOIN value_reservation ON value_reservation.id = reservation.id OR value_reservation.id = alias_reservation.value_reservation_id
LEFT JOIN recording_service_reservation ON recording_service_reservation.id = reservation.id;

/**
 * @see executable_summary in postgresql/init.sql
 */
CREATE VIEW executable_summary_view AS
SELECT
    executable.id AS id,
    room_provider_capability.resource_id AS resource_id,
    CASE
        WHEN used_room_endpoint.id IS NOT NULL THEN 'USED_ROOM'
        WHEN room_endpoint.id IS NOT NULL THEN 'ROOM'
        ELSE 'OTHER'
    END AS type,
    CASE
        WHEN room_endpoint.id IS NOT NULL THEN execution_target.slot_start - room_endpoint.slot_minutes_before MINUTE
        ELSE execution_target.slot_start
    END AS slot_start,
    CASE
        WHEN room_endpoint.id IS NOT NULL THEN execution_target.slot_end + room_endpoint.slot_minutes_after MINUTE
        ELSE execution_target.slot_end
    END AS slot_end,
    executable.state AS state,
    NULL AS room_name,
    GROUP_CONCAT(DISTINCT room_configuration_technologies.technologies SEPARATOR ',') AS room_technologies,
    room_configuration.license_count AS room_license_count,
    room_endpoint.room_description AS room_description,
    used_room_endpoint.room_endpoint_id AS room_id,
    0 AS room_usage_count,
    NULL AS room_has_recording_service,
    NULL AS room_has_recordings
FROM executable
LEFT JOIN execution_target ON execution_target.id = executable.id
LEFT JOIN room_endpoint ON room_endpoint.id = executable.id
LEFT JOIN used_room_endpoint ON used_room_endpoint.id = executable.id
LEFT JOIN resource_room_endpoint ON resource_room_endpoint.id = room_endpoint.id OR resource_room_endpoint.id = used_room_endpoint.room_endpoint_id
LEFT JOIN capability AS room_provider_capability ON room_provider_capability.id = resource_room_endpoint.room_provider_capability_id
LEFT JOIN room_configuration ON room_configuration.id = room_endpoint.room_configuration_id
LEFT JOIN room_configuration_technologies ON room_configuration_technologies.room_configuration_id = room_configuration.id
GROUP BY
    executable.id,
    execution_target.id,
    room_endpoint.id,
    room_provider_capability.id,
    used_room_endpoint.id,
    room_configuration.id
ORDER BY executable.id;

CREATE TABLE executable_summary AS (SELECT * FROM executable_summary_view) WITH NO DATA;
CREATE TABLE specification_summary AS (SELECT * FROM specification_summary_view) WITH NO DATA;
