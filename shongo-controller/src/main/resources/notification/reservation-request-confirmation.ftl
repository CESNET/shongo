<#--
  -- Template for rendering {@link ReservationRequestConfirmationNotification}
  -->
${context.message("reservationRequestConfirmation.message")}

<#assign indent = 23>
<#if context.reservationRequestConfirmationUrl??>
${context.message(indent, "reservationRequest.url")}: <a href="${context.formatReservationRequestConfirmationUrl(target.resourceId, notification.requestedSlot)}">${context.formatReservationRequestConfirmationUrl(target.resourceId, notification.requestedSlot)}</a>
<#elseif notification.reservationRequestId??>
${context.message(indent, "reservationRequest.id")}: ${notification.reservationRequestId}
</#if>
<#if notification.reservationRequestUpdatedAt??>
${context.message(indent, "reservationRequest.updatedAt")}: ${context.formatDateTime(notification.reservationRequestUpdatedAt)}
${context.message(indent, "reservationRequest.updatedBy")}: ${context.formatUser(notification.reservationRequestUpdatedBy)}
</#if>
<#if notification.reservationRequestDescription??>
${context.message(indent, "reservationRequest.description")}: ${context.indentNextLines(indent + 2, notification.reservationRequestDescription)}
</#if>
${context.message(indent, 'reservationRequest.requestedSlot')}: ${context.formatInterval(notification.requestedSlot)}
<#if context.timeZone != "UTC">
${context.width(indent)}  ${context.formatInterval(notification.requestedSlot, "UTC")}
</#if>
<#include "target.ftl">