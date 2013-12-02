/**
 * Change acl_record.id in table acl_record_dependency.
 */
CREATE FUNCTION alc_record_change_id(oldId bigint, newId bigint) RETURNS void AS $$
BEGIN
    UPDATE acl_record_dependency SET parent_acl_record_id = newId WHERE parent_acl_record_id = oldId;
    UPDATE acl_record_dependency SET child_acl_record_id = newId WHERE child_acl_record_id = oldId;
END;
$$ LANGUAGE plpgsql;

/**
 * During UPDATE if acl_record already exists, delete the acl_record which is being modified to existing acl_record
 * and update it's acl_record_dependency records.
 */
CREATE RULE ignore_update_duplicate AS ON UPDATE TO acl_record
WHERE EXISTS(SELECT 1 FROM acl_record WHERE (user_id, entity_id, entity_type, role) = (NEW.user_id, NEW.entity_id, NEW.entity_type, NEW.role))
DO INSTEAD (
    SELECT alc_record_change_id(
        (SELECT id FROM acl_record WHERE (user_id, entity_id, entity_type, role) = (OLD.user_id, OLD.entity_id, OLD.entity_type, OLD.role)),
        (SELECT id FROM acl_record WHERE (user_id, entity_id, entity_type, role) = (NEW.user_id, NEW.entity_id, NEW.entity_type, NEW.role))
    );
    DELETE FROM acl_record WHERE (user_id, entity_id, entity_type, role) = (OLD.user_id, OLD.entity_id, OLD.entity_type, OLD.role);
);

/**
 * During UPDATE if acl_record_dependency already exists, delete the acl_record_dependency which is being modified
 * to existing acl_record_dependency.
 */
CREATE RULE ignore_update_duplicate AS ON UPDATE TO acl_record_dependency
WHERE EXISTS(SELECT 1 FROM acl_record_dependency WHERE (parent_acl_record_id, child_acl_record_id) = (NEW.parent_acl_record_id, NEW.child_acl_record_id))
DO INSTEAD (
  DELETE FROM acl_record_dependency WHERE (parent_acl_record_id, child_acl_record_id) = (OLD.parent_acl_record_id, OLD.child_acl_record_id);
);

[perform]

/**
 * Cleanup.
 */
DROP RULE ignore_update_duplicate ON acl_record_dependency;
DROP RULE ignore_update_duplicate ON acl_record;
DROP FUNCTION alc_record_change_id(bigint, bigint);