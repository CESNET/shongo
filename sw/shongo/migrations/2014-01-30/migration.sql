/**
 * 2014-01-30: Removed persistent migration and move the values to the executables.
 * 1) Add columns migrate_from_executable_id and migrate_to_executable_id to executable table
 * 2) Copy from migration table to executable table
 * 3) Delete migration table
 */
BEGIN TRANSACTION;

ALTER TABLE executable ADD COLUMN migrate_from_executable_id INT8;
ALTER TABLE executable ADD COLUMN migrate_to_executable_id INT8;
ALTER TABLE executable ADD CONSTRAINT FK2024844A31716A3D FOREIGN KEY (migrate_from_executable_id) REFERENCES executable;
ALTER TABLE executable ADD CONSTRAINT FK2024844A252A544E FOREIGN KEY (migrate_to_executable_id) REFERENCES executable;

UPDATE executable SET migrate_to_executable_id = (
	SELECT migration.target_executable_id FROM migration
	WHERE migration.source_executable_id = executable.id
) WHERE executable.id IN (
	SELECT migration.source_executable_id FROM migration
	WHERE migration.source_executable_id IS NOT NULL AND migration.target_executable_id IS NOT NULL
);

UPDATE executable SET migrate_from_executable_id = (
	SELECT migration.source_executable_id FROM migration
	WHERE migration.target_executable_id = executable.id
) WHERE executable.id IN (
	SELECT migration.target_executable_id FROM migration
	WHERE migration.source_executable_id IS NOT NULL AND migration.target_executable_id IS NOT NULL
);

DROP TABLE migration;

COMMIT TRANSACTION;
