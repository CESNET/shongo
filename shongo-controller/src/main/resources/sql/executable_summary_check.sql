/**
 * Get all results not consistent in summary table, compared to view.
 */
SELECT * FROM executable_summary_view
EXCEPT
SELECT * FROM executable_summary