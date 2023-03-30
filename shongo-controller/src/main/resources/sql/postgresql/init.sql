/** Drop all views to be created */
DROP TABLE IF EXISTS executable_summary;
DROP TABLE IF EXISTS specification_summary;

DROP VIEW IF EXISTS resource_summary;
DROP VIEW IF EXISTS specification_summary_view;
DROP VIEW IF EXISTS alias_specification_summary;
DROP VIEW IF EXISTS reservation_request_summary;
DROP VIEW IF EXISTS reservation_request_state;
DROP VIEW IF EXISTS reservation_request_set_earliest_child;
DROP VIEW IF EXISTS reservation_request_active_usage;
DROP VIEW IF EXISTS reservation_request_earliest_usage;
DROP VIEW IF EXISTS reservation_summary;
DROP VIEW IF EXISTS executable_summary_view;
DROP VIEW IF EXISTS room_endpoint_earliest_usage;

/**
 * Create missing foreign keys' indexes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
DROP FUNCTION IF EXISTS create_fk_indexes();
CREATE FUNCTION create_fk_indexes () RETURNS INTEGER AS $$
DECLARE result RECORD;
DECLARE count INT = 0;
BEGIN
    RAISE NOTICE 'Creating missing FK indexes...';
    FOR result IN (
      SELECT 'CREATE INDEX ' || regexp_replace(relname || '_' || array_to_string(column_name_list, '_'), '([^_]{5})[^_]*_', '\1_', 'g') || '_idx ' ||
             'ON ' || conrelid || ' (' || array_to_string(column_name_list, ',') || ')' AS command
      FROM (
          SELECT DISTINCT conrelid,
                 array_agg(attname) column_name_list,
                 array_agg(attnum) as column_list
          FROM pg_attribute
          JOIN (
              SELECT conrelid::regclass,
                     conname,
                     unnest(conkey) as column_index
                FROM (
                    SELECT DISTINCT conrelid,
                           conname,
                           conkey
                      FROM pg_constraint
                      JOIN pg_class ON pg_class.oid = pg_constraint.conrelid
                      JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace
                     WHERE nspname !~ '^pg_' and nspname <> 'information_schema'
                ) fkey
          ) fkey ON fkey.conrelid = pg_attribute.attrelid AND fkey.column_index = pg_attribute.attnum
          GROUP BY conrelid, conname
      ) candidate_index
      JOIN pg_class ON pg_class.oid = candidate_index.conrelid
      LEFT JOIN pg_index ON pg_index.indrelid = conrelid AND indkey::text = array_to_string(column_list, ' ')
      WHERE indexrelid IS NULL
    ) LOOP
	      RAISE NOTICE 'Executing "%"...', result.command;
	      EXECUTE result.command;
	      count := count + 1;
    END LOOP;
    RETURN count;
END;$$ LANGUAGE 'plpgsql';
SELECT 'Number of created FK indexes: ' || create_fk_indexes();

/**
 * Alter columns to TEXT types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
DROP FUNCTION IF EXISTS alter_text_columns();
CREATE FUNCTION alter_text_columns() RETURNS INTEGER AS $$
DECLARE result RECORD;
DECLARE count INT = 0;
BEGIN
    RAISE NOTICE 'Altering text columns...';
    FOR result IN (
        SELECT 'ALTER TABLE ' || tables.table_name || ' ALTER COLUMN ' || columns.column_name || ' TYPE TEXT' AS command
        FROM information_schema.tables AS tables
        LEFT JOIN information_schema.columns AS columns ON columns.table_name = tables.table_name
        WHERE tables.table_schema = 'public'
          AND tables.table_type = 'BASE TABLE'
          AND columns.character_maximum_length IS NOT NULL
          AND columns.column_name IN ('command', 'description', 'reason', 'meeting_name', 'meeting_description', 'room_description')
    ) LOOP
	      RAISE NOTICE 'Executing "%"...', result.command;
	      EXECUTE result.command;
	      count := count + 1;
    END LOOP;
    RETURN count;
END;$$ LANGUAGE 'plpgsql';
SELECT 'Number of columns changed to text type: ' || alter_text_columns();

/**
 * View of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW resource_summary AS
SELECT
    resource.id AS id,
    resource.resource_id AS parent_resource_id,
    resource.user_id AS user_id,
    resource.name AS name,
    resource.allocatable AS allocatable,
    resource.allocation_order AS allocation_order,
    resource.description AS description,
    resource.calendar_public AS calendar_public,
    resource.calendar_uri_key AS calendar_uri_key,
    resource.confirm_by_owner AS confirm_by_owner,
    string_agg(device_resource_technologies.technologies, ',') AS technologies,
    CASE
      WHEN resource.id IN (SELECT resource_id FROM capability INNER JOIN room_provider_capability on room_provider_capability.id = capability.id) THEN 'ROOM_PROVIDER'
      WHEN resource.id IN (SELECT resource_id FROM capability INNER JOIN recording_capability on recording_capability.id = capability.id) THEN 'RECORDING_SERVICE'
      ELSE 'RESOURCE'
    END AS type,
    string_agg(tag.id || ',' || tag.name || ',' || tag.type || ',' || COALESCE(tag.data #>> '{}', ''), '|') AS tags
FROM resource
LEFT JOIN device_resource ON device_resource.id = resource.id
LEFT JOIN device_resource_technologies ON device_resource_technologies.device_resource_id = device_resource.id
LEFT JOIN resource_tag ON resource.id = resource_tag.resource_id
LEFT JOIN tag ON resource_tag.tag_id = tag.id
GROUP BY resource.id;

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
CREATE VIEW specification_summary_view AS
SELECT
    specification.id AS id,
    string_agg(specification_technologies.technologies, ',') AS technologies,
    CASE
        WHEN room_specification.id IS NOT NULL AND room_specification.participant_count IS NULL THEN 'PERMANENT_ROOM'
        WHEN room_specification.id IS NOT NULL AND room_specification.reused_room THEN 'USED_ROOM'
        WHEN room_specification.id IS NOT NULL THEN 'ROOM'
        WHEN alias_specification_summary.id IS NOT NULL THEN 'ALIAS'
        WHEN resource_specification.id IS NOT NULL THEN 'RESOURCE'
        ELSE 'OTHER'
    END AS type,
    alias_specification_summary.room_name AS alias_room_name,
    room_specification.participant_count AS room_participant_count,
    COALESCE(value_provider_capability.resource_id, resource_specification.resource_id, room_specification.device_resource_id) AS resource_id
FROM specification
LEFT JOIN specification_technologies ON specification_technologies.specification_id = specification.id
LEFT JOIN room_specification ON room_specification.id = specification.id
LEFT JOIN resource_specification ON resource_specification.id = specification.id
LEFT JOIN alias_specification_summary ON alias_specification_summary.id = specification.id
LEFT JOIN value_specification ON value_specification.id = specification.id
LEFT JOIN value_provider ON value_provider.id = value_specification.value_provider_id
LEFT JOIN capability AS value_provider_capability ON value_provider_capability.id = value_provider.capability_id
GROUP BY
    specification.id,
    alias_specification_summary.id,
    alias_specification_summary.room_name,
    room_specification.id,
    resource_specification.id,
    value_provider_capability.id;

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
    reservation.id AS last_reservation_id,
    executable.id AS last_executable_id
FROM reservation_request
LEFT JOIN abstract_reservation_request ON abstract_reservation_request.id = reservation_request.id
LEFT JOIN reservation ON reservation.allocation_id = abstract_reservation_request.allocation_id AND abstract_reservation_request.state = 'ACTIVE'
LEFT JOIN executable ON executable.id = reservation.executable_id
GROUP BY reservation_request.id, executable.id, reservation.id
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
  LEFT JOIN abstract_reservation_request AS usage ON usage.state = 'ACTIVE' AND usage.reused_allocation_id = abstract_reservation_request.allocation_id
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
    abstract_reservation_request.reused_allocation_id AS allocation_id,
    MIN(CASE WHEN reservation_request.slot_end > (now() at time zone 'UTC') THEN reservation_request.slot_end ELSE NULL END) AS slot_end_future_min
  FROM reservation_request
  LEFT JOIN abstract_reservation_request ON abstract_reservation_request.id = reservation_request.id
  WHERE reservation_request.allocation_state = 'ALLOCATED' AND abstract_reservation_request.reused_allocation_id IS NOT NULL
  GROUP BY abstract_reservation_request.reused_allocation_id
) AS reservation_request_usage_slots
/* join usages which matches the "future minimum" or the "whole maximum" slot ending */
INNER JOIN allocation ON allocation.id = reservation_request_usage_slots.allocation_id
INNER JOIN abstract_reservation_request ON abstract_reservation_request.reused_allocation_id = reservation_request_usage_slots.allocation_id
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
            parent_allocation.abstract_reservation_request_id AS parent_reservation_request_id,
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
            abstract_reservation_request.aux_data #>> '{}' AS aux_data,
            reservation_request_set_earliest_child.child_id AS child_id,
            reservation_request_set_earliest_child.future_child_count AS future_child_count,
            COALESCE(reservation_request.slot_start, reservation_request_set_earliest_child.slot_start) AS slot_start,
            COALESCE(reservation_request.slot_end, reservation_request_set_earliest_child.slot_end) AS slot_end,
            reservation_request_state.allocation_state AS allocation_state,
            reservation_request_state.executable_state AS executable_state,
            reservation_request_state.last_reservation_id AS last_reservation_id,
            reservation_request_state.last_executable_id AS last_executable_id,
            reservation_request_active_usage.executable_state AS usage_executable_state,
            reservation_request_earliest_usage.slot_start AS usage_slot_start,
            reservation_request_earliest_usage.slot_end AS usage_slot_end,
            CASE
              WHEN last_reservation_id IN (SELECT id FROM abstract_foreign_reservation) THEN FALSE
              ELSE TRUE
            END AS allowCache
        FROM abstract_reservation_request
        LEFT JOIN allocation AS reused_allocation ON reused_allocation.id = abstract_reservation_request.reused_allocation_id
        LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
        LEFT JOIN allocation AS parent_allocation ON parent_allocation.id = reservation_request.parent_allocation_id
        LEFT JOIN reservation_request_set_earliest_child ON reservation_request_set_earliest_child.id = abstract_reservation_request.id
        LEFT JOIN reservation_request_earliest_usage ON reservation_request_earliest_usage.id = reservation_request.id
        LEFT JOIN reservation_request_state ON reservation_request_state.id = COALESCE(reservation_request.id, reservation_request_set_earliest_child.child_id)
        LEFT JOIN reservation_request_active_usage ON reservation_request_active_usage.id = COALESCE(reservation_request.id, reservation_request_set_earliest_child.child_id)
    ) AS reservation_request_summary
) AS reservation_request_summary;

/**
 * View of summary for each reservation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
CREATE VIEW reservation_summary AS
WITH RECURSIVE reservation_allocation AS (
    SELECT reservation0.id AS id,
           reservation0.allocation_id AS allocation_id,
           0 as level
      FROM reservation AS reservation0
     WHERE reservation0.reservation_id IS NULL
    UNION ALL
    SELECT reservationN.id AS id,
           COALESCE(parent_reservation.allocation_id, reservationN.allocation_id) AS allocation_id,
           parent_reservation.level + 1 AS level
      FROM reservation AS reservationN
      JOIN reservation_allocation AS parent_reservation ON reservationN.reservation_id = parent_reservation.id
)
SELECT
    reservation.id AS id,
    reservation.user_id AS user_id,
    allocation.abstract_reservation_request_id AS reservation_request_id,
    CASE
      WHEN resource_reservation.id IS NOT NULL THEN 'RESOURCE'
      WHEN foreign_resource_reservation.id IS NOT NULL THEN 'RESOURCE'
      WHEN room_reservation.id IS NOT NULL THEN 'ROOM'
      WHEN alias_reservation.id IS NOT NULL THEN 'ALIAS'
      WHEN value_reservation.id IS NOT NULL THEN 'VALUE'
      WHEN recording_service_reservation.id IS NOT NULL THEN 'RECORDING_SERVICE'
      ELSE 'OTHER'
    END AS type,
    reservation.slot_start AS slot_start,
    reservation.slot_end AS slot_end,
    COALESCE(
        resource_reservation.resource_id,
        room_capability.resource_id,
        alias_capability.resource_id,
        value_capability.resource_id,
        recording_capability.resource_id
    ) AS resource_id,
    foreign_resource_reservation.foreign_resources_id as foreign_resources_id,
    room_reservation.license_count AS room_license_count,
    CAST(NULL AS TEXT) AS room_name,
    STRING_AGG(alias.type, ',') AS alias_types,
    value_reservation.value AS value,
    abstract_reservation_request.description AS reservation_request_description,
    parent_allocation.abstract_reservation_request_id as parent_reservation_request_id
FROM reservation
LEFT JOIN reservation_allocation ON reservation_allocation.id = reservation.id
LEFT JOIN allocation ON allocation.id = reservation_allocation.allocation_id
LEFT JOIN resource_reservation ON resource_reservation.id = reservation.id
LEFT JOIN foreign_resource_reservation ON foreign_resource_reservation.id = reservation.id
LEFT JOIN room_reservation ON room_reservation.id = reservation.id
LEFT JOIN capability AS room_capability ON room_capability.id = room_reservation.room_provider_capability_id
LEFT JOIN alias_reservation ON alias_reservation.id = reservation.id
LEFT JOIN capability AS alias_capability ON alias_capability.id = alias_reservation.alias_provider_capability_id
LEFT JOIN alias_provider_capability_aliases ON alias_provider_capability_aliases.alias_provider_capability_id = alias_reservation.alias_provider_capability_id
LEFT JOIN alias ON alias.id = alias_provider_capability_aliases.alias_id
LEFT JOIN value_reservation ON value_reservation.id = reservation.id OR value_reservation.id = alias_reservation.value_reservation_id
LEFT JOIN value_provider_capability ON value_provider_capability.value_provider_id = value_reservation.value_provider_id
LEFT JOIN capability AS value_capability ON value_capability.id = value_provider_capability.id
LEFT JOIN recording_service_reservation ON recording_service_reservation.id = reservation.id
LEFT JOIN capability AS recording_capability ON recording_capability.id = recording_service_reservation.recording_capability_id
LEFT JOIN abstract_reservation_request ON abstract_reservation_request.id = allocation.abstract_reservation_request_id
LEFT JOIN reservation_request ON reservation_request.id = abstract_reservation_request.id
LEFT JOIN allocation AS parent_allocation ON parent_allocation.id = reservation_request.parent_allocation_id
GROUP BY reservation.id,
         allocation.id,
         resource_reservation.id,
         foreign_resource_reservation.id,
         room_reservation.id,
         room_capability.id,
         alias_reservation.id,
         alias_capability.id,
         value_reservation.id,
         value_capability.id,
         recording_service_reservation.id,
         recording_capability.id,
         abstract_reservation_request.description,
         parent_allocation.abstract_reservation_request_id;

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
CREATE VIEW executable_summary_view AS
SELECT
    DISTINCT ON(executable.id)
    executable.id AS id,
    room_provider_capability.resource_id AS resource_id,
    CASE
        WHEN used_room_endpoint.id IS NOT NULL THEN 'USED_ROOM'
        WHEN room_endpoint.id IS NOT NULL THEN 'ROOM'
        ELSE 'OTHER'
    END AS type,
    CASE
        WHEN room_endpoint.id IS NOT NULL THEN execution_target.slot_start + cast(room_endpoint.slot_minutes_before || ' MINUTES' AS INTERVAL)
        ELSE execution_target.slot_start
    END AS slot_start,
    CASE
        WHEN room_endpoint.id IS NOT NULL THEN execution_target.slot_end - cast(room_endpoint.slot_minutes_after || ' MINUTES' AS INTERVAL)
        ELSE execution_target.slot_end
    END AS slot_end,
    executable.state AS state,
    alias.value AS room_name,
    string_agg(DISTINCT room_configuration_technologies.technologies, ',') AS room_technologies,
    room_configuration.license_count AS room_license_count,
    room_endpoint.room_description AS room_description,
    used_room_endpoint.room_endpoint_id AS room_id,
    COUNT(recording_service.id) > 0 AS room_has_recording_service,
    COUNT(recording_service.id) > 0 OR COUNT(resource_room_endpoint_recording_folder_ids.recording_folder_id) > 0 AS room_has_recordings
FROM executable
LEFT JOIN execution_target ON execution_target.id = executable.id
LEFT JOIN room_endpoint ON room_endpoint.id = executable.id
LEFT JOIN used_room_endpoint ON used_room_endpoint.id = executable.id
LEFT JOIN room_configuration ON room_configuration.id = room_endpoint.room_configuration_id
LEFT JOIN room_configuration_technologies ON room_configuration_technologies.room_configuration_id = room_configuration.id
LEFT JOIN endpoint_assigned_aliases ON endpoint_assigned_aliases.endpoint_id = executable.id OR endpoint_assigned_aliases.endpoint_id = used_room_endpoint.room_endpoint_id
LEFT JOIN alias ON alias.id = endpoint_assigned_aliases.alias_id AND alias.type = 'ROOM_NAME'
LEFT JOIN used_room_endpoint AS room_endpoint_usage ON room_endpoint_usage.room_endpoint_id = executable.id
LEFT JOIN resource_room_endpoint ON resource_room_endpoint.id = room_endpoint.id OR resource_room_endpoint.id = used_room_endpoint.room_endpoint_id
LEFT JOIN capability AS room_provider_capability ON room_provider_capability.id = resource_room_endpoint.room_provider_capability_id
LEFT JOIN executable_service ON executable_service.executable_id = executable.id OR executable_service.executable_id = room_endpoint_usage.id
LEFT JOIN recording_service ON recording_service.id = executable_service.id
LEFT JOIN resource_room_endpoint_recording_folder_ids ON resource_room_endpoint_recording_folder_ids.resource_room_endpoint_id = executable.id OR resource_room_endpoint_recording_folder_ids.resource_room_endpoint_id = used_room_endpoint.room_endpoint_id
GROUP BY
    executable.id,
    execution_target.id,
    room_endpoint.id,
    room_provider_capability.id,
    used_room_endpoint.id,
    room_configuration.id,
    alias.id
ORDER BY executable.id, alias.id;

CREATE TABLE executable_summary AS SELECT * FROM executable_summary_view;
CREATE TABLE specification_summary AS SELECT * FROM specification_summary_view;
