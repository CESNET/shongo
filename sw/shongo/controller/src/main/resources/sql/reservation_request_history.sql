/**
 * Select query for history of reservation request.
 *
 * @param reservationRequestId
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
(
    /* Select new and modified versions of reservation request */
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
        specification_summary.technologies AS specification_technologies,
        specification_summary.room_participant_count AS room_participant_count,
        specification_summary.alias_room_name AS alias_room_name,
        specification_summary.resource_id AS resource_id,
        reservation_request_summary.usage_executable_state AS usage_executable_state,
        reservation_request_summary.future_child_count
    FROM reservation_request_summary    
    LEFT JOIN specification_summary ON specification_summary.id = reservation_request_summary.specification_id
    WHERE reservation_request_summary.allocation_id IN(
        SELECT abstract_reservation_request.allocation_id FROM abstract_reservation_request WHERE abstract_reservation_request.id = :reservationRequestId
    )
)
UNION ALL (
    /* Select deleted version of reservation request */
    SELECT
        reservation_request_summary.id AS id,
        'DELETED' AS type,
        reservation_request_summary.updated_at AS created_at,
        reservation_request_summary.updated_by AS created_by,
        reservation_request_summary.description AS description,
        reservation_request_summary.purpose AS purpose,
        reservation_request_summary.slot_start AS slot_start,
        reservation_request_summary.slot_end AS slot_end,
        NULL AS allocation_state,
        NULL AS executable_state,
        reservation_request_summary.provided_reservation_request_id AS provided_reservation_request_id,
        reservation_request_summary.last_reservation_id AS last_reservation_id,
        specification_summary.type AS specification_type,
        specification_summary.technologies AS specification_technologies,
        specification_summary.room_participant_count AS room_participant_count,
        specification_summary.alias_room_name AS alias_room_name,
        specification_summary.resource_id AS resource_id,
        reservation_request_summary.usage_executable_state AS usage_executable_state,
        reservation_request_summary.future_child_count
    FROM allocation
    LEFT JOIN reservation_request_summary ON reservation_request_summary.id = allocation.abstract_reservation_request_id
    LEFT JOIN specification_summary ON specification_summary.id = reservation_request_summary.specification_id
    WHERE reservation_request_summary.state = 'DELETED' AND allocation.id IN(
        SELECT abstract_reservation_request.allocation_id FROM abstract_reservation_request WHERE abstract_reservation_request.id = :reservationRequestId
    )
)
ORDER BY created_at DESC    
