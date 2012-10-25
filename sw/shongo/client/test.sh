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
--cmd "list-resources"