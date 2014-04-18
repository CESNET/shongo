cd `dirname $0`/../
#
# Create testing data
#

CONTROLLER=127.0.0.1
RUN_CLIENT_CLI="./shongo_client_cli.sh --connect $CONTROLLER --scripting"

$RUN_CLIENT_CLI <<EOF

    \${id} = create-reservation-request {
        class: 'ReservationRequestSet',
        purpose: 'OWNER',
        slots: [
            '2013-07-15T12:00/2013-07-15T14:00',
            '2013-08-15T12:00/2013-08-15T14:00'
        ],
        description: 'Alias in MCU',
        specification: {
            class: 'RoomSpecification',
            technologies: ['ADOBE_CONNECT'],
            participantCount: 3
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequestSet',
        purpose: 'OWNER',
        slots: [
            '2013-07-15T12:00/2013-07-15T14:00',
            '2013-07-31T08:00/2013-07-31T15:00',
            '2013-08-15T12:00/2013-08-15T14:00'
        ],
        description: 'Alias in MCU',
        specification: {
            class: 'RoomSpecification',
            technologies: ['ADOBE_CONNECT'],
            participantCount: 3
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequestSet',
        purpose: 'OWNER',
        slots: [
            '2013-07-15T12:00/2013-07-15T14:00',
            '2013-07-20T12:00/2013-07-20T14:00'
        ],
        description: 'Alias in MCU',
        specification: {
            class: 'RoomSpecification',
            technologies: ['ADOBE_CONNECT'],
            participantCount: 3
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'Alias in MCU',
        specification: {
            class: 'AliasSetSpecification',
            aliasSpecifications: [
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'mcu-test' },
                { aliasTypes: ['H323_E164'] }
            ]
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'Alias in Adobe Connect',
        specification: {
            class: 'AliasSetSpecification',
            aliasSpecifications: [
                { technologies: ['ADOBE_CONNECT'], aliasTypes: ['ROOM_NAME'], value: 'connect-test' },
            ]
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'Room in MCU',
        specification: {
            class: 'RoomSpecification',
            technologies: ['H323'],
            participantCount: 5
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'Room in Adobe Connect',
        specification: {
            class: 'RoomSpecification',
            technologies: ['ADOBE_CONNECT'],
            participantCount: 3
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'test alias 1',
        specification: {
            class: 'AliasSetSpecification',
            aliasSpecifications: [
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'alias 1' },
            ]
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'test alias 2',
        specification: {
            class: 'AliasSetSpecification',
            aliasSpecifications: [
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'alias 2' },
            ]
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'test alias 3',
        specification: {
            class: 'AliasSetSpecification',
            aliasSpecifications: [
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'alias 3' },
            ]
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'test alias 4',
        specification: {
            class: 'AliasSetSpecification',
            aliasSpecifications: [
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'alias 4' },
            ]
        }
    }

    create-acl 51 \${id} OWNER

    \${id} = create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '2013-01-01T12:00/2013-01-01T14:00',
        description: 'test alias 5',
        specification: {
            class: 'AliasSetSpecification',
            aliasSpecifications: [
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'alias 5' },
            ]
        }
    }

    create-acl 51 \${id} OWNER

EOF