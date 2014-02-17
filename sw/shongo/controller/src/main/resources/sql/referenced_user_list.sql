/**
 * Select query for list of users which are referenced by any entity.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    DISTINCT user_id, string_agg(description, ', ') AS description
FROM (
    SELECT acl_identity.principal_id, COUNT(acl_entry.id) || ' acl-records' AS description
    FROM acl_entry
    LEFT JOIN acl_identity ON acl_identity.id = acl_entry.acl_identity_id
    WHERE acl_identity.type = 'USER'
    GROUP BY acl_identity.principal_id
    UNION ALL
    SELECT person.user_id, COUNT(person.id) || ' persons' AS description
    FROM person WHERE person.user_id IS NOT NULL GROUP BY person.user_id
    UNION ALL
    SELECT resource.user_id, COUNT(resource.user_id) || ' resources' AS description
    FROM resource GROUP BY resource.user_id
    UNION ALL
    SELECT user_id, COUNT(user_settings.id) || ' settings' AS description
    FROM user_settings GROUP BY user_settings.user_id
    UNION ALL
    SELECT requests.user_id, COUNT(requests.id) || ' requests' AS description
    FROM (
        SELECT DISTINCT user_id, id
        FROM (
            SELECT created_by AS user_id, abstract_reservation_request.id AS id
            FROM abstract_reservation_request
            UNION ALL
            SELECT updated_by AS user_id, abstract_reservation_request.id AS id
            FROM abstract_reservation_request
        ) AS requests
    ) AS requests GROUP BY requests.user_id
) AS data
GROUP BY user_id
ORDER BY user_id