<#--
  -- Template for rendering {@link AllocationFailedNotification}
  -->
<#assign indent = 23>
<#if context.configuration.class.simpleName != 'ParentConfiguration'>
<#if context.reservationRequestUrl??>
${context.message(indent, "reservationRequest.url")}: ${context.formatReservationRequestUrl(notification.reservationRequestId)}
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
</#if>
${context.message(indent, 'allocationFailed.requestedSlot')}: ${context.formatInterval(notification.requestedSlot)}
<#include "target.ftl">
<#if adminReport??>
${context.message(indent, 'allocationFailed.userError')}: ${context.indentNextLines(indent + 2, userError)}
${context.message(indent, 'allocationFailed.reason')}: ${context.indentNextLines(indent + 2, adminReport)}
<#else>
${context.message(indent, 'allocationFailed.reason')}: ${context.indentNextLines(indent + 2, userError)}
</#if>
