#!/usr/bin/sh
CONTROLLER=127.0.0.1:8686

bin/client-cli.sh --connect $CONTROLLER --testing-access-token --scripting <<EOF

    create-reservation-request {
        class: 'ReservationRequest',
        description: 'test',
        purpose: 'SCIENCE',
        slot: '`date +"%Y-%m-%d"`T00:00/P1Y',
        specification: {
            class: 'AliasSpecification',
            technologies: ['ADOBE_CONNECT'],
            aliasTypes: ['ROOM_NAME'],
            value: 'Test Test',
        }
    }

    create-reservation-request {
        class: 'ReservationRequest',
        description: 'test',
        purpose: 'SCIENCE',
        slot: '`date +"%Y-%m-%d"`T12:00/PT1H',
        specification: {
            class: 'RoomSpecification',
            technologies: ['H323', 'SIP'],
            participantCount: 5,
            aliasSpecifications: [{
                aliasTypes: ['ROOM_NAME'],
                value: 'Testing Testing',
            }]
        }
    }

    create-reservation-request {
        class: 'ReservationRequest',
        description: 'test',
        purpose: 'SCIENCE',
        slot: '`date +"%Y-%m-%d"`T12:00/PT1H',
        specification: {
            class: 'CompartmentSpecification',
            specifications: [{
                class: 'ExternalEndpointSetSpecification',
                technologies: ['H323'],
                count: 3
            }]
        }
    }

EOF

echo "Waiting for allocation..."
sleep 2

bin/client-cli.sh --connect $CONTROLLER --testing-access-token --scripting \
--cmd "list-reservation-requests" \
--cmd "get-reservation-request 1" \
--cmd "get-reservation-for-request 1" \
--cmd "get-reservation-request 2" \
--cmd "get-reservation-for-request 2" \
--cmd "get-reservation-request 3" \
--cmd "get-reservation-for-request 3" \
--cmd "list-executables" \
--cmd "get-executable 4" \
--cmd "get-executable 5" \
--cmd "get-executable 6" \
