/**
 * Change acl_entry.id in table acl_entry_dependency.
 */
CREATE FUNCTION alc_record_change_id(oldId bigint, newId bigint) RETURNS void AS $$
BEGIN
    UPDATE acl_entry_dependency SET parent_acl_entry_id = newId WHERE parent_acl_entry_id = oldId;
    UPDATE acl_entry_dependency SET child_acl_entry_id = newId WHERE child_acl_entry_id = oldId;
END;
$$ LANGUAGE plpgsql;

/**
 * During UPDATE if acl_entry already exists, delete the acl_entry which is being modified to existing acl_entry
 * and update it's acl_entry_dependency records.
 */
CREATE RULE ignore_update_duplicate AS ON UPDATE TO acl_entry
WHERE EXISTS(SELECT 1 FROM acl_entry WHERE (acl_identity_id, acl_object_identity_id, role) = (NEW.acl_identity_id, NEW.acl_object_identity_id, NEW.role))
DO INSTEAD (
    SELECT alc_record_change_id(
        (SELECT id FROM acl_entry WHERE (acl_identity_id, acl_object_identity_id, role) = (OLD.acl_identity_id, OLD.acl_object_identity_id, OLD.role)),
        (SELECT id FROM acl_entry WHERE (acl_identity_id, acl_object_identity_id, role) = (NEW.acl_identity_id, NEW.acl_object_identity_id, NEW.role))
    );
    DELETE FROM acl_entry WHERE (acl_identity_id, acl_object_identity_id, role) = (OLD.acl_identity_id, OLD.acl_object_identity_id, OLD.role);
);

/**
 * During UPDATE if acl_entry_dependency already exists, delete the acl_entry_dependency which is being modified
 * to existing acl_entry_dependency.
 */
CREATE RULE ignore_update_duplicate AS ON UPDATE TO acl_entry_dependency
WHERE EXISTS(SELECT 1 FROM acl_entry_dependency WHERE (parent_acl_entry_id, child_acl_entry_id) = (NEW.parent_acl_entry_id, NEW.child_acl_entry_id))
DO INSTEAD (
  DELETE FROM acl_entry_dependency WHERE (parent_acl_entry_id, child_acl_entry_id) = (OLD.parent_acl_entry_id, OLD.child_acl_entry_id);
);

[perform]

/**
 * Cleanup.
 */
DROP RULE ignore_update_duplicate ON acl_entry_dependency;
DROP RULE ignore_update_duplicate ON acl_entry;
DROP FUNCTION alc_record_change_id(bigint, bigint);