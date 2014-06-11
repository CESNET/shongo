/**
 * 2014-06-11: Add column user_id to reservation.
 */
BEGIN TRANSACTION;

/* Create column user_id which allows NULL values */
ALTER TABLE reservation ADD COLUMN user_id VARCHAR(32);

/* Set user_id column values for all reservations (based on active version of request) */
WITH RECURSIVE reservation_allocation AS (
    SELECT reservation0.id AS id,
           reservation0.allocation_id AS allocation_id,
           0 as level
      FROM reservation AS reservation0
     WHERE reservation0.reservation_id IS NULL
    UNION ALL
    SELECT reservationN.id AS id,
           COALESCE(parent_reservation.allocation_id, reservationN.allocation_id) AS allocation_id,
           parent_reservation.level + 1 AS level
      FROM reservation AS reservationN
      JOIN reservation_allocation AS parent_reservation ON reservationN.reservation_id = parent_reservation.id
)
UPDATE reservation SET user_id = (
    SELECT abstract_reservation_request.created_by
    FROM reservation_allocation
    LEFT JOIN allocation ON allocation.id = reservation_allocation.allocation_id
    LEFT JOIN abstract_reservation_request ON abstract_reservation_request.id = allocation.abstract_reservation_request_id
    WHERE reservation_allocation.id = reservation.id
);

/* Disable NULL values for column user_id */
ALTER TABLE reservation ALTER COLUMN user_id SET NOT NULL;

COMMIT TRANSACTION;