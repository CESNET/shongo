/**
 * 2013-11-11:
 * 1) Create table ExecutionTarget.
 * 2) Create records in ExecutionTarget for each executable.
 * 3) Move columns from Executable to ExecutionTarget.
 * 4) Create table ExecutionReport.
 * 5) Create records in ExecutionReport.
 * 6) Rename record discriminator values in ExecutionReport.
 * 7) Delete table ExecutableReport.
 */
BEGIN TRANSACTION;

DROP VIEW IF EXISTS executable_summary;
DROP VIEW IF EXISTS room_endpoint_earliest_usage;

CREATE TABLE execution_target (
    id int8 NOT NULL,
    attempt_count INTEGER DEFAULT 0 NOT NULL,
    next_attempt TIMESTAMP,
    slot_end TIMESTAMP,
    slot_start TIMESTAMP,
    PRIMARY KEY(id)
);

INSERT INTO execution_target(id, attempt_count, next_attempt, slot_end, slot_start)
    SELECT id, attempt_count, next_attempt, slot_end, slot_start FROM executable;

ALTER TABLE executable DROP COLUMN attempt_count;
ALTER TABLE executable DROP COLUMN next_attempt;
ALTER TABLE executable DROP COLUMN slot_start;
ALTER TABLE executable DROP COLUMN slot_end;

CREATE TABLE execution_report (
    dtype VARCHAR(50) NOT NULL,
    id INT8 NOT NULL,
    date_time TIMESTAMP,
    command VARCHAR(255),
    room_name VARCHAR(255),
    execution_target_id INT8,
    jadereport_id BIGINT,
    PRIMARY KEY(id)
);

INSERT INTO execution_report(dtype, id, date_time, command, room_name, execution_target_id, jadereport_id)
    SELECT dtype, id, date_time, command, room_name, executable_id, jadereport_id FROM executable_report;

DROP TABLE executable_report;

ALTER DATABASE shongo OWNER TO shongo;
ALTER SCHEMA public OWNER TO shongo;
ALTER TABLE execution_target OWNER TO shongo;
ALTER TABLE execution_report OWNER TO shongo;

COMMIT TRANSACTION;