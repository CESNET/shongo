<#--
  -- Template for rendering {@link ReservationRequestNotification}
  -->
${context.message(10, 'reservationRequest.url')}: ${notification.url}
${context.message(10, 'reservationRequest.updatedAt')}: ${context.formatDateTime(notification.updatedAt)}
${context.message(10, 'reservationRequest.updatedBy')}: ${context.formatUser(notification.updatedBy)}
${context.message(10, 'reservationRequest.description')}: ${notification.description}