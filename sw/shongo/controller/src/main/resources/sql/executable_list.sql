/**
 * Select query for list of executables.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    executable_summary.id AS id,
    executable_summary.type AS type,
    executable_summary.slot_start AS slot_start,
    executable_summary.slot_end AS slot_end,
    executable_summary.state AS state,
    executable_summary.room_name AS room_name,
    executable_summary.room_technologies AS room_technologies,
    executable_summary.room_license_count AS room_license_count,
    executable_summary.room_id AS room_id,
    COUNT(used_room_endpoint.id) AS room_usage_count
FROM executable_summary
LEFT JOIN used_room_endpoint ON executable_summary.type = 'ROOM'
                            AND used_room_endpoint.room_endpoint_id = executable_summary.id
                            AND used_room_endpoint.${filterExecutableId}
WHERE executable_summary.${filterExecutableId} AND ${filter}
    /* List only allocated executables */
    AND executable_summary.state NOT IN('NOT_ALLOCATED', 'TO_DELETE')
    /* List only top executables (not children) */
    AND executable_summary.id NOT IN(
        SELECT executable_child_executables.child_executable_id FROM executable_child_executables
    )
GROUP BY
    executable_summary.id,
    executable_summary.type,
    executable_summary.slot_start,
    executable_summary.slot_end,
    executable_summary.state,
    executable_summary.room_name,
    executable_summary.room_technologies,
    executable_summary.room_license_count,
    executable_summary.room_id
ORDER BY ${order}