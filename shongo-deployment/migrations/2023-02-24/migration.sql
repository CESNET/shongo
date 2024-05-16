/**
 * 2023-02-24: add auxData to reservation request
 */
BEGIN TRANSACTION;

ALTER TABLE abstract_reservation_request ADD COLUMN aux_data jsonb DEFAULT '[]'::jsonb NOT NULL;

-- Has to be here, otherwise JPA creates TABLE instead of VIEW
CREATE VIEW arr_aux_data AS
SELECT
    arr.*,
    jsonb_array_elements(aux_data)->>'tagName' AS tag_name,
    (jsonb_array_elements(aux_data)->>'enabled')::boolean AS enabled,
    jsonb_array_elements(aux_data)->'data' AS data
FROM abstract_reservation_request arr;

COMMIT TRANSACTION;
