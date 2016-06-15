/**
 * 2015-10-29: create indexes for userIds
 */
BEGIN TRANSACTION;


CREATE INDEX created_by_idx ON abstract_reservation_request (created_by);
CREATE INDEX user_id_idx ON reservation (user_id);

COMMIT TRANSACTION;