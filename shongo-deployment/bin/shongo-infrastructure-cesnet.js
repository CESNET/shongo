/**
 * CESNET infrastructures for Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */

/**
 * Existing room names in CESNET devices.
 */
const EXISTING_ROOM_NAMES = [
    // mcuc.cesnet.cz
    "Akutne","BBMRI-CZ","CC","CEITEC CSR - telekonference","CESNET","CESNET2011","CESNET office","CEZ","Content_test","Datova uloziste","DELLIISS","eduID","EInfrastruktura","ELI","EUAsiaGrid","FEL-Recording","Fond rozvoje","FROV JCU","GN3plus-SAB","Gridy","ICASSP","ICRC","IMETE-ICT","IPv6-wg","Klub reditelu","Mneme","Monitoring a konfigurace","Multimedia","MZK","NEAT-FT","NIDV","NRENPC","OIS","OKaVV","OOaP","Opticke site","PHW 602","Podpora_VaVaI","Pokusna","pokustemppstn","Povros","Predstavenstvo","PSaC","PSaC_Akce","Roadmap","RR","ServiceDesk","Sitova identita","Sitova infrastruktura","Sitove aplikace","Skoda Power","TWAREN","UJV REZ","UPa-IC","VSB-CIT","VSB-IT4I",
    // connect.cesnet.cz
    "aai","antispit","athena","budnik","cerit-sc","cinch","cit-sukb-komenskeho","clarin","cuni-cozp","cuni-lfp-ukbh","cuni-natur","cuni-natur-admiral","cvut-fd-decin","cvut-fel-multimediatech","delliss","egi-eb-private","egi-vt-elixir","ei","elf-dilna","elf-intro","encryption_weak_randomness","entanglement_distillation","eudat","eudml","fedcloud","fykos","gembus","inovacentrum","inovacom","ithanet","levyna","lindat-tech","medigrid","metacentrum","mu-adhoc-a","mu-adhoc-b","mu-adhoc-c","mu-adhoc-d","mu-adhoc-e","mu-ff-cit","mu-ff-kisk-uisk","mu-ff-kniharum","mu-ff-kpi","mu-ff-kpi-seminar","mu-ff-simane","mu-ff-zounek","mu-fsps-kineziologie","mu-ics-oss","mu-lf-adhoc-a","mu-lf-adhoc-b","mu-lf-adhoc-c","mu-lf-adhoc-d","mu-lf-adhoc-e","mu-lf-video","mu-sitmu","mu-ukb-cit","mu-ukb-cit2","mu-ukb-kuk","mu-ukb-video","mu-uvt-ovss","mu-uvt-vedeni","odz","osu","parseme-sc","perun","petrus2","pokus","psac","qkd_weak_randomness","r65127795","radiact","rcna-pilsen","rdc","rebok-konzultace","sa7t5","seminar-iptel","shongo","simultaneous_contract_signing","t2t1","t2t2","talnet-a","talnet-cafe","talnet-i","talnet-kurz-1","talnet-kurz-2","talnet-kurz-3","talnet-kurz-4","talnet-nafta","telovych","telovych2","test","testsem","testsem2","uk-is","uk-is-tutorials","uk-lfp-ovavt","uvt-uk","vienna_group","vsb","vsb-test","vtpup","vut","zcu","zcu-kiv-pd"
];

/**
 * @param mode
 * @returns {Array} of resources for given {@code mode}
 */
function getResources(mode) {
    var resources = [];

    // Resource administrators
    var resourceAdministrators = [];
    if (mode == "meetings") {
        resourceAdministrators.push({email: "meetings-announce@cesnet.cz"});
    }
    else if (mode == "shongo-dev") {
        resourceAdministrators.push({userId: 3859}); // srom@cesnet.cz
        resourceAdministrators.push({userId: 13728}); // pavelka@cesnet.cz
    }
    else {
        resourceAdministrators.push({email: "srom.martin@gmail.com"});
    }

    // Naming service
    resources.push({
        type: "value",
        name: "namingService",
        patternPrefix: common.select(mode, {
            "meetings": "ZZ-shongo-",
            "shongo-dev": "shongo-dev",
            "default": "shongo-local"
        })
    });
    var aliasNamePrefix = common.select(mode, {
        "meetings": "",
        "default": "YY-"
    });

    // Connect
    resources.push({
        type: "connect",
        name: "connect1",
        agent: common.select(mode, {
            "meetings": "connect1",
            "default": "connect-test"
        }),
        address: common.select(mode, {
            "meetings": "https://connect.cesnet.cz",
            "default": "https://tconn.cesnet.cz"
        }),
        aliases: {
            namePrefix: aliasNamePrefix
        },
        licenseCount: common.select(mode, {
            "meetings": 100,
            "default": 20
        }),
        administrators: resourceAdministrators
    });

    // MCU1
    resources.push({
        type: "mcu",
        name: "mcu1",
        agent: "mcu1",
        address: "mcuc.cesnet.cz",
        aliases: {
            namePrefix: aliasNamePrefix,
            number: common.select(mode, {
                "meetings": "950087[200:299]",
                "shongo-dev": "950087[050:099]",
                "default": "950087[090:099]"
            })
        },
        licenseCount: common.select(mode, {
            "meetings": 15,
            "default": 10
        }),
        administrators: resourceAdministrators
    });

    // MCU2
    resources.push({
        type: "mcu",
        name: "mcu2",
        agent: "mcu2",
        address: "mcuc2.cesnet.cz",
        aliases: {
            namePrefix: aliasNamePrefix,
            number: common.select(mode, {
                "meetings": "950083[700:750]",
                "shongo-dev": "950083[750:799]",
                "default": "950083[790:799]"
            })
        },
        licenseCount: common.select(mode, {
            "meetings": 15,
            "default": 10
        }),
        administrators: resourceAdministrators
    });

    // MCU3
    resources.push({
        type: "mcu",
        name: "mcu3",
        agent: "mcu3",
        address: "mcuc3.cesnet.cz",
        aliases: {
            namePrefix: aliasNamePrefix,
            number: common.select(mode, {
                "meetings": "950083[800:850]",
                "shongo-dev": "950083[850:899]",
                "default": "950083[890:099]"
            })
        },
        licenseCount: common.select(mode, {
            "meetings": 15,
            "default": 10
        }),
        administrators: resourceAdministrators
    });

    // TCS1
    resources.push({
        type: "tcs",
        name: "tcs1",
        agent: "tcs1",
        address: "rec1.cesnet.cz",
        licenseCount: common.select(mode, {
            "meetings": 5,
            "default": 3
        }),
        administrators: resourceAdministrators
    });

    // TCS2
    resources.push({
        type: "tcs",
        name: "tcs2",
        agent: "tcs2",
        address: "rec2.cesnet.cz",
        licenseCount: common.select(mode, {
            "meetings": 5,
            "default": 3
        }),
        administrators: resourceAdministrators
    });

    return resources;
}

var common = require('./shongo-infrastructure-common');
for ( var mode in {"meetings": null, "shongo-dev": null, "srom-dev": null}) {
    var resources = getResources(mode);
    console.log(mode + ":\n" + common.formatResources(resources), "\n");
}
//common.bookValues("namingService", "existing-room-names", EXISTING_ROOM_NAMES);