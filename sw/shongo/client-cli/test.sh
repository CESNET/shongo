#! /bin/sh

DATE=$(date --date='tomorrow' +%Y-%m-%d)

./client-cli.sh --connect localhost --testing-access-token \
--cmd "\
    create-resource -confirm { \
        class: 'DeviceResource', \
        name: 'mcu-cesnet', \
        allocatable: 1, \
        maximumFuture: 'P4M', \
        technologies: ['H323'], \
        mode: { \
            connectorAgentName: 'mcu-cesnet' \
        }, \
        capabilities: [{ \
            class: 'VirtualRoomsCapability', \
            portCount: 20 \
        }, { \
            class: 'AliasProviderCapability', \
            technology: 'H323', \
            type: 'E164', \
            patterns: ['95008721[d]'], \
            restrictedToOwnerResource: 1 \
        }] \
    }" \
--cmd "\
    create-resource -confirm { \
        class: 'DeviceResource', \
        name: 'mcu-muni', \
        allocatable: 1, \
        maximumFuture: 'P4M', \
        technologies: ['H323'], \
        mode: { \
            connectorAgentName: 'mcu-muni' \
        }, \
        capabilities: [{ \
            class: 'VirtualRoomsCapability', \
            portCount: 10 \
        }, { \
            class: 'AliasProviderCapability', \
            technology: 'H323', \
            type: 'E164', \
            patterns: ['95008722[d]'], \
            restrictedToOwnerResource: 1 \
        }] \
    }" \
--cmd "\
    create-resource -confirm { \
        class: 'DeviceResource', \
        name: 'c90-sitola', \
        allocatable: 1, \
        technologies: ['H323'], \
        mode: { \
            connectorAgentName: 'c90-sitola' \
        }, \
        capabilities: [{ \
            class: 'StandaloneTerminalCapability', \
            aliases: [{ \
                technology: 'H323', \
                type: 'E164', \
                value: '950081038' \
            }] \
        }] \
    }"

exit 0

./client-cli.sh --connect localhost --testing-access-token \
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