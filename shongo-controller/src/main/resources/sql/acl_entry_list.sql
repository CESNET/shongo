/**
 * Select query for list of acl entries.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    acl_entry.*,
    COUNT(acl_entry_dependency.id) AS dependency_count
FROM (
    SELECT
        acl_entry.id AS id,
        acl_identity.id AS identity_id,
        acl_identity.type AS identity_type,
        acl_identity.principal_id AS identity_principal_id,
        acl_object_identity.id AS object_identity_id,
        acl_object_class.id AS object_class_id,
        acl_object_class.class AS object_class,
        CASE
            WHEN acl_object_class.class = 'RESERVATION_REQUEST' THEN allocation.abstract_reservation_request_id
            ELSE acl_object_identity.object_id
        END AS object_id,
        acl_entry.role AS role
    FROM acl_entry
    LEFT JOIN acl_identity ON acl_identity.id = acl_entry.acl_identity_id
    LEFT JOIN acl_object_identity ON acl_object_identity.id = acl_entry.acl_object_identity_id
    LEFT JOIN acl_object_class ON acl_object_class.id = acl_object_identity.acl_object_class_id
    LEFT JOIN allocation ON allocation.id = acl_object_identity.object_id AND acl_object_class.class = 'RESERVATION_REQUEST'
    WHERE (acl_object_class.class != 'RESERVATION_REQUEST' OR allocation.abstract_reservation_request_id IS NOT NULL)
) AS acl_entry
LEFT JOIN acl_entry_dependency ON acl_entry_dependency.child_acl_entry_id = acl_entry.id
WHERE ${filter}
GROUP BY acl_entry.id, acl_entry.identity_id, acl_entry.identity_type, acl_entry.identity_principal_id,
         acl_entry.object_identity_id, acl_entry.object_class_id, acl_entry.object_class, acl_entry.object_id, acl_entry.role
ORDER BY acl_entry.identity_type, acl_entry.id