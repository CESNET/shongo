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
        reservation_request_summary.parent_reservation_request_id AS parent_reservation_request_id,
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
        reservation_request_summary.reused_reservation_request_id AS reused_reservation_request_id,
        reservation_request_summary.last_reservation_id AS last_reservation_id,
        specification_summary.type AS specification_type,
        specification_summary.technologies AS specification_technologies,
        specification_summary.room_participant_count AS room_participant_count,
        FALSE AS room_has_recording_service,
        FALSE AS room_has_recordings,
        specification_summary.alias_room_name AS alias_room_name,
        specification_summary.resource_id AS resource_id,
        reservation_request_summary.usage_executable_state AS usage_executable_state,
        reservation_request_summary.future_child_count,
        reservation_request_summary.state
    FROM reservation_request_summary    
    LEFT JOIN specification_summary ON specification_summary.id = reservation_request_summary.specification_id
    WHERE reservation_request_summary.allocation_id IN(
        :allocationId
    )
)
ORDER BY created_at DESC    
