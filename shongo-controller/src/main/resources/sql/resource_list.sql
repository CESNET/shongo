/**
 * Select query for list of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    resource_summary.id AS id,
    resource_summary.id AS parent_resource_id,
    resource_summary.user_id AS user_id,
    resource_summary.name AS name,
    resource_summary.allocatable AS allocatable,
    resource_summary.allocation_order AS allocation_order,
    resource_summary.technologies AS technologies
FROM resource_summary
WHERE ${filter}
ORDER BY ${order}