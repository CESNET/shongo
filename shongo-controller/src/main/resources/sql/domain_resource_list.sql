/**
 * Select query for list of domains resources.
 *
 * resource_summary.technologies AS technologies,
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
SELECT
    resource_summary.id AS id,
    resource_summary.name AS name,
    resource_summary.description AS description,
    resource_summary.calendar_public AS calendar_public,
    resource_summary.calendar_uri_key AS calendar_uri_key,
    domain_resource.license_count AS license_count,
    domain_resource.price AS price,
    resource_summary.technologies,
    resource_summary.type AS type
FROM resource_summary INNER JOIN domain_resource ON resource_summary.id = domain_resource.resource_id
WHERE ${filter}
ORDER BY ${order}