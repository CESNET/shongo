/**
 * Select query for list of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
SELECT
    reservation_summary.id AS id,
    reservation_summary.type AS type,
    reservation_summary.slot_start AS slot_start,
    reservation_summary.slot_end AS slot_end,
    reservation_summary.resource_id AS resource_id,
    reservation_summary.room_license_count AS room_license_count,
    reservation_summary.room_name AS room_name,
    reservation_summary.alias_types AS alias_types,
    reservation_summary.value AS value
FROM reservation_summary
WHERE ${filter}
ORDER BY ${order}