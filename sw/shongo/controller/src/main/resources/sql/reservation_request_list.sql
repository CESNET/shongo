/**
 * Select query for list of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    reservation_request_summary.id AS id,
     CASE
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
    reservation_request_summary.provided_reservation_request_id AS provided_reservation_request_id,
    reservation_request_summary.last_reservation_id AS last_reservation_id,
    specification_summary.type AS specification_type,
    specification_summary.technologies AS technologies,
    specification_summary.room_participant_count AS room_participant_count,
    specification_summary.alias_room_name AS alias_room_name,
    specification_summary.resource_id AS resource_id
FROM reservation_request_summary
LEFT JOIN reservation_request ON reservation_request.id = reservation_request_summary.id
LEFT JOIN specification_summary ON specification_summary.id = reservation_request_summary.specification_id
WHERE ${filter}
    /* List only latest versions of a reservation requests (no it's modifications or deleted requests) */
    AND reservation_request_summary.state = 'ACTIVE'
ORDER BY ${order}