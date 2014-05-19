/*****************************************************************************/
/** Select reservation requests in Shongo created by regular users.         **/
/*****************************************************************************/
SELECT
    /* Latest request id (modified versions of the same request share the same request_id) */
    'req:' || abstract_reservation_request.latest_id AS latest_id,

    /* Real request version id (modified versions of the same request have different version_id) */
    'req:' || abstract_reservation_request.id AS id,

    /* Request state (ACTIVE: request can be seen by users, MODIFIED: newer request version exists, DELETED: request was deleted) */
    abstract_reservation_request.state AS state,

    /* Requested timeslot */
    CASE
        WHEN reservation_request.id IS NOT NULL THEN reservation_request.slot_start
        WHEN reservation_request_set.id IS NOT NULL THEN periodic_date_time.start
    END AS slot_start,
    CASE
        WHEN reservation_request.id IS NOT NULL THEN reservation_request.slot_end
        WHEN reservation_request_set.id IS NOT NULL THEN periodic_date_time.start +
            CASE periodic_slot.duration
                WHEN 'P1D' THEN interval '1 day'
                WHEN 'P1W' THEN interval '1 week'
                ELSE interval '0'
            END
    END AS slot_end,

    /* Requested type of timeslot (single|periodic) */
    CASE
        WHEN reservation_request.id IS NOT NULL THEN 'Single'
        WHEN reservation_request_set.id IS NOT NULL THEN 'Periodic (' || periodic_date_time.period || ', ' || periodic_date_time.ending || ')'
    END AS slot_type,

    /* Requested technologies */
    (
        SELECT STRING_AGG(DISTINCT technologies, ',') FROM (
            SELECT technologies FROM specification_technologies
            WHERE specification_id = room_specification.id OR specification_id = room_name_specification.id
            UNION ALL
            SELECT DISTINCT alias_technologies FROM alias_specification_alias_technologies
            WHERE alias_specification_alias_technologies.alias_specification_id = room_name_specification.id
        ) AS data
    ) AS technologies,

    /* Requested specification */
    CASE
       WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NOT NULL AND abstract_reservation_request.reused_allocation_id IS NULL THEN
           'One-Time Room (' || room_specification.participant_count || ' participants)'
       WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NOT NULL AND abstract_reservation_request.reused_allocation_id IS NOT NULL THEN
           'Permanent Room Capacity (' || COALESCE(room_name_specification.value, '') || ', ' || room_specification.participant_count || ' participants)'
       WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NULL AND abstract_reservation_request.reused_allocation_id IS NULL THEN
           'Permanent Room (' || COALESCE(room_name_specification.value, '') || ')'
       ELSE 'Other'
    END AS specification,

   /* Requested specification type */
    CASE
       WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NOT NULL AND abstract_reservation_request.reused_allocation_id IS NULL THEN
           'ONE_TIME'
       WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NOT NULL AND abstract_reservation_request.reused_allocation_id IS NOT NULL THEN
           'PERMANENT_ROOM_CAPACITY'
       WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NULL AND abstract_reservation_request.reused_allocation_id IS NULL THEN
           'PERMANENT_ROOM'
       ELSE 'Other'
    END AS specificationType,

    /* Number of requested participants */
    room_specification.participant_count AS room_participant_count,

    /* Permanent room name */
    room_name_specification.value AS room_name,

    /* User description */
    abstract_reservation_request.description AS description,

    /* Date/time when the request was created */
    abstract_reservation_request.updated_by AS created_by,

    /* Date/time when the request was created */
    abstract_reservation_request.updated_at AS created_at

/* All requests */
FROM (
    /* Prepare (version_id, latest_id) for each request */
    WITH RECURSIVE reservation_request_ids AS (
         SELECT request0.id AS version_id,
                request0.id AS latest_id,
                request0.modified_reservation_request_id AS modified_id,
                0 as level
           FROM abstract_reservation_request AS request0
      UNION ALL
         SELECT requestN.id AS version_id,
                COALESCE(newer_version.latest_id, requestN.id) AS latest_id,
                requestN.modified_reservation_request_id AS modified_id,
                newer_version.level + 1 AS level
          FROM abstract_reservation_request AS requestN
          JOIN reservation_request_ids AS newer_version ON requestN.id = newer_version.modified_id
    )
    SELECT
        /* All columns from abstract_reservation_request */
        abstract_reservation_request.*,
        /* Additional column latest_id */
        (
            SELECT latest_id FROM reservation_request_ids
            WHERE version_id = abstract_reservation_request.id
            ORDER BY level DESC
            LIMIT 1
        ) AS latest_id
    FROM abstract_reservation_request
) AS abstract_reservation_request

/* Join single request (exists only for some requests) */
LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id

/* Join periodic request (exists only for some requests) */
LEFT JOIN reservation_request_set ON reservation_request_set.id = abstract_reservation_request.id
LEFT JOIN date_time_slot AS periodic_slot ON periodic_slot.id = (
    SELECT reservation_request_set_slots.date_time_slot_id FROM reservation_request_set_slots
    WHERE reservation_request_set_slots.reservation_request_set_id = reservation_request_set.id
    LIMIT 1
)
LEFT JOIN periodic_date_time ON periodic_date_time.id = periodic_slot.periodic_date_time_id

/* Join room specification (one-time room and permanent room capacity) */
LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id

/* Join alias set specification (permanent room) */
LEFT JOIN alias_set_specification ON alias_set_specification.id = abstract_reservation_request.specification_id

/* Join room name specification */
LEFT JOIN alias_specification AS room_name_specification ON room_name_specification.id IN (
    /* Room name specification can be located in alias_set_specification or in reused reservation request (in permanent room request) */
    SELECT specification.id FROM (
        /* This request in room_specification */
        SELECT room_specification_alias_specifications.alias_specification_id AS id
        FROM room_specification
        LEFT JOIN room_specification_alias_specifications ON room_specification_alias_specifications.room_specification_id = room_specification.id
        WHERE room_specification.id = abstract_reservation_request.specification_id
        UNION ALL
        /* Reused request in room_specification */
        SELECT room_specification_alias_specifications.alias_specification_id AS id
	FROM allocation AS reused_allocation
        LEFT JOIN abstract_reservation_request AS reused_reservation_request ON reused_reservation_request.id = reused_allocation.abstract_reservation_request_id
        LEFT JOIN room_specification_alias_specifications ON room_specification_alias_specifications.room_specification_id = reused_reservation_request.specification_id
        WHERE reused_allocation.id = abstract_reservation_request.reused_allocation_id
    ) AS specification
    WHERE specification.id IN (SELECT alias_specification_id FROM alias_specification_alias_types WHERE alias_types = 'ROOM_NAME')
)

/* Filter */
WHERE TRUE

/* Only requests created by non-root users (root user has user-id '0') and not expanded from periodic requests */
AND abstract_reservation_request.created_by != '0' AND reservation_request.parent_allocation_id IS NULL

/* Order by request_id and version_id */
ORDER BY abstract_reservation_request.latest_id, abstract_reservation_request.id



