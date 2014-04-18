/**
 * 2014-03-28: Refactorize resource administrators to owners and additional emails
 */
BEGIN TRANSACTION;

/* Create additional resource administrators emails */
CREATE TABLE resource_administrator_emails (resource_id INT8 NOT NULL, administrator_emails VARCHAR(255));
ALTER TABLE resource_administrator_emails ADD CONSTRAINT FKA5741D9A35C5863A FOREIGN KEY(resource_id) REFERENCES resource;
ALTER TABLE resource_administrator_emails OWNER TO shongo;

/* Insert additional resource administrators emails */
INSERT INTO resource_administrator_emails(resource_id, administrator_emails)
    SELECT resource_id, email FROM resource_administrators
    LEFT JOIN person ON person.id = resource_administrators.person_id
    WHERE dtype = 'AnonymousPerson';

/* Insert not existing 'RESOURCE' acl_object_class */
INSERT INTO acl_object_class(id, class)
    SELECT nextval('hibernate_sequence'), 'RESOURCE' AS class
    WHERE 'RESOURCE' NOT IN (SELECT acl_object_class.class FROM acl_object_class);

/* Insert not existing acl_object_classes for resources with administrators */
INSERT INTO acl_object_identity(id, acl_object_class_id, object_id)
    SELECT nextval('hibernate_sequence'), acl_object_class.id, resource_administrators.resource_id FROM resource_administrators
    LEFT JOIN person ON person.id = resource_administrators.person_id
    LEFT JOIN acl_object_class ON class = 'RESOURCE'
    LEFT JOIN acl_object_identity ON acl_object_identity.object_id = resource_administrators.resource_id AND acl_object_identity.acl_object_class_id = acl_object_class.id
    WHERE dtype = 'UserPerson' AND acl_object_identity.id IS NULL;

/* Insert not existing acl_identities for resource administrators */
INSERT INTO acl_identity(id, type, principal_id)
    SELECT nextval('hibernate_sequence'), 'USER', person.user_id FROM resource_administrators
    LEFT JOIN person ON person.id = resource_administrators.person_id
    LEFT JOIN acl_identity ON acl_identity.principal_id = person.user_id AND acl_identity.type = 'USER'
    WHERE dtype = 'UserPerson' AND acl_identity.id IS NULL;

/* Insert not existing acl_entries for resource administrators */
INSERT INTO acl_entry (id, acl_identity_id, acl_object_identity_id, role)
    SELECT nextval('hibernate_sequence'), acl_identity.id, acl_object_identity.id, 'OWNER' FROM resource_administrators
    LEFT JOIN person ON person.id = resource_administrators.person_id
    LEFT JOIN acl_identity ON acl_identity.principal_id = person.user_id AND acl_identity.type = 'USER'
    LEFT JOIN acl_object_class ON class = 'RESOURCE'
    LEFT JOIN acl_object_identity ON acl_object_identity.object_id = resource_administrators.resource_id AND acl_object_identity.acl_object_class_id = acl_object_class.id
    LEFT JOIN acl_entry ON acl_entry.acl_identity_id = acl_identity.id AND acl_entry.acl_object_identity_id = acl_object_identity.id AND acl_entry.role = 'OWNER'
    WHERE dtype = 'UserPerson' AND acl_entry.id IS NULL;

/* Drop old resource administrators */
DROP TABLE resource_administrators;

COMMIT TRANSACTION;
