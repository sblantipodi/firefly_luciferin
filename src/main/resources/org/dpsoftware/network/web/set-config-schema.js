// Declarative schema of the settings page: the sections and their fields (id, type, numeric constraints) used to build the form. Values and
// labels are merged at runtime from the server-provided config/field options.
export const sections = [
    {
        id: 'leds', fields: [
            {id: 'topLed', type: 'number', numeric: true, min: 0},
            {id: 'leftLed', type: 'number', numeric: true, min: 0},
            {id: 'rightLed', type: 'number', numeric: true, min: 0},
            {id: 'bottomLeftLed', type: 'number', numeric: true, min: 0},
            {id: 'bottomRightLed', type: 'number', numeric: true, min: 0},
            {id: 'bottomRowLed', type: 'number', numeric: true, min: 0},
            {id: 'ledStartOffset', type: 'number', numeric: true, min: 0},
            {id: 'orientation', type: 'select'},
            {id: 'groupBy', type: 'number', numeric: true, min: 0},
            {id: 'splitBottomMargin', type: 'text', numeric: false},
            {id: 'grabberAreaTopBottom', type: 'number', numeric: true, min: 0},
            {id: 'grabberSide', type: 'number', numeric: true, min: 0},
            {id: 'gapTypeTopBottom', type: 'text', numeric: false},
            {id: 'gapTypeSide', type: 'text', numeric: false}
        ]
    },
    {
        id: 'mode', fields: [
            {id: 'outputDevice', type: 'text', numeric: false},
            {id: 'baudRate', type: 'select'},
            {id: 'staticGlowWormIp', type: 'text', numeric: false},
            {id: 'desiredFramerate', type: 'select'},
            {id: 'smoothingType', type: 'select'},
            {id: 'smoothingTargetFramerate', type: 'number', numeric: true, min: 0, max: 240},
            {id: 'frameInsertionTarget', type: 'number', numeric: true, min: 0, max: 120},
            {id: 'emaAlpha', type: 'number', numeric: true, step: '0.05', min: 0, max: 1},
            {id: 'simdAvx', type: 'select'},
            {id: 'resamplingFactor', type: 'select'},
            {id: 'captureMethod', type: 'text', numeric: false},
            {id: 'monitorNumber', type: 'number', numeric: true, min: 1, max: 8},
            {id: 'screenResX', type: 'number', numeric: true, min: 0},
            {id: 'screenResY', type: 'number', numeric: true, min: 0},
            {id: 'osScaling', type: 'number', numeric: true, min: 100, max: 500},
            {id: 'defaultLedMatrix', type: 'select'},
            {id: 'autoDetectBlackBars', type: 'checkbox', numeric: false},
            {id: 'algo', type: 'select'},
            {id: 'language', type: 'select'}
        ],
        subAccordions: [
            {
                id: 'display', fields: [
                    {id: 'cubeLut', type: 'select'}
                ]
            }
        ]
    },
    {
        id: 'network', fields: [
            {id: 'mqttEnable', type: 'checkbox', numeric: false},
            {id: 'wirelessStream', type: 'checkbox', numeric: false},
            {id: 'streamType', type: 'select'},
            {id: 'mqttServer', type: 'text', numeric: false},
            {id: 'mqttTopic', type: 'text', numeric: false},
            {id: 'mqttDiscoveryTopic', type: 'text', numeric: false},
            {id: 'mqttUsername', type: 'text', numeric: false},
            {id: 'mqttPwd', type: 'text', numeric: false}
        ]
    },
    {
        id: 'misc', fields: [
            {id: 'effect', type: 'select'},
            {id: 'colorMode', type: 'select'},
            {id: 'gamma', type: 'number', numeric: true, step: '0.1', min: 0, max: 4},
            {id: 'whiteTemperature', type: 'number', numeric: true, min: 0, max: 30000},
            {id: 'brightness', type: 'number', numeric: true, min: 0, max: 100},
            {id: 'nightModeFrom', type: 'text', numeric: false},
            {id: 'nightModeTo', type: 'text', numeric: false},
            {id: 'nightModeBrightness', type: 'text', numeric: false},
            {id: 'toggleLed', type: 'checkbox', numeric: false},
            {id: 'startWithSystem', type: 'checkbox', numeric: false},
            {id: 'runtimeLogLevel', type: 'text', numeric: false}
        ],
        subAccordions: [
            {
                id: 'colorCorr', fields: [
                    {
                        id: 'ccInfo',
                        type: 'note',
                        note: 'Exposed via the hueMap field, currently not available from the web API (excluded by the server). Manage it from the JavaFX interface.'
                    }
                ]
            },
            {
                id: 'eyeCare', fields: [
                    {id: 'nightLight', type: 'select'},
                    {id: 'nightLightLvl', type: 'number', numeric: true, min: 1, max: 100},
                    {id: 'luminosityThreshold', type: 'number', numeric: true, min: 0},
                    {id: 'brightnessLimiter', type: 'select'}
                ]
            },
            {
                id: 'gamma', fields: [
                    {id: 'enableAutomaticGamma', type: 'checkbox', numeric: false},
                    {id: 'gammaLevel', type: 'select'}
                ]
            },
            {
                id: 'profile', fields: [
                    {id: 'checkFullScreen', type: 'checkbox', numeric: false},
                    {id: 'gpuThreshold', type: 'number', numeric: true, min: 0, max: 100},
                    {id: 'cpuThreshold', type: 'number', numeric: true, min: 0, max: 100},
                    {id: 'profileProcess1', type: 'text', numeric: false, list: 'profileProcesses', index: 0},
                    {id: 'profileProcess2', type: 'text', numeric: false, list: 'profileProcesses', index: 1},
                    {id: 'profileProcess3', type: 'text', numeric: false, list: 'profileProcesses', index: 2}
                ]
            },
            {
                id: 'smoothing', fields: [
                    {id: 'smoothingTargetFramerate', type: 'number', numeric: true, min: 0, max: 240},
                    {
                        id: 'smoothingNote',
                        type: 'note',
                        note: 'EMA alpha, frame insertion and smoothing type are derived from the target and managed by the app.'
                    }
                ]
            }
        ]
    },
    {
        id: 'devices', fields: [
            {id: 'powerSaving', type: 'select'},
            {id: 'multiMonitor', type: 'select'},
            {id: 'multiScreenSingleDevice', type: 'checkbox', numeric: false},
            {id: 'checkForUpdates', type: 'checkbox', numeric: false},
            {id: 'syncCheck', type: 'checkbox', numeric: false}
        ],
        subAccordions: [
            {
                id: 'connectedDevices', fields: [
                    {id: 'devicesContent', type: 'note', note: 'Loading devices…'}
                ]
            },
            {
                id: 'satellites', fields: [
                    {
                        id: 'satInfo',
                        type: 'note',
                        note: 'Managed via the satellites map, currently not available from the web API (excluded by the server). Manage it from the JavaFX interface.'
                    }
                ]
            }
        ]
    },
    {
        id: 'ldr', fields: [
            {id: 'enableLDR', type: 'checkbox', numeric: false},
            {id: 'ldrInterval', type: 'number', numeric: true, min: 0},
            {id: 'ldrMin', type: 'number', numeric: true, min: 0},
            {id: 'ldrTurnOff', type: 'checkbox', numeric: false}
        ]
    }
];
