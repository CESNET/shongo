/**
 * Select query for list of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    reservation_request_summary.id AS id,
    reservation_request_summary.parent_reservation_request_id AS parent_reservation_request_id,
    CASE
        WHEN reservation_request_summary.state = 'DELETED' THEN 'DELETED'
        WHEN reservation_request_summary.modified_reservation_request_id IS NOT NULL THEN 'MODIFIED'
        ELSE 'NEW'
    END AS type,
    reservation_request_summary.created_at AS created_at,
    reservation_request_summary.created_by AS created_by,
    reservation_request_summary.description AS description,
    reservation_request_summary.purpose AS purpose,    
    reservation_request_summary.slot_start AS slot_start,
    reservation_request_summary.slot_end AS slot_end,
    reservation_request_summary.allocation_state AS allocation_state,
    reservation_request_summary.executable_state AS executable_state,
    reservation_request_summary.reused_reservation_request_id AS reused_reservation_request_id,
    reservation_request_summary.last_reservation_id AS last_reservation_id,
    specification_summary.type AS specification_type,
    specification_summary.technologies AS specification_technologies,
    specification_summary.room_participant_count AS room_participant_count,
    executable_summary.room_has_recording_service AS room_has_recording_service,
    executable_summary.room_has_recordings AS room_has_recordings,
    CASE
        WHEN specification_summary.alias_room_name IS NOT NULL THEN specification_summary.alias_room_name
        ELSE reused_specification_summary.alias_room_name
    END AS alias_room_name,
    specification_summary.resource_id resource_id,
    reservation_request_summary.usage_executable_state AS usage_executable_state,
    reservation_request_summary.future_child_count,
    resource_summary.name AS resource_name,
    foreign_resources.foreign_resource_id,
    domain.name as domain_name,
    reservation_request_summary.allowCache as allowCache,
    resource_summary.tags as tags,
    reservation_request_summary.aux_data as aux_data
FROM reservation_request_summary
LEFT JOIN reservation_request ON reservation_request.id = reservation_request_summary.id
LEFT JOIN specification_summary ON specification_summary.id = reservation_request_summary.specification_id
LEFT JOIN abstract_reservation_request AS reused_reservation_request ON reused_reservation_request.id = reservation_request_summary.reused_reservation_request_id
LEFT JOIN specification_summary AS reused_specification_summary ON reused_specification_summary.id = reused_reservation_request.specification_id
LEFT JOIN executable_summary ON executable_summary.id = reservation_request_summary.last_executable_id
LEFT JOIN resource_summary ON resource_summary.id = specification_summary.resource_id
LEFT JOIN resource_specification ON resource_specification.id = specification_summary.id
LEFT JOIN foreign_resources ON foreign_resources.id = resource_specification.foreign_resources_id
LEFT JOIN domain ON domain.id = foreign_resources.domain_id
WHERE ${filter}
ORDER BY ${order}
