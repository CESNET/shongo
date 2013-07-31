/**
 * View of time slot and state for the earliest child reservation request for each set of reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE OR REPLACE VIEW reservation_request_set_earliest_child AS
SELECT 
    DISTINCT ON(id) /* only one child reservation request for each set of reservation requests */
    reservation_request_set_slots.id AS id,
    reservation_request.id AS child_id,
    reservation_request.slot_start AS slot_start,
    reservation_request.slot_end AS slot_end,
    reservation_request.allocation_state AS allocation_state
FROM (    
    SELECT /* sets of reservation requests with "future minimum" and "whole maximum" slot ending */
        abstract_reservation_request.id AS id,      
        abstract_reservation_request.allocation_id AS allocation_id,              
        MIN(CASE WHEN reservation_request.slot_end > now() THEN reservation_request.slot_end ELSE NULL END) AS slot_end_future_min,
        MAX(reservation_request.slot_end) AS slot_end_max
    FROM reservation_request_set
    LEFT JOIN abstract_reservation_request ON abstract_reservation_request.id = reservation_request_set.id    
    LEFT JOIN reservation_request ON reservation_request.parent_allocation_id = abstract_reservation_request.allocation_id
    GROUP BY abstract_reservation_request.id
) AS reservation_request_set_slots
/* join child reservation requests which matches the "future minimum" and the "whole maximum" slot ending */
LEFT JOIN reservation_request ON reservation_request.parent_allocation_id = reservation_request_set_slots.allocation_id 
      AND (reservation_request.slot_end = reservation_request_set_slots.slot_end_future_min
           OR reservation_request.slot_end = reservation_request_set_slots.slot_end_max)
/* we want one child reservation request which has the earliest slot ending */
ORDER BY id, reservation_request.slot_end;




