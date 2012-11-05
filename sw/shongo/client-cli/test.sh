#! /bin/sh

DATE=$(date --date='tomorrow' +%Y-%m-%d)

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
            patterns: ['9500872[dd]'], \
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
        class: 'ReservationRequestSet', \
        name: 'demo', \
        purpose: 'SCIENCE', \
        slots: [{ \
            start: '${DATE}T12:00', \
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
--cmd "\
    create-reservation-request -confirm { \
        class: 'ReservationRequest', \
        name: 'demo', \
        purpose: 'SCIENCE', \
        slot: '${DATE}T14:00/PT4M', \
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
        class: 'ReservationRequest', \
        name: 'demo', \
        purpose: 'SCIENCE', \
        slot: '${DATE}T14:00/PT4M', \
        specification : { \
            class: 'VirtualRoomSpecification', \
            technologies: ['H323'], \
            portCount: 5 \
        } \
    }" \