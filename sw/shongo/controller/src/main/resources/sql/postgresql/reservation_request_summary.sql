/**
 * View of time slot and state for each reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */   
DROP VIEW IF EXISTS reservation_request_summary;
CREATE VIEW reservation_request_summary AS
SELECT
    abstract_reservation_request.id AS id,
    abstract_reservation_request.created_at AS created_at,
    abstract_reservation_request.created_by AS created_by,
    abstract_reservation_request.description AS description,
    abstract_reservation_request.purpose AS purpose,
    abstract_reservation_request.state AS state,
    abstract_reservation_request.specification_id AS specification_id,
    abstract_reservation_request.provided_reservation_request_id AS provided_reservation_request_id,
    abstract_reservation_request.modified_reservation_request_id AS modified_reservation_request_id,
    reservation_request_set_earliest_child.child_id AS child_id,
    COALESCE(reservation_request.slot_start, reservation_request_set_earliest_child.slot_start) AS slot_start,
    COALESCE(reservation_request.slot_end, reservation_request_set_earliest_child.slot_end) AS slot_end,
    COALESCE(reservation_request.allocation_state, reservation_request_set_earliest_child.allocation_state) AS allocation_state
FROM abstract_reservation_request
LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
LEFT JOIN reservation_request_set_earliest_child ON reservation_request_set_earliest_child.id = abstract_reservation_request.id;
