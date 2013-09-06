<#--
  -- Template for rendering {@link ReservationRequestNotification}
  -->
<#assign indent = 24>
<#---->
<#if notification.url??>
${context.message(indent, 'reservationRequest.url')}: ${notification.url}
<#else>
${context.message(indent, 'reservationRequest.id')}: ${notification.id}
</#if>
${context.message(indent, 'reservationRequest.updatedAt')}: ${context.formatDateTime(notification.updatedAt)}
${context.message(indent, 'reservationRequest.updatedBy')}: ${context.formatUser(notification.updatedBy)}
<#if notification.description??>
${context.message(indent, 'reservationRequest.description')}: ${notification.description}
</#if>