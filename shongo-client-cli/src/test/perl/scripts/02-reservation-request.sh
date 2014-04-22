#!/usr/bin/sh
CONTROLLER=127.0.0.1:8686

shongo-deployment/bin/shongo-client-cli.sh --connect $CONTROLLER --scripting <<EOF

    create-reservation-request {
        class: 'ReservationRequest',
        description: 'test',
        purpose: 'SCIENCE',
        slot: '`date +"%Y-%m-%d"`T00:00/`date +"%Y-%m-%d"`T23:59',
        specification: {
            class: 'AliasSetSpecification',
            aliasSpecifications: [{
                class: 'AliasSpecification',
                technologies: ['ADOBE_CONNECT'],
                aliasTypes: ['ROOM_NAME'],
                value: 'Test Alias 1',
            },{
                class: 'AliasSpecification',
                technologies: ['ADOBE_CONNECT'],
                aliasTypes: ['ROOM_NAME'],
                value: 'Test Alias 2',
            },{
                class: 'AliasSpecification',
                technologies: ['ADOBE_CONNECT'],
                aliasTypes: ['ROOM_NAME'],
                value: 'Test Alias 3',
            }]
        }
    }

    create-reservation-request {
        class: 'ReservationRequest',
        description: 'test',
        purpose: 'SCIENCE',
        slot: '`date +"%Y-%m-%d"`T22:00/`date +"%Y-%m-%d"`T23:00',
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
        slot: '`date +"%Y-%m-%d"`T22:00/`date +"%Y-%m-%d"`T23:00',
        specification: {
            class: 'CompartmentSpecification',
            participants: [{
                class: 'ExternalEndpointSetParticipant',
                technologies: ['H323'],
                count: 3
            }]
        }
    }

EOF

echo "Waiting for allocation..."
sleep 2

shongo-deployment/bin/shongo-client-cli.sh --connect $CONTROLLER \
--cmd "list-reservation-requests" \
--cmd "get-reservation-request 1" \
--cmd "get-reservation-for-request 1" \
--cmd "get-reservation-request 2" \
--cmd "get-reservation-for-request 2" \
--cmd "get-reservation-request 3" \
--cmd "get-reservation-for-request 3" \
--cmd "list-executables" \
--cmd "get-executable 1" \
--cmd "get-executable 2" \
