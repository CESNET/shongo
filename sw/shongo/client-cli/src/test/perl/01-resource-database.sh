#!/usr/bin/sh

./client-cli.sh --connect 127.0.0.1:8686 --testing-access-token --scripting \
--cmd "\
    create-resource { \
        class: 'DeviceResource', \
        name: 'mcu-cesnet', \
        allocatable: 1, \
        maximumFuture: 'P4M', \
        technologies: ['H323','SIP'], \
        mode: { \
            connectorAgentName: 'mcu-cesnet' \
        }, \
        capabilities: [{ \
            class: 'RoomProviderCapability', \
            licenseCount: 20 \
        }, { \
            class: 'AliasProviderCapability', \
            aliases: [ \
                { type: 'H323_E164', value: '{value}' }, \
                { type: 'SIP_URI', value: '{value}@cesnet.cz' } \
            ], \
            patterns: ['95008721[d]'], \
            restrictedToOwnerResource: 1 \
        }] \
    }" \
--cmd "\
    create-resource { \
        class: 'DeviceResource', \
        name: 'mcu-muni', \
        allocatable: 1, \
        maximumFuture: 'P4M', \
        technologies: ['H323','SIP'], \
        mode: { \
            connectorAgentName: 'mcu-muni' \
        }, \
        capabilities: [{ \
            class: 'RoomProviderCapability', \
            licenseCount: 10 \
        }, { \
            class: 'AliasProviderCapability', \
            aliases: [ \
                { type: 'H323_E164', value: '{value}' }, \
                { type: 'SIP_URI', value: '{value}@cesnet.cz' } \
            ], \
            patterns: ['95008722[d]'], \
            restrictedToOwnerResource: 1 \
        }] \
    }" \
--cmd "\
    create-resource { \
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
                type: 'H323_E164', \
                value: '950081038' \
            }] \
        }] \
    }" \

./client-cli.sh --connect 127.0.0.1:8686 --testing-access-token --scripting \
--cmd "list-resources" \
--cmd "get-resource 1" \
--cmd "get-resource 2" \
--cmd "get-resource 3" \
--cmd "get-resource-allocation 1" \
--cmd "get-resource-allocation 2" \
--cmd "get-resource-allocation 3" \
