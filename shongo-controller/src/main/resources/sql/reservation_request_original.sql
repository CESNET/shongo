/**
 * Select query for original reservation request.
 */
(
    SELECT
        abstract_reservation_request.id AS id,
        abstract_reservation_request.created_by AS created_by,
        abstract_reservation_request.created_at AS created_at,
        abstract_reservation_request.state AS state,
        parent_allocation.abstract_reservation_request_id AS parent_reservation_request_id
    FROM abstract_reservation_request
    LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
    LEFT JOIN allocation AS parent_allocation ON parent_allocation.id = reservation_request.parent_allocation_id
    WHERE abstract_reservation_request.allocation_id IN(
        :allocationId
    )
    ORDER BY abstract_reservation_request.created_at ASC
    LIMIT 1
)
