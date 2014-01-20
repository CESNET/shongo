<#--
  -- Template for rendering {@link ReservationNotification} inside the {@link ReservationRequestNotification}
  -->
<#assign indent = 23>
${context.message(indent, "reservation.slot")}: ${context.formatInterval(slot)}
<#if context.timeZone != "UTC">
${context.width(indent)}  ${context.formatInterval(slot, "UTC")}
</#if>
<#if target.class.simpleName == "Room">
${context.width(indent)}  ${context.message("target.room.available", context.formatDuration(target.slotBefore), context.formatDuration(target.slotAfter))}
</#if>
<#include "target.ftl">