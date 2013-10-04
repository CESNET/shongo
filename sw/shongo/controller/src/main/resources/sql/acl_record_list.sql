/**
 * Select query for list of acl records.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    acl_record.*,
    COUNT(acl_record_dependency.id) AS dependency_count
FROM (
    SELECT
        acl_record.id AS id,
        acl_record.user_id AS user_id,
        CASE
            WHEN acl_record.entity_type = 'ALLOCATION' THEN 'RESERVATION_REQUEST'
            ELSE acl_record.entity_type
        END AS entity_type,
        CASE
            WHEN acl_record.entity_type = 'ALLOCATION' THEN abstract_reservation_request.id
            ELSE acl_record.entity_id
        END AS entity_id,
        acl_record.role AS role
    FROM acl_record
    LEFT JOIN abstract_reservation_request ON abstract_reservation_request.allocation_id = acl_record.entity_id AND acl_record.entity_type = 'ALLOCATION'
    WHERE (acl_record.entity_type != 'ALLOCATION' OR abstract_reservation_request.id IS NOT NULL)
) AS acl_record
LEFT JOIN acl_record_dependency ON acl_record_dependency.child_acl_record_id = acl_record.id
WHERE ${filter}
GROUP BY acl_record.id, acl_record.user_id, acl_record.entity_type,
         acl_record.entity_id, acl_record.role