/**
 * 2023-02-24: add auxData to reservation request
 */
BEGIN TRANSACTION;

ALTER TABLE abstract_reservation_request ADD COLUMN aux_data jsonb DEFAULT '[]'::jsonb NOT NULL;

COMMIT TRANSACTION;
