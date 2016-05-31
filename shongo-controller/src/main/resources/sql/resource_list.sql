﻿/**
 * Select query for list of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    resource_summary.id AS id,
    resource_summary.parent_resource_id AS parent_resource_id,
    resource_summary.user_id AS user_id,
    resource_summary.name AS name,
    resource_summary.allocatable AS allocatable,
    resource_summary.allocation_order AS allocation_order,
    resource_summary.technologies AS technologies,
    resource_summary.description AS description,
    resource_summary.calendar_public AS calendar_public,
    resource_summary.calendar_uri_key AS calendar_uri_key,
    resource_summary.confirm_by_owner AS confirm_by_owner
FROM resource_summary
WHERE ${filter}
ORDER BY ${order}