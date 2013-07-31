DROP VIEW IF EXISTS specification_summary;
DROP VIEW IF EXISTS alias_specification_summary;

/**
 * View of specification summaries for aliases or sets of aliases.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */   
CREATE VIEW alias_specification_summary AS
SELECT 
    DISTINCT ON(specification.id)
    specification.id,
    alias_specification.value as room_name
FROM specification
LEFT JOIN alias_set_specification_alias_specifications AS child_alias_specification ON child_alias_specification.alias_set_specification_id = specification.id
LEFT JOIN alias_specification ON alias_specification.id = specification.id OR alias_specification.id = child_alias_specification.alias_specification_id
LEFT JOIN alias_specification_alias_types AS types ON types.alias_specification_id = alias_specification.id
WHERE types.alias_types = 'ROOM_NAME'
ORDER BY specification.id;

/**
 * View of specification summaries.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */   
CREATE VIEW specification_summary AS
SELECT     
    specification.id AS id,
    string_agg(specification_technologies.technologies, ', ') AS technologies,
    CASE 
    WHEN room_specification.id IS NOT NULL THEN 'ROOM'
    WHEN alias_specification_summary.id IS NOT NULL THEN 'ALIAS'
    WHEN resource_specification.id IS NOT NULL THEN 'RESOURCE'
    ELSE 'OTHER'
    END AS type,
    alias_specification_summary.room_name AS alias_room_name,
    room_specification.participant_count AS room_participant_count,
    resource_specification.resource_id AS resource_id
FROM specification
LEFT JOIN specification_technologies ON specification_technologies.specification_id = specification.id
LEFT JOIN room_specification ON room_specification.id = specification.id
LEFT JOIN resource_specification ON resource_specification.id = specification.id
LEFT JOIN alias_specification_summary ON alias_specification_summary.id = specification.id
GROUP BY 
    specification.id,    
    alias_specification_summary.id,    
    alias_specification_summary.room_name,
    room_specification.id,
    room_specification.participant_count,
    resource_specification.id