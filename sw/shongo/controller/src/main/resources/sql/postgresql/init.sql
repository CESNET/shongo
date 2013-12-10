/** Drop all views to be created */
DROP VIEW IF EXISTS specification_summary;
DROP VIEW IF EXISTS alias_specification_summary;
DROP VIEW IF EXISTS reservation_request_summary;
DROP VIEW IF EXISTS reservation_request_state;
DROP VIEW IF EXISTS reservation_request_set_earliest_child;
DROP VIEW IF EXISTS reservation_request_active_usage;
DROP VIEW IF EXISTS reservation_request_earliest_usage;
DROP VIEW IF EXISTS executable_summary;
DROP VIEW IF EXISTS room_endpoint_earliest_usage;

/**
 * View of room name for specifications for aliases or sets of aliases.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */   
CREATE VIEW alias_specification_summary AS
SELECT 
    DISTINCT ON(specification.id)
    specification.id,
    alias_specification.value as room_name
FROM specification
LEFT JOIN room_specification_alias_specifications AS room_alias_specification ON room_alias_specification.room_specification_id = specification.id
LEFT JOIN alias_set_specification_alias_specifications AS child_alias_specification ON child_alias_specification.alias_set_specification_id = specification.id
LEFT JOIN alias_specification ON alias_specification.id = specification.id OR alias_specification.id = child_alias_specification.alias_specification_id OR alias_specification.id = room_alias_specification.alias_specification_id
LEFT JOIN alias_specification_alias_types AS types ON types.alias_specification_id = alias_specification.id
WHERE alias_specification.id IS NOT NULL AND types.alias_types = 'ROOM_NAME'
ORDER BY specification.id;

/**
 * View of specification summaries.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */   
CREATE VIEW specification_summary AS
SELECT     
    specification.id AS id,
    string_agg(specification_technologies.technologies, ',') AS technologies,
    CASE 
        WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NULL THEN 'PERMANENT_ROOM'
        WHEN room_specification.id IS NOT NULL AND allocation.abstract_reservation_request_id IS NOT NULL THEN 'USED_ROOM'
        WHEN room_specification.id IS NOT NULL THEN 'ROOM'
        WHEN alias_specification_summary.id IS NOT NULL THEN 'ALIAS'
        WHEN resource_specification.id IS NOT NULL THEN 'RESOURCE'
        ELSE 'OTHER'
    END AS type,
    alias_specification_summary.room_name AS alias_room_name,
    room_specification.participant_count AS room_participant_count,
    allocation.abstract_reservation_request_id AS room_reused_reservation_request_id,
    resource_specification.resource_id AS resource_id
FROM specification
LEFT JOIN specification_technologies ON specification_technologies.specification_id = specification.id
LEFT JOIN room_specification ON room_specification.id = specification.id
LEFT JOIN allocation ON allocation.id = room_specification.allocation_id
LEFT JOIN resource_specification ON resource_specification.id = specification.id
LEFT JOIN alias_specification_summary ON alias_specification_summary.id = specification.id
GROUP BY 
    specification.id,    
    alias_specification_summary.id,    
    alias_specification_summary.room_name,
    room_specification.id,
    room_specification.participant_count,
    allocation.abstract_reservation_request_id,
    resource_specification.id;

/**
 * View of id and time slot for the earliest child reservation request for each set of reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW reservation_request_set_earliest_child AS
SELECT
    DISTINCT ON(reservation_request_set_slots.id) /* only one child reservation request for each set of reservation requests */
    reservation_request_set_slots.id AS id,
    reservation_request.id AS child_id,
    reservation_request.slot_start AS slot_start,
    reservation_request.slot_end AS slot_end,
    CASE
        WHEN reservation_request_set_slots.future_child_count > 0 THEN reservation_request_set_slots.future_child_count - 1
        ELSE 0
    END AS future_child_count
FROM (
    SELECT /* sets of reservation requests with "future minimum" and "whole maximum" slot ending */
        abstract_reservation_request.id AS id,
        abstract_reservation_request.allocation_id AS allocation_id,
        MIN(CASE WHEN reservation_request.slot_end > (now() at time zone 'UTC') THEN reservation_request.slot_end ELSE NULL END) AS slot_end_future_min,
        MAX(reservation_request.slot_end) AS slot_end_max,
        COUNT(CASE WHEN reservation_request.slot_end > (now() at time zone 'UTC') THEN 1 ELSE NULL END) AS future_child_count
    FROM reservation_request_set
    LEFT JOIN abstract_reservation_request ON abstract_reservation_request.id = reservation_request_set.id
    LEFT JOIN reservation_request ON reservation_request.parent_allocation_id = abstract_reservation_request.allocation_id
    GROUP BY abstract_reservation_request.id
) AS reservation_request_set_slots
/* join child reservation requests which matches the "future minimum" or the "whole maximum" slot ending */
LEFT JOIN reservation_request ON reservation_request.parent_allocation_id = reservation_request_set_slots.allocation_id
      AND (reservation_request.slot_end = reservation_request_set_slots.slot_end_future_min
           OR reservation_request.slot_end = reservation_request_set_slots.slot_end_max)
/* we want one child reservation request which has the earliest slot ending */
ORDER BY reservation_request_set_slots.id, reservation_request.slot_end;

/**
 * View of computed state for each single reservation request.
 *
 *   NOT_ALLOCATED:
 *     reservation request has not been allocated by the scheduler yet
 *   ALLOCATED:
 *     reservation request is allocated by the scheduler but the allocated executable has not been started yet
 *   STARTED:
 *     reservation request is allocated by the scheduler and the allocated executable is started
 *   FINISHED:
 *     reservation request is allocated by the scheduler and the allocated executable has been started and stopped
 *   FAILED:
 *     reservation request cannot be allocated by the scheduler or the starting of executable failed
 *   MODIFICATION_FAILED:
 *     modification of reservation request cannot be allocated by the scheduler
 *     but some previous version of reservation request has been allocated and started
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW reservation_request_state AS
SELECT
    DISTINCT ON(reservation_request.id)
    reservation_request.id AS id,
    reservation_request.allocation_state AS allocation_state,
    executable.state AS executable_state,
    reservation.id AS last_reservation_id
FROM reservation_request
LEFT JOIN abstract_reservation_request ON abstract_reservation_request.id = reservation_request.id
LEFT JOIN reservation ON reservation.allocation_id = abstract_reservation_request.allocation_id AND abstract_reservation_request.state = 'ACTIVE'
LEFT JOIN executable ON executable.id = reservation.executable_id
ORDER BY reservation_request.id, reservation.slot_end DESC;

/**
 * View of id and allocation/executable state for the active usage reservation request for each reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW reservation_request_active_usage AS
SELECT
    DISTINCT ON(abstract_reservation_request.id)
    abstract_reservation_request.id AS id,
    usage_reservation_request.id AS usage_id,
    usage_reservation_request.slot_start AS slot_start,
    usage_reservation_request.slot_end AS slot_end,
    usage_reservation_request.allocation_state AS allocation_state,
    usage_executable.state AS executable_state
FROM abstract_reservation_request
  LEFT JOIN abstract_reservation_request AS usage ON usage.state = 'ACTIVE' AND (usage.reused_allocation_id = abstract_reservation_request.allocation_id OR usage.specification_id IN(
    SELECT room_specification.id FROM room_specification WHERE room_specification.allocation_id = abstract_reservation_request.allocation_id
  ))
  INNER JOIN reservation_request AS usage_reservation_request ON usage_reservation_request.id = usage.id
  LEFT JOIN reservation ON reservation.allocation_id = usage.allocation_id
  LEFT JOIN executable AS usage_executable ON usage_executable.id = reservation.executable_id
ORDER BY abstract_reservation_request.id, usage_reservation_request.slot_end DESC, reservation.slot_end DESC;

/**
 * View of id and time slot for the earliest usage for each reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW reservation_request_earliest_usage AS
SELECT
    DISTINCT ON(allocation.abstract_reservation_request_id) /* only one usage for each reservation request */
    allocation.abstract_reservation_request_id AS id,
    reservation_request.id AS usage_id,
    reservation_request.slot_start AS slot_start,
    reservation_request.slot_end AS slot_end
FROM (
  SELECT /* reused allocations with "future minimum" slot ending for usages */
    COALESCE(room_specification.allocation_id, abstract_reservation_request.reused_allocation_id) AS allocation_id,
    MIN(CASE WHEN reservation_request.slot_end > (now() at time zone 'UTC') THEN reservation_request.slot_end ELSE NULL END) AS slot_end_future_min
  FROM reservation_request
  LEFT JOIN abstract_reservation_request ON abstract_reservation_request.id = reservation_request.id
  LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
  WHERE reservation_request.allocation_state = 'ALLOCATED' AND (abstract_reservation_request.reused_allocation_id IS NOT NULL OR room_specification.allocation_id IS NOT NULL)
  GROUP BY COALESCE(room_specification.allocation_id, abstract_reservation_request.reused_allocation_id)
) AS reservation_request_usage_slots
/* join usages which matches the "future minimum" or the "whole maximum" slot ending */
INNER JOIN allocation ON allocation.id = reservation_request_usage_slots.allocation_id
INNER JOIN abstract_reservation_request ON abstract_reservation_request.reused_allocation_id = reservation_request_usage_slots.allocation_id OR abstract_reservation_request.specification_id IN(
  SELECT room_specification.id FROM room_specification WHERE room_specification.allocation_id = reservation_request_usage_slots.allocation_id
)
INNER JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
       AND reservation_request.slot_end = reservation_request_usage_slots.slot_end_future_min
/* we want one usage which has the earliest slot ending */
ORDER BY allocation.abstract_reservation_request_id, reservation_request.slot_end;

/**
 * View of time slot and state for each reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW reservation_request_summary AS
SELECT
  reservation_request_summary.*,
  CASE
      /* Highest priority have requests which starts after "-7 days" and ends in interval [now(), +7 days] (e.g., planned, active, or finished rooms) */
      WHEN reservation_request_summary.slot_nearness_start > ((NOW() AT TIME ZONE 'UTC') - (INTERVAL '7 DAY')) AND reservation_request_summary.slot_nearness_end > (NOW() AT TIME ZONE 'UTC') AND reservation_request_summary.slot_nearness_end < ((NOW() AT TIME ZONE 'UTC') + (INTERVAL '7 DAY')) THEN 0
      /* Then the requests whose time slots are currently taking place (e.g. permanent rooms) */
      WHEN (NOW() AT TIME ZONE 'UTC') BETWEEN reservation_request_summary.slot_nearness_start AND reservation_request_summary.slot_nearness_end THEN 1
      /* Then the rest */
      ELSE 2
  END AS slot_nearness_priority,
  CASE
      /* For requests whose timeslot are currently taking place the difference is computed as "slot_end - now()" */
      WHEN reservation_request_summary.executable_state = 'STARTED' OR ((NOW() AT TIME ZONE 'UTC') BETWEEN reservation_request_summary.slot_nearness_start AND reservation_request_summary.slot_nearness_end) THEN
          EXTRACT(EPOCH FROM (reservation_request_summary.slot_nearness_end - (NOW() AT TIME ZONE 'UTC')))
      /* For requests whose timeslot is in future the difference is computed as "slot_start - now()" */
      WHEN (NOW() AT TIME ZONE 'UTC') < reservation_request_summary.slot_nearness_start THEN
          EXTRACT(EPOCH FROM (reservation_request_summary.slot_nearness_start - (NOW() AT TIME ZONE 'UTC')))
      /* For requests whose timeslot is in history the difference is computed as "7 days + (now() - slot_end)" */
      ELSE
          7 * 24 * 3600 + ABS(EXTRACT(EPOCH FROM ((NOW() AT TIME ZONE 'UTC') - reservation_request_summary.slot_nearness_end)))
  END AS slot_nearness_value
FROM (
    SELECT
        reservation_request_summary.*,
        COALESCE(reservation_request_summary.usage_slot_start, reservation_request_summary.slot_start) AS slot_nearness_start,
        COALESCE(reservation_request_summary.usage_slot_end, reservation_request_summary.slot_end) AS slot_nearness_end
    FROM (
        SELECT
            abstract_reservation_request.id AS id,
            abstract_reservation_request.created_at AS created_at,
            abstract_reservation_request.created_by AS created_by,
            abstract_reservation_request.updated_at AS updated_at,
            abstract_reservation_request.updated_by AS updated_by,
            abstract_reservation_request.description AS description,
            abstract_reservation_request.purpose AS purpose,
            abstract_reservation_request.state AS state,
            abstract_reservation_request.specification_id AS specification_id,
            reused_allocation.abstract_reservation_request_id AS reused_reservation_request_id,
            abstract_reservation_request.modified_reservation_request_id AS modified_reservation_request_id,
            abstract_reservation_request.allocation_id AS allocation_id,
            reservation_request_set_earliest_child.child_id AS child_id,
            reservation_request_set_earliest_child.future_child_count AS future_child_count,
            COALESCE(reservation_request.slot_start, reservation_request_set_earliest_child.slot_start) AS slot_start,
            COALESCE(reservation_request.slot_end, reservation_request_set_earliest_child.slot_end) AS slot_end,
            reservation_request_state.allocation_state AS allocation_state,
            reservation_request_state.executable_state AS executable_state,
            reservation_request_state.last_reservation_id AS last_reservation_id,
            reservation_request_active_usage.executable_state AS usage_executable_state,
            reservation_request_earliest_usage.slot_start AS usage_slot_start,
            reservation_request_earliest_usage.slot_end AS usage_slot_end
        FROM abstract_reservation_request
        LEFT JOIN allocation AS reused_allocation ON reused_allocation.id = abstract_reservation_request.reused_allocation_id
        LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
        LEFT JOIN reservation_request_set_earliest_child ON reservation_request_set_earliest_child.id = abstract_reservation_request.id
        LEFT JOIN reservation_request_earliest_usage ON reservation_request_earliest_usage.id = reservation_request.id
        LEFT JOIN reservation_request_state ON reservation_request_state.id = reservation_request.id OR reservation_request_state.id = reservation_request_set_earliest_child.child_id
        LEFT JOIN reservation_request_active_usage ON reservation_request_active_usage.id = reservation_request.id OR reservation_request_active_usage.id = reservation_request_set_earliest_child.child_id
    ) AS reservation_request_summary
) AS reservation_request_summary;

/**
 * View of id and time slot for the earliest usage for each room endpoint.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW room_endpoint_earliest_usage AS
SELECT
    DISTINCT ON(room_endpoint_usage_slots.id) /* only one usage for each set of room endpoint */
    room_endpoint_usage_slots.id AS id,
    executable.id AS usage_id,
    execution_target.slot_start AS slot_start,
    execution_target.slot_end AS slot_end,
    executable.state AS state,
    room_configuration.license_count
FROM (
    SELECT /* room endpoints with "future minimum" slot ending for usages */
        room_endpoint.id AS id,
        MIN(CASE WHEN execution_target.slot_end > (now() at time zone 'UTC') THEN execution_target.slot_end ELSE NULL END) AS slot_end_future_min
    FROM room_endpoint
    LEFT JOIN used_room_endpoint AS room_endpoint_usage ON room_endpoint_usage.room_endpoint_id = room_endpoint.id
    LEFT JOIN executable ON executable.id = room_endpoint_usage.id
    LEFT JOIN execution_target ON execution_target.id = executable.id
    GROUP BY room_endpoint.id
) AS room_endpoint_usage_slots
/* join room endpoint usage which matches the "future minimum" slot ending */
LEFT JOIN used_room_endpoint ON used_room_endpoint.room_endpoint_id = room_endpoint_usage_slots.id
LEFT JOIN execution_target ON execution_target.id = used_room_endpoint.id
                              AND execution_target.slot_end = room_endpoint_usage_slots.slot_end_future_min
LEFT JOIN executable ON executable.id = execution_target.id
LEFT JOIN room_endpoint ON room_endpoint.id = executable.id
LEFT JOIN room_configuration ON room_configuration.id = room_endpoint.room_configuration_id
/* we want one room endpoint usage which has the earliest slot ending */
ORDER BY room_endpoint_usage_slots.id, execution_target.slot_end;

/**
 * View of summaries of executables.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW executable_summary AS
SELECT
    DISTINCT ON(executable.id)
    executable.id AS id,
    CASE
        WHEN used_room_endpoint.id IS NOT NULL THEN 'USED_ROOM'
        WHEN room_endpoint.id IS NOT NULL THEN 'ROOM'
        ELSE 'OTHER'
    END AS type,
    execution_target.slot_start AS slot_start,
    execution_target.slot_end AS slot_end,
    executable.state AS state,
    alias.value AS room_name,
    string_agg(DISTINCT room_configuration_technologies.technologies, ',') AS room_technologies,
    room_configuration.license_count AS room_license_count,
    room_endpoint.room_description AS room_description,
    used_room_endpoint.room_endpoint_id AS room_id,
    room_endpoint_earliest_usage.slot_start AS room_usage_slot_start,
    room_endpoint_earliest_usage.slot_end AS room_usage_slot_end,
    room_endpoint_earliest_usage.state AS room_usage_state,
    room_endpoint_earliest_usage.license_count AS room_usage_license_count
FROM executable
LEFT JOIN execution_target ON execution_target.id = executable.id
LEFT JOIN room_endpoint ON room_endpoint.id = executable.id
LEFT JOIN used_room_endpoint ON used_room_endpoint.id = executable.id
LEFT JOIN room_configuration ON room_configuration.id = room_endpoint.room_configuration_id
LEFT JOIN room_configuration_technologies ON room_configuration_technologies.room_configuration_id = room_configuration.id
LEFT JOIN endpoint_assigned_aliases ON endpoint_assigned_aliases.endpoint_id = executable.id OR endpoint_assigned_aliases.endpoint_id = used_room_endpoint.room_endpoint_id
LEFT JOIN alias ON alias.id = endpoint_assigned_aliases.alias_id AND alias.type = 'ROOM_NAME'
LEFT JOIN room_endpoint_earliest_usage ON room_endpoint_earliest_usage.id = executable.id
GROUP BY
    executable.id,
    execution_target.id,
    room_endpoint.id,
    used_room_endpoint.id,
    room_configuration.id,
    alias.id,
    room_endpoint_earliest_usage.slot_start,
    room_endpoint_earliest_usage.slot_end,
    room_endpoint_earliest_usage.state,
    room_endpoint_earliest_usage.license_count
ORDER BY executable.id, alias.id
