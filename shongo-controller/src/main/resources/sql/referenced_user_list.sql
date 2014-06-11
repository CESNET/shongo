/**
 * Select query for list of users which are referenced by any entity.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    DISTINCT CAST(user_id AS INT),
    SUM(reservation_requests) AS reservation_requests,
    SUM(resources) AS resources,
    SUM(user_settings) AS user_settings,
    SUM(acl_records) AS acl_records,
    SUM(user_persons) AS user_persons
FROM (
    SELECT
        reservation_requests.user_id AS user_id,
        COUNT(reservation_requests.id) AS reservation_requests,
        CAST(NULL AS BIGINT) AS resources,
        CAST(NULL AS BIGINT) AS user_settings,
        CAST(NULL AS BIGINT) AS acl_records,
        CAST(NULL AS BIGINT) AS user_persons
    FROM (
        SELECT DISTINCT user_id, id
        FROM (
            SELECT created_by AS user_id, abstract_reservation_request.id AS id
            FROM abstract_reservation_request
            UNION ALL
            SELECT updated_by AS user_id, abstract_reservation_request.id AS id
            FROM abstract_reservation_request
        ) AS reservation_requests
    ) AS reservation_requests GROUP BY reservation_requests.user_id

    UNION ALL
    SELECT reservation.user_id, NULL, NULL, NULL, NULL, NULL
    FROM reservation GROUP BY reservation.user_id

    UNION ALL
    SELECT resource.user_id, NULL, COUNT(resource.user_id) AS resources, NULL, NULL, NULL
    FROM resource GROUP BY resource.user_id

    UNION ALL
    SELECT user_id, NULL, NULL, COUNT(user_settings.id) AS settings, NULL, NULL
    FROM user_settings GROUP BY user_settings.user_id

    UNION ALL
    SELECT  acl_identity.principal_id AS user_id, NULL, NULL, NULL, COUNT(acl_entry.id), NULL
    FROM acl_entry
    LEFT JOIN acl_identity ON acl_identity.id = acl_entry.acl_identity_id
    WHERE acl_identity.type = 'USER'
    GROUP BY acl_identity.principal_id

    UNION ALL
    SELECT person.user_id, NULL, NULL, NULL, NULL, COUNT(person.id) AS persons
    FROM person WHERE person.user_id IS NOT NULL GROUP BY person.user_id
) AS data
GROUP BY user_id
ORDER BY user_id