#! /bin/sh

./client.sh --connect localhost --testing-access-token \
--cmd "\
    modify-resource -confirm { \
        identifier: '1', \
        name: 'mcu', \
    }" \
--cmd "get-resource 1"

./client.sh --connect localhost --testing-access-token \
--cmd "\
    modify-reservation-request -confirm { \
        identifier: '1', \
        name: 'testxxx', \
    }" \
--cmd "get-reservation-request 1"

exit 0

./client.sh --connect localhost --testing-access-token \
--cmd "\
    create-resource -confirm { \
        class: 'DeviceResource', \
        name: 'mcu', \
        allocatable: 1, \
        technologies: ['H323'], \
        mode: { \
            connectorAgentName: 'mcu' \
        }, \
        capabilities: [{ \
            class: 'VirtualRoomsCapability', \
            portCount: 100 \
        }, { \
            class: 'AliasProviderCapability', \
            technology: 'H323', \
            type: 'E164', \
            pattern: '9500872[dd]', \
            restrictedToOwnerResource: 1 \
        }] \
    }" \
--cmd "\
    create-resource -confirm { \
        class: 'DeviceResource', \
        name: 'c90', \
        allocatable: 1, \
        technologies: ['H323'], \
        mode: { \
            connectorAgentName: 'c90' \
        }, \
        capabilities: [{ \
            class: 'StandaloneTerminalCapability' \
        }] \
    }" \
--cmd "\
    create-reservation-request -confirm { \
        class: 'PermanentReservationRequest', \
        name: 'test', \
        resourceIdentifier: 'shongo:cz.cesnet:1', \
        slots: [{ \
            start: '2012-01-01T12:00', \
            duration: 'PT4M' \
        }] \
    }" \
--cmd "\
    create-reservation-request -confirm { \
        class: 'ReservationRequest', \
        name: 'demo', \
        purpose: 'SCIENCE', \
        slot: '2012-01-01T12:00/PT4M', \
        specification : { \
            class: 'CompartmentSpecification', \
            specifications: [{ \
                class: 'ExternalEndpointSetSpecification', \
                technology: 'H323', \
                count: 5 \
            }] \
        } \
    }" \
--cmd "\
    create-reservation-request -confirm { \
        class: 'ReservationRequestSet', \
        name: 'demo', \
        purpose: 'SCIENCE', \
        slots: [{ \
            start: '2012-01-01T12:00', \
            duration: 'PT4M' \
        }], \
        specifications: [{ \
            class: 'CompartmentSpecification', \
            specifications: [{ \
                class: 'ExternalEndpointSetSpecification', \
                technology: 'H323', \
                count: 5 \
            }] \
        }] \
    }" \