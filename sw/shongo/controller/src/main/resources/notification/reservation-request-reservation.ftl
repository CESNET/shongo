<#--
  -- Template for rendering {@link ReservationNotification} inside the {@link ReservationRequestNotification}
  -->
<#assign indent = 23>
${context.message(indent, "reservation.slot")}: ${context.formatInterval(slot)}
<#if context.timeZone != "UTC">
${context.width(indent)}  ${context.formatInterval(slot, "UTC")}
</#if>
<#if target.class.simpleName == "Room" && (target.slotBefore?? || target.slotAfter??)>
${context.width(indent)}  (${context.message("target.room.available")}<#rt>
<#if target.slotBefore??> ${context.message("target.room.available.before", context.formatDuration(target.slotBefore))}</#if><#rt>
<#if target.slotBefore?? && target.slotAfter??> ${context.message("target.room.available.and")}</#if><#t>
<#if target.slotAfter??> ${context.message("target.room.available.after", context.formatDuration(target.slotAfter))}</#if><#rt>
)
</#if>
<#include "target.ftl">