#!/usr/bin/sh
CONTROLLER=127.0.0.1:8686

bin/client-cli.sh --connect $CONTROLLER --testing-access-token --scripting <<EOF

    create-reservation-request {
        class: 'ReservationRequest',
        description: 'test',
        purpose: 'SCIENCE',
        slot: '`date +"%Y-%m-%d"`T00:00/`date +"%Y-%m-%d"`T23:59',
        specification: {
            class: 'AliasSetSpecification',
            sharedExecutable: 1,
            aliases: [{
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
        slot: '`date +"%Y-%m-%d"`T12:00/`date +"%Y-%m-%d"`T13:00',
        specification: {
            class: 'RoomSpecification',
            technologies: ['H323', 'SIP'],
            participantCount: 5,
            aliases: [{
                aliasTypes: ['ROOM_NAME'],
                value: 'Testing Testing',
            }]
        }
    }

    create-reservation-request {
        class: 'ReservationRequest',
        description: 'test',
        purpose: 'SCIENCE',
        slot: '`date +"%Y-%m-%d"`T12:00/`date +"%Y-%m-%d"`T13:00',
        specification: {
            class: 'CompartmentSpecification',
            specifications: [{
                class: 'ExternalEndpointSetSpecification',
                technologies: ['H323'],
                count: 3
            }]
        }
    }

    create-reservation-request {
        class: 'ReservationRequest',
        description: 'test',
        purpose: 'SCIENCE',
        slot: '`date +"%Y-%m-%d"`T00:00/`date +"%Y-%m-%d"`T23:59',
        specification: {
            class: 'AliasSetSpecification',
            aliases: [{
                class: 'AliasSpecification',
                technologies: ['ADOBE_CONNECT'],
                aliasTypes: ['ROOM_NAME'],
                value: 'Test Room 1',
            },{
                class: 'AliasSpecification',
                technologies: ['ADOBE_CONNECT'],
                aliasTypes: ['ROOM_NAME'],
                value: 'Test Room 2',
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
--cmd "get-reservation-request 4" \
--cmd "get-reservation-for-request 4" \
--cmd "list-executables" \
--cmd "get-executable 5" \
--cmd "get-executable 6" \
--cmd "get-executable 7" \
--cmd "get-executable 11" \
--cmd "get-executable 12" \
