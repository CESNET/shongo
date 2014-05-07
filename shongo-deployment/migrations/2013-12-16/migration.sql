/**
 * 2013-12-01: Perun aware user settings.
 * 1) Add home_time_zone and use_web_service columns to user_settings table
 * 2) Alter user_settings with czech locale to use web service for loading locale (default web service locale is czech)
 * 3) Set home_time_zone to value from time_zone
 * 4) Remove time_zone column from user_settings table.
 */
BEGIN TRANSACTION;
ALTER TABLE user_settings ADD COLUMN home_time_zone VARCHAR(255);
ALTER TABLE user_settings ADD COLUMN use_web_service BOOLEAN DEFAULT FALSE;
UPDATE user_settings SET locale = null, use_web_service = true WHERE locale = 'cs';
UPDATE user_settings SET home_time_zone = time_zone;
ALTER TABLE user_settings DROP COLUMN time_zone;
COMMIT TRANSACTION;


/**
 * 2013-12-04: Refactorization of permanent rooms to room specifications.
 */
BEGIN TRANSACTION;
/**
 * Alter tables
 */
ALTER TABLE alias_provider_capability DROP COLUMN permanent_room;
ALTER TABLE alias_set_specification DROP COLUMN shared_executable;
ALTER TABLE alias_specification DROP COLUMN permanent_room;
DROP TABLE alias_specification_permanent_room_participants;
ALTER TABLE room_specification ALTER COLUMN participant_count DROP NOT NULL;
ALTER TABLE room_specification ADD COLUMN allocation_id int8;
ALTER TABLE room_specification ADD CONSTRAINT fk_room_specification_allocation_id foreign key (allocation_id) references allocation;
/**
 * UPDATE permanent rooms from alias_specifications
 *
 * Initialize */
ALTER TABLE room_specification ADD COLUMN alias_specification_id int8;
/* Create room_specifications from standalone alias_specifications */
INSERT INTO room_specification(id, alias_specification_id)
    SELECT alias_specification.id, nextval('hibernate_sequence') FROM abstract_reservation_request
    LEFT JOIN alias_specification ON alias_specification.id = abstract_reservation_request.specification_id
    WHERE alias_specification.id IS NOT NULL;
/* Create duplicates of alias_specifications */
INSERT INTO specification (id)
    SELECT room_specification.alias_specification_id FROM alias_specification
    LEFT JOIN room_specification ON room_specification.id = alias_specification.id
    WHERE room_specification.alias_specification_id IS NOT NULL;
INSERT INTO alias_specification (id, alias_provider_capability_id, value)
    SELECT room_specification.alias_specification_id, alias_provider_capability_id, value FROM alias_specification
    LEFT JOIN room_specification ON room_specification.id = alias_specification.id
    WHERE room_specification.alias_specification_id IS NOT NULL;
/* Create types and technologies to duplicates */
UPDATE alias_specification_alias_types SET alias_specification_id = (
    SELECT room_specification.alias_specification_id FROM room_specification
    WHERE room_specification.id = alias_specification_alias_types.alias_specification_id
)
WHERE alias_specification_alias_types.alias_specification_id IN (
    SELECT room_specification.id FROM room_specification
    WHERE room_specification.alias_specification_id IS NOT NULL
);
UPDATE alias_specification_alias_technologies SET alias_specification_id = (
    SELECT room_specification.alias_specification_id FROM room_specification
    WHERE room_specification.id = alias_specification_alias_technologies.alias_specification_id
)
WHERE alias_specification_alias_technologies.alias_specification_id IN (
    SELECT room_specification.id FROM room_specification
    WHERE room_specification.alias_specification_id IS NOT NULL
);
/* Delete old alias specifications */
DELETE FROM alias_specification WHERE alias_specification.id IN (SELECT room_specification.id FROM room_specification);
/* Move standalone alias_specifications into room_specifications */
INSERT INTO room_specification_alias_specifications(room_specification_id, alias_specification_id)
    SELECT room_specification.id, room_specification.alias_specification_id FROM room_specification
    WHERE room_specification.alias_specification_id IS NOT NULL;
/* De-initialize */
ALTER TABLE room_specification DROP COLUMN alias_specification_id;
/**
 * UPDATE permanent rooms from alias_set_specifications
 *
 * Create room_specifications from alias_set_specifications */
INSERT INTO room_specification(id)
    SELECT alias_set_specification.id FROM alias_set_specification;
/* Move alias_specifications from alias_set_specifications to room_specifications */
INSERT INTO room_specification_alias_specifications(room_specification_id, alias_specification_id)
    SELECT alias_set_specification_id, alias_specification_id  FROM alias_set_specification_alias_specifications;
DELETE FROM alias_set_specification_alias_specifications;
/* Delete alias_set_specifications */
DELETE FROM alias_set_specification;
/**
 * UPDATE permanent room capacities
 *
 * Copy reused_allocation_id from abstract_reservation_request to allocation_id in room_specification */
UPDATE room_specification SET allocation_id = (
    SELECT abstract_reservation_request.reused_allocation_id
    FROM abstract_reservation_request
    WHERE abstract_reservation_request.specification_id = room_specification.id
) WHERE room_specification.id IN (
    SELECT room_specification.id
    FROM abstract_reservation_request
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE abstract_reservation_request.reused_allocation_id IS NOT NULL AND room_specification.id IS NOT NULL
);
/* Clear reused_allocation_id in abstract_reservation_request */
UPDATE abstract_reservation_request SET reused_allocation_id = NULL WHERE abstract_reservation_request.id IN(
    SELECT abstract_reservation_request.id FROM abstract_reservation_request
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE abstract_reservation_request.reused_allocation_id IS NOT NULL AND room_specification.id IS NOT NULL
);
COMMIT TRANSACTION;


/**
 * 2013-12-10: Rename abstract_person to person.
 */
BEGIN TRANSACTION;
ALTER TABLE abstract_person RENAME TO person;
ALTER TABLE resource_administrators RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE device_resource_permanent_persons RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE person_participant RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE endpoint_persons RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE endpoint_participant_persons RENAME COLUMN abstract_person_id TO person_id;
/* Refactorize room_specification.allocation_id back to abstract_reservation_request.reused_allocation_id */
ALTER TABLE room_specification ADD COLUMN reused_room boolean default false;
/* Copy allocation_id from room_specification to reused_allocation_id in abstract_reservation_request */
UPDATE abstract_reservation_request SET reused_allocation_id = (
    SELECT room_specification.allocation_id
    FROM abstract_reservation_request AS source
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE source.id = abstract_reservation_request.id
) WHERE abstract_reservation_request.id IN (
    SELECT abstract_reservation_request.id
    FROM abstract_reservation_request
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE room_specification.allocation_id IS NOT NULL
);
/* Set reused_room to room_specification */
UPDATE room_specification SET reused_room = true WHERE room_specification.id IN (
    SELECT room_specification.id
    FROM abstract_reservation_request
    LEFT JOIN room_specification ON room_specification.id = abstract_reservation_request.specification_id
    WHERE abstract_reservation_request.reused_allocation_id IS NOT NULL AND room_specification.id IS NOT NULL
);
ALTER TABLE room_specification DROP COLUMN allocation_id CASCADE;
COMMIT TRANSACTION;


/**
 * 2013-12-12: Little refactorizations.
 */
BEGIN TRANSACTION;
ALTER TABLE acl_record RENAME COLUMN role TO entity_role;
UPDATE person_participant SET role = 'ADMINISTRATOR' WHERE role = 'ADMIN';
COMMIT TRANSACTION;


/**
 * 2013-12-16: Refactorizations of acl_record to acl_entry.
 */
BEGIN TRANSACTION;
CREATE TABLE acl_entry (id int8 not null, role varchar(255) not null, acl_identity_id int8 not null, acl_object_identity_id int8 not null, primary key (id), unique (acl_identity_id, acl_object_identity_id, role));
CREATE TABLE acl_entry_dependency (id int8 not null, type varchar(255), child_acl_entry_id int8 not null, parent_acl_entry_id int8 not null, primary key (id), unique (parent_acl_entry_id, child_acl_entry_id));
CREATE TABLE acl_identity (id int8 not null, principal_id varchar(255) not null, type varchar(255) not null, primary key (id), unique (type, principal_id));
CREATE TABLE acl_object_class (id int8 not null, class varchar(255) not null unique, primary key (id));
CREATE TABLE acl_object_identity (id int8 not null, object_id int8 not null, acl_object_class_id int8 not null, primary key (id), unique (acl_object_class_id, object_id));
ALTER TABLE acl_entry add constraint FK5302D47DCC9409C0 foreign key (acl_identity_id) references acl_identity;
ALTER TABLE acl_entry add constraint FK5302D47D748E9AE9 foreign key (acl_object_identity_id) references acl_object_identity;
ALTER TABLE acl_entry_dependency add constraint FK35BB944D9513D3B7 foreign key (child_acl_entry_id) references acl_entry;
ALTER TABLE acl_entry_dependency add constraint FK35BB944D786976E9 foreign key (parent_acl_entry_id) references acl_entry;
ALTER TABLE acl_object_identity add constraint FK2A2BB0097D29732B foreign key (acl_object_class_id) references acl_object_class;
/* Create user identities */
INSERT INTO acl_identity(id, type, principal_id)
    SELECT nextval('hibernate_sequence'), 'USER', user_id FROM acl_record GROUP BY user_id;
/* Create object classes */
INSERT INTO acl_object_class(id, class)
    SELECT nextval('hibernate_sequence'), data.object_class FROM (
        SELECT DISTINCT CASE WHEN entity_type = 'ALLOCATION' THEN 'RESERVATION_REQUEST' ELSE entity_type END AS object_class FROM acl_record
    ) AS data;
/* Create object identities */
INSERT INTO acl_object_identity(id, acl_object_class_id, object_id)
    SELECT nextval('hibernate_sequence'), acl_object_class.id, data.object_id FROM(
        SELECT CASE WHEN entity_type = 'ALLOCATION' THEN 'RESERVATION_REQUEST' ELSE entity_type END AS object_class, entity_id AS object_id FROM acl_record GROUP BY entity_type, entity_id
    ) AS data
    LEFT JOIN acl_object_class ON acl_object_class.class = data.object_class;
/* Create entries */
INSERT INTO acl_entry (id, acl_identity_id, acl_object_identity_id, role)
    SELECT nextval('hibernate_sequence'), acl_identity.id, acl_object_identity.id, data.role FROM(
        SELECT user_id, CASE WHEN entity_type = 'ALLOCATION' THEN 'RESERVATION_REQUEST' ELSE entity_type END AS object_class, entity_id AS object_id, entity_role AS role FROM acl_record
    ) AS data
    LEFT JOIN acl_identity ON acl_identity.type = 'USER' AND acl_identity.principal_id = data.user_id
    LEFT JOIN acl_object_class ON acl_object_class.class = data.object_class
    LEFT JOIN acl_object_identity ON acl_object_identity.acl_object_class_id = acl_object_class.id AND acl_object_identity.object_id = data.object_id;
DROP TABLE acl_record_dependency;
DROP TABLE acl_record;

ALTER DATABASE shongo OWNER TO shongo;
ALTER SCHEMA public OWNER TO shongo;
ALTER TABLE acl_entry OWNER TO shongo;
ALTER TABLE acl_entry_dependency OWNER TO shongo;
ALTER TABLE acl_identity OWNER TO shongo;
ALTER TABLE acl_object_class OWNER TO shongo;
ALTER TABLE acl_object_identity OWNER TO shongo;

COMMIT TRANSACTION;