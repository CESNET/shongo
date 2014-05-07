/**
 * 2014-04-07: Refactorize scheduler reports
 */
BEGIN TRANSACTION;

/* Rename columns */
ALTER TABLE execution_report RENAME COLUMN jadereport_id TO jade_report_id;
ALTER TABLE scheduler_report RENAME COLUMN usageinterval_start TO usage_interval_start;
ALTER TABLE scheduler_report RENAME COLUMN usageinterval_end TO usage_interval_end;
ALTER TABLE scheduler_report RENAME COLUMN executableslot_start TO executable_slot_start;
ALTER TABLE scheduler_report RENAME COLUMN executableslot_end TO executable_slot_end;
ALTER TABLE scheduler_report RENAME COLUMN serviceslot_start TO service_slot_start;
ALTER TABLE scheduler_report RENAME COLUMN serviceslot_end TO service_slot_end;
ALTER TABLE scheduler_report RENAME COLUMN reservationrequest_id TO reservation_request_id;
ALTER TABLE scheduler_report RENAME COLUMN endpointfrom_id TO endpoint_from_id;
ALTER TABLE scheduler_report RENAME COLUMN endpointto_id TO endpoint_to_id;
ALTER TABLE scheduler_report RENAME COLUMN usagereservationrequest_id TO usage_reservation_request_id;

/* Rename collection for TechnologySet in AllocatingRoomReport */
ALTER TABLE scheduler_report_technologies RENAME TO scheduler_report_technology_sets;

/* Rename collection for Set<AliasType> in AllocatingAliasReport */
ALTER TABLE scheduler_report_set_allocating_alias_report_alias_types RENAME TO scheduler_report_alias_types;
ALTER TABLE scheduler_report_alias_types RENAME COLUMN scheduler_report_set_allocating_alias_report_id TO scheduler_report_id;
/* Refactorize values from enum index to enum name */
ALTER TABLE scheduler_report_alias_types RENAME COLUMN alias_types TO old_alias_types;
ALTER TABLE scheduler_report_alias_types ADD COLUMN alias_types varchar(255);
UPDATE scheduler_report_alias_types SET alias_types = CASE old_alias_types
    WHEN 0 THEN 'ROOM_NAME'
    WHEN 1 THEN 'H323_E164'
    WHEN 2 THEN 'H323_URI'
    WHEN 3 THEN 'H323_IP'
    WHEN 4 THEN 'SIP_URI'
    WHEN 5 THEN 'SIP_IP'
    ELSE 'ADOBE_CONNECT_URI'
END;
ALTER TABLE scheduler_report_alias_types DROP COLUMN old_alias_types;

/* Rename collection for Map<String, String> in CollidingReservationsReport */
ALTER TABLE scheduler_report_set_colliding_reservations_report_reservations RENAME TO scheduler_report_reservations;
ALTER TABLE scheduler_report_reservations RENAME COLUMN scheduler_report_set_colliding_reservations_report_id TO scheduler_report_id;

/* Rename collection for List<String> in ReallocatingReservationRequestsReport */
ALTER TABLE scheduler_report_set_reallocating_reservation_requests_report_r RENAME TO scheduler_report_reservation_requests;
ALTER TABLE scheduler_report_reservation_requests RENAME COLUMN scheduler_report_set_reallocating_reservation_requests_report_i TO scheduler_report_id;

/* Create collection for Set<Technology> in EndpointNotFoundReport, AllocatingAliasReport */
CREATE TABLE scheduler_report_technologies (scheduler_report_id int8 not null, technologies varchar(255));
ALTER TABLE scheduler_report_technologies add constraint FKCEC20E5136C64819 foreign key (scheduler_report_id) references scheduler_report;
ALTER TABLE scheduler_report_technologies OWNER TO shongo;
/* Fill data from old technologies tables */
INSERT INTO scheduler_report_technologies(scheduler_report_id, technologies)
    SELECT
         data.scheduler_report_id,
         CASE data.technologies
             WHEN 1 THEN 'H323'
             WHEN 2 THEN 'SIP'
             ELSE 'ADOBE_CONNECT'
         END
    FROM (
        SELECT scheduler_report_set_allocating_alias_report_id AS scheduler_report_id, technologies AS technologies
        FROM scheduler_report_set_allocating_alias_report_technologies
        UNION ALL
        SELECT scheduler_report_set_endpoint_not_found_report_id AS scheduler_report_id, technologies AS technologies
        FROM scheduler_report_set_endpoint_not_found_report_technologies
    ) AS data;
/* Drop old technologies tables */
DROP TABLE scheduler_report_set_allocating_alias_report_technologies;
DROP TABLE scheduler_report_set_endpoint_not_found_report_technologies;

COMMIT TRANSACTION;
