<#--
  -- Template for rendering {@link ReservationNotification} inside the {@link ReservationRequestNotification}
  -->
<#assign indent = 23>
${context.message(indent, "reservation.slot")}: ${context.formatInterval(notification.slot)}
<#if context.timeZone != "UTC">
${context.width(23)}  ${context.formatInterval(notification.slot, "UTC")}
</#if>
<#include "target.ftl">