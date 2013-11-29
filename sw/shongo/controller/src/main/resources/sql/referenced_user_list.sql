/**
 * Select query for list of users which are referenced by any entity.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    DISTINCT user_id, string_agg(description, ', ') AS description
FROM (
    SELECT acl_record.user_id, COUNT(acl_record.id) || ' acl-records' AS description
    FROM acl_record GROUP BY acl_record.user_id
    UNION ALL
    SELECT abstract_person.user_id, COUNT(abstract_person.id) || ' persons' AS description
    FROM abstract_person WHERE abstract_person.user_id IS NOT NULL GROUP BY abstract_person.user_id
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