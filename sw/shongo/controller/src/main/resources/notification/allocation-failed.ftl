<#--
  -- Template for rendering {@link AllocationFailedNotification}
  -->
<#assign indent = 23>
${context.message(indent, 'allocationFailed.requestedSlot')}: ${context.formatInterval(notification.requestedSlot)}
<#include "target.ftl">
${context.message(indent, 'allocationFailed.reason')}: ${context.indentNextLines(indent + 2, notification.reason)}
