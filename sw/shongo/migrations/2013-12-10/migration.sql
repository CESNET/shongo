BEGIN TRANSACTION;

ALTER TABLE abstract_person RENAME TO person;
ALTER TABLE resource_administrators RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE device_resource_permanent_persons RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE person_participant RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE endpoint_persons RENAME COLUMN abstract_person_id TO person_id;
ALTER TABLE endpoint_participant_persons RENAME COLUMN abstract_person_id TO person_id;

COMMIT TRANSACTION;