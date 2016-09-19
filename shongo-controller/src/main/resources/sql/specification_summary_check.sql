/**
 * Get all results not consistent in summary table, compared to view.
 */
SELECT * FROM specification_summary_view
EXCEPT
SELECT * FROM specification_summary