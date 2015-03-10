<#--
  -- Template for rendering {@link ReservationNotification} inside the {@link ReservationRequestNotification}
  -->
<#assign indent = 23>
${context.message(indent, "reservation.slot")}: ${context.formatInterval(slot)}
<#if context.timeZone != "UTC">
${context.width(indent)}  ${context.formatInterval(slot, "UTC")}
</#if>
<#if (period??) >
${context.message(indent, "reservation.slot.periodicity")}: ${context.message("reservation.slot.periodicity.period")} ${context.formatPeriod(period)} (${context.message("reservation.slot.periodicity.until")}: ${context.formatDate(end)})
</#if>
<#if target.class.simpleName == "Room" && (target.slotBefore?? || target.slotAfter??)>
${context.width(indent)}  (${context.message("target.room.available")}<#rt>
<#if target.slotBefore??> ${context.message("target.room.available.before", context.formatDuration(target.slotBefore))}</#if><#rt>
<#if target.slotBefore?? && target.slotAfter??> ${context.message("target.room.available.and")}</#if><#t>
<#if target.slotAfter??> ${context.message("target.room.available.after", context.formatDuration(target.slotAfter))}</#if><#rt>
)
</#if>
<#include "target.ftl">

<#if (errors??) >
${context.message(indent, "reservationRequest.allocationFailed.for.someSlots")}:
<#list errors?keys as formatedSlot>
    ${context.width(indent, formatedSlot)}
${context.width(indent)}${errors[formatedSlot]}
</#list>
</#if>

<#if (deletedList??) >
${context.message(indent, "reservationRequest.deleted.for.someSlots")}:
    <#list deletedList as deleted>
    ${context.width(indent)}  ${deleted}
    </#list>
</#if>
