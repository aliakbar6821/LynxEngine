package com.lynxengine.app.data

/**
 * One game entry stored in lynx_game_data (Settings.Secure).
 * All 6 Build fields are stored so LynxGameHooks can override them
 * without any extra lookup.
 */
data class GameEntry(
    val packageName: String,
    val appName: String,
    val mode: String,          // "auto" | "manual"
    val profileName: String,   // display label, e.g. "ASUS ROG Phone 8 Pro"
    val brand: String,
    val device: String,
    val manufacturer: String,
    val model: String,
    val fingerprint: String,
    val product: String
)

/**
 * All device profiles hardcoded in the app binary.
 * Not stored in Settings, GitHub, or any user-visible location.
 * Source: LynxEngine device JSON (31 entries).
 */
object DeviceProfiles {
    val ALL: List<GameDeviceProfile> = listOf(
        GameDeviceProfile(
            name         = "ASUS ROG Phone 8 Pro",
            brand        = "asus",
            device       = "ROG Phone 8 Pro",
            manufacturer = "asus",
            model        = "ASUS_AI2401",
            fingerprint  = "asus/WW_AI2401/ASUS_AI2401:14/UKQ1.231003.002/35.1210.1420.78-0-0:user/release-keys",
            product      = "ASUS_AI2401"
        ),
        GameDeviceProfile(
            name         = "Asus ROG Phone 9",
            brand        = "asus",
            device       = "ROG Phone 9",
            manufacturer = "asus",
            model        = "ASUSAI2501",
            fingerprint  = "asus/WWAI2501/ASUSAI2501:15/AQ3A.240829.003/35.1810.1810.346-0:user/release-keys",
            product      = "ASUSAI2501"
        ),
        GameDeviceProfile(
            name         = "ROG Phone 5s",
            brand        = "ASUS",
            device       = "ROG Phone 5s",
            manufacturer = "ASUS",
            model        = "ZS676KS",
            fingerprint  = "ASUS/ZS676KS/ROG Phone 5s:12/SKQ1.210821.001/20220810:user/release-keys",
            product      = "ZS676KS"
        ),
        GameDeviceProfile(
            name         = "ROG Phone 6D Ultimate",
            brand        = "ASUS",
            device       = "ROG Phone 6D Ultimate",
            manufacturer = "ASUS",
            model        = "AI2203",
            fingerprint  = "ASUS/AI2203/ROG Phone 6D:14/UP1A.231005.007/20240315:user/release-keys",
            product      = "AI2203"
        ),
        GameDeviceProfile(
            name         = "Black Shark 4",
            brand        = "Black Shark",
            device       = "Xiaomi Black Shark 4",
            manufacturer = "Xiaomi",
            model        = "2SM-X706B",
            fingerprint  = "BlackShark/PRS-H0/Black Shark 4:13/TQ3A.230805.001/20230315:user/release-keys",
            product      = "2SM-X706B"
        ),
        GameDeviceProfile(
            name         = "iQOO 12 Pro",
            brand        = "iQOO",
            device       = "iQOO 12 Pro",
            manufacturer = "vivo",
            model        = "I2401",
            fingerprint  = "iQOO/I2401/I2401:15/AP3A.240905.015.A2/compiler250328151630:user/release-keys",
            product      = "I2401"
        ),
        GameDeviceProfile(
            name         = "Lenovo Y700 (Gaming Tablet)",
            brand        = "Lenovo",
            device       = "Lenovo Legion Y700",
            manufacturer = "lenovo",
            model        = "Lenovo TB-9707F",
            fingerprint  = "Lenovo/TB-9707F_PRC/TB-9707F:13/TKQ1.221013.002/15.0.342_231018:user/release-keys",
            product      = "TB-9707F"
        ),
        GameDeviceProfile(
            name         = "RedMagic 9 Pro",
            brand        = "nubia",
            device       = "REDMAGIC 9 Pro",
            manufacturer = "ZTE",
            model        = "NX769J",
            fingerprint  = "nubia/NX769J/NX769J:14/UKQ1.230917.001/20240813.173312:user/release-keys",
            product      = "NX769J"
        ),
        GameDeviceProfile(
            name         = "RedMagic 10 Pro",
            brand        = "nubia",
            device       = "RedMagic 10 Pro",
            manufacturer = "ZTE",
            model        = "NX789J",
            fingerprint  = "nubia/NX789J-UN/NX789J:15/AQ3A.240812.002/20241212.194919:user/release-keys",
            product      = "NX789J"
        ),
        GameDeviceProfile(
            name         = "RedMagic Tablet 3",
            brand        = "NUBIA",
            device       = "NP05J",
            manufacturer = "NUBIA",
            model        = "NP05J",
            fingerprint  = "nubia/NP05J/NP05J:15/AQ3A.240812.002/20250101.000000:user/release-keys",
            product      = "NP05J"
        ),
        GameDeviceProfile(
            name         = "OnePlus 11",
            brand        = "OnePlus",
            device       = "OnePlus 11",
            manufacturer = "OnePlus",
            model        = "OP5566L1",
            fingerprint  = "OnePlus/CPH2411/OP5566L1:12/SP1A.210812.016/S.202208251855:user/release-keys",
            product      = "OP5566L1"
        ),
        GameDeviceProfile(
            name         = "OnePlus 13",
            brand        = "OnePlus",
            device       = "OnePlus 13",
            manufacturer = "OnePlus",
            model        = "PJD110",
            fingerprint  = "OnePlus/PJD110/OP5D0DL1:15/AP3A.240617.008/V.1bd19a1-1-2:user/release-keys",
            product      = "PJD110"
        ),
        GameDeviceProfile(
            name         = "OnePlus 15",
            brand        = "OnePlus",
            device       = "OP60FFL1",
            manufacturer = "OnePlus",
            model        = "PLK110",
            fingerprint  = "OnePlus/PLK110/OP60FFL1:15/AP3A.240905.015/V.latest:user/release-keys",
            product      = "PLK110"
        ),
        GameDeviceProfile(
            name         = "OnePlus 8 Pro 5G",
            brand        = "OnePlus",
            device       = "OnePlus 8 Pro 5G",
            manufacturer = "OnePlus",
            model        = "IN2023",
            fingerprint  = "OnePlus/IN2023/OnePlus8Pro:13/RKQ1.211119.001/20230501:user/release-keys",
            product      = "IN2023"
        ),
        GameDeviceProfile(
            name         = "OnePlus 9 Pro 5G",
            brand        = "OnePlus",
            device       = "OnePlus 9 Pro 5G",
            manufacturer = "OnePlus",
            model        = "LE2123",
            fingerprint  = "OnePlus/LE2123/OnePlus9Pro:14/UP1A.231005.007/20240115:user/release-keys",
            product      = "LE2123"
        ),
        GameDeviceProfile(
            name         = "POCO F5",
            brand        = "POCO",
            device       = "POCO F5",
            manufacturer = "Xiaomi",
            model        = "23049PCD8G",
            fingerprint  = "POCO/23049PCD8G/POCOF5:13/TQ3A.230805.001/20230620:user/release-keys",
            product      = "23049PCD8G"
        ),
        GameDeviceProfile(
            name         = "Realme P3 5G",
            brand        = "realme",
            device       = "Realme P3 5G",
            manufacturer = "realme",
            model        = "RMX5070",
            fingerprint  = "realme/kbi/RE58B4L1:14/UKQ1.230804.001/1715073230894:user/release-keys",
            product      = "RMX5070"
        ),
        GameDeviceProfile(
            name         = "Realme 15 Pro 5G",
            brand        = "realme",
            device       = "Realme 15 Pro 5G",
            manufacturer = "realme",
            model        = "RMX5101",
            fingerprint  = "realme/RMX5101IN/RE60B4L1:15/AP3A.240617.008/V.R4T2.26cec0e-80bb4e-80b757:user/release-keys",
            product      = "RMX5101"
        ),
        GameDeviceProfile(
            name         = "Samsung Galaxy S23 Ultra",
            brand        = "samsung",
            device       = "Samsung Galaxy S23 Ultra",
            manufacturer = "Samsung",
            model        = "SM-S918B",
            fingerprint  = "samsung/dm3q/dm3q:14/UP1A.231005.007/123456:user/release-keys",
            product      = "dm3qxx"
        ),
        GameDeviceProfile(
            name         = "Samsung Galaxy S24 Ultra",
            brand        = "Samsung",
            device       = "Galaxy S24 Ultra",
            manufacturer = "Samsung",
            model        = "SM-S928B",
            fingerprint  = "samsung/SM-S928B/SM-S928B:14/UP1A.231005.007/20240225:user/release-keys",
            product      = "SM-S928B"
        ),
        GameDeviceProfile(
            name         = "Samsung Galaxy S25 Ultra",
            brand        = "samsung",
            device       = "Samsung Galaxy S25 Ultra",
            manufacturer = "samsung",
            model        = "SM-S938B",
            fingerprint  = "samsung/pa3qxxx/pa3q:15/AP3A.240905.015.A2/S938BXXU1AYB4:user/release-keys",
            product      = "pa3qxxx"
        ),
        GameDeviceProfile(
            name         = "Xiaomi 11T Pro",
            brand        = "Xiaomi",
            device       = "Xiaomi 11T Pro",
            manufacturer = "Xiaomi",
            model        = "2107113SG",
            fingerprint  = "Xiaomi/2107113SI/Mi 11T Pro:13/RKQ1.211001.001/20230410:user/release-keys",
            product      = "2107113SG"
        ),
        GameDeviceProfile(
            name         = "Xiaomi 13",
            brand        = "Xiaomi",
            device       = "Xiaomi 13",
            manufacturer = "Xiaomi",
            model        = "2211133G",
            fingerprint  = "Xiaomi/fuxi_eea/fuxi:13/TKQ1.221114.001/OS2.0.102.0.VMCEUXM:user/release-keys",
            product      = "fuxi"
        ),
        GameDeviceProfile(
            name         = "Xiaomi 13 Pro",
            brand        = "Xiaomi",
            device       = "Xiaomi 13 Pro",
            manufacturer = "Xiaomi",
            model        = "2210132G",
            fingerprint  = "Xiaomi/nuwa_global/nuwa:13/TKQ1.221114.001/OS2.0.100.0.VMBMIXM:user/release-keys",
            product      = "nuwa"
        ),
        GameDeviceProfile(
            name         = "Xiaomi 14 Pro",
            brand        = "Xiaomi",
            device       = "Xiaomi 14 Pro",
            manufacturer = "Xiaomi",
            model        = "23116PN5BC",
            fingerprint  = "Xiaomi/shennong/shennong:15/AQ3A.240627.003/OS2.0.202.0.VNBCNXM:user/release-keys",
            product      = "shennong"
        ),
        GameDeviceProfile(
            name         = "Xiaomi 14 Ultra",
            brand        = "Xiaomi",
            device       = "Xiaomi 14 Ultra",
            manufacturer = "Xiaomi",
            model        = "24030PN60G",
            fingerprint  = "Xiaomi/aurora_eea/aurora:14/UKQ1.240523.001/OS2.0.102.0.VNAEUXM:user/release-keys",
            product      = "aurora"
        ),
        GameDeviceProfile(
            name         = "Xiaomi 15 Ultra",
            brand        = "Xiaomi",
            device       = "Xiaomi 15 Ultra",
            manufacturer = "Xiaomi",
            model        = "25010PN30G",
            fingerprint  = "Xiaomi/xuanyuan_eea/xuanyuan:15/AQ3A.240912.001/OS2.0.10.0.VOAEUXM:user/release-keys",
            product      = "xuanyuan"
        ),
        GameDeviceProfile(
            name         = "Google Pixel 8 Pro",
            brand        = "google",
            device       = "husky",
            manufacturer = "Google",
            model        = "Pixel 8 Pro",
            fingerprint  = "google/husky/husky:15/BP1A.250405.005.B1/13151136:user/release-keys",
            product      = "husky"
        ),
        GameDeviceProfile(
            name         = "Google Pixel 9 Pro XL",
            brand        = "google",
            device       = "komodo",
            manufacturer = "Google",
            model        = "Pixel 9 Pro XL",
            fingerprint  = "google/komodo/komodo:15/BP1A.250405.005.A1/13151424:user/release-keys",
            product      = "komodo"
        ),
        GameDeviceProfile(
            name         = "Google Pixel XL",
            brand        = "google",
            device       = "Pixel XL",
            manufacturer = "Google",
            model        = "marlin",
            fingerprint  = "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys",
            product      = "marlin"
        ),
        GameDeviceProfile(
            name         = "iQOO 15",
            brand        = "VIVO",
            device       = "V2505A",
            manufacturer = "VIVO",
            model        = "V2505A",
            fingerprint  = "vivo/V2505A/V2505A:15/AP3A.240905.015/compiler250101:user/release-keys",
            product      = "V2505A"
        ),
    )
}

data class GameDeviceProfile(
    val name: String,
    val brand: String,
    val device: String,
    val manufacturer: String,
    val model: String,
    val fingerprint: String,
    val product: String
)

/**
 * Per-game recommended profile names — sourced from Clover/EvolutionX GamesPropsUtils research.
 * Key   = package name
 * Value = name that matches a DeviceProfiles.ALL entry (profile.name)
 *
 * These are the exact device whitelists that each game checks internally.
 * Picking ANY other profile may still work, but these are confirmed.
 */
object RecommendedProfiles {

    val MAP: Map<String, String> = mapOf(

        // ── PUBG Mobile (all regions) → Galaxy S24 Ultra (SM-S928B) ────────
        "com.tencent.ig"              to "Samsung Galaxy S24 Ultra",
        "com.pubg.imobile"            to "Samsung Galaxy S24 Ultra",
        "com.pubg.krmobile"           to "Samsung Galaxy S24 Ultra",
        "com.rekoo.pubgm"             to "Samsung Galaxy S24 Ultra",
        "com.tencent.tmgp.pubgmhd"   to "Samsung Galaxy S24 Ultra",
        "com.vng.pubgmobile"          to "Samsung Galaxy S24 Ultra",
        "com.blizzard.diablo.immortal" to "Samsung Galaxy S24 Ultra",
        "com.kitkagames.fallbuddies"  to "Samsung Galaxy S24 Ultra",

        // ── Call of Duty Mobile (all regions) → Lenovo Y700 ─────────────────
        // Unlocks 120fps in Battle Royale mode
        "com.activision.callofduty.shooter" to "Lenovo Y700 (Gaming Tablet)",
        "com.garena.game.codm"              to "Lenovo Y700 (Gaming Tablet)",
        "com.tencent.tmgp.kr.codm"         to "Lenovo Y700 (Gaming Tablet)",
        "com.vng.codmvn"                    to "Lenovo Y700 (Gaming Tablet)",

        // ── Free Fire / Free Fire Max / Mobile Legends → POCO F5 ─────────────
        "com.dts.freefireth"  to "POCO F5",
        "com.dts.freefiremax" to "POCO F5",
        "com.mobile.legends"  to "POCO F5",

        // ── Wild Rift (all regions) / Infinite Borders → OnePlus 8 Pro 5G ───
        "com.riotgames.league.wildrift"   to "OnePlus 8 Pro 5G",
        "com.riotgames.league.wildrifttw" to "OnePlus 8 Pro 5G",
        "com.riotgames.league.wildriftvn" to "OnePlus 8 Pro 5G",
        "com.netease.lztgglobal"          to "OnePlus 8 Pro 5G",

        // ── Fortnite / LoL Mobile / PES → OnePlus 9 Pro 5G ──────────────────
        "com.epicgames.fortnite"  to "OnePlus 9 Pro 5G",
        "com.epicgames.portal"    to "OnePlus 9 Pro 5G",
        "com.tencent.lolm"        to "OnePlus 9 Pro 5G",
        "jp.konami.pesam"         to "OnePlus 9 Pro 5G",

        // ── TFT / Asphalt / Dead Trigger → ROG Phone 6D Ultimate ─────────────
        "com.riotgames.league.teamfighttactics"   to "ROG Phone 6D Ultimate",
        "com.riotgames.league.teamfighttacticstw" to "ROG Phone 6D Ultimate",
        "com.riotgames.league.teamfighttacticsvn" to "ROG Phone 6D Ultimate",
        "com.gameloft.android.ANMP.GloftA9HM"    to "ROG Phone 6D Ultimate",
        "com.madfingergames.legends"               to "ROG Phone 6D Ultimate",

        // ── Honor of Kings (Global / VN) → Xiaomi 13 Pro ─────────────────────
        "com.levelinfinite.sgameGlobal" to "Xiaomi 13 Pro",
        "com.tencent.tmgp.sgame"        to "Xiaomi 13 Pro",

        // ── Apex Legends / Brawl Stars / Clash of Clans → Xiaomi 11T Pro ─────
        "com.ea.gp.apexlegendsmobilefps" to "Xiaomi 11T Pro",
        "com.supercell.brawlstars"       to "Xiaomi 11T Pro",
        "com.supercell.clashofclans"     to "Xiaomi 11T Pro",
        "com.levelinfinite.hotta.gp"     to "Xiaomi 11T Pro",
        "com.vng.mlbbvn"                 to "Xiaomi 11T Pro",

        // ── iQOO / CrossFire / COD tencent / Naraka → iQOO 12 Pro ───────────
        "com.tencent.KiHan"    to "iQOO 12 Pro",
        "com.tencent.tmgp.cf"  to "iQOO 12 Pro",
        "com.tencent.tmgp.cod" to "iQOO 12 Pro",
        "com.tencent.tmgp.gnyx" to "iQOO 12 Pro",

        // ── Black Desert / FIFA Mobile → ROG Phone 5s ────────────────────────
        "com.pearlabyss.blackdesertm.gl" to "ROG Phone 5s",
        "com.pearlabyss.blackdesertm"    to "ROG Phone 5s",
        "com.ea.gp.fifamobile"           to "ROG Phone 5s",

        // ── Where Winds Meet / Genshin-tier open world → Xiaomi 14 Ultra ─────
        "com.netease.game.xyzmobile"       to "Xiaomi 14 Ultra",
        "com.miHoYo.GenshinImpact"         to "Xiaomi 14 Ultra",
        "com.HoYoverse.GenshinImpactpc"    to "Xiaomi 14 Ultra",
        "com.miHoYo.HonkaiStarRail"        to "Xiaomi 14 Ultra",
        "com.HoYoverse.StarRailGlobal"     to "Xiaomi 14 Ultra",

        // ── Aether Gazer → RedMagic 9 Pro ────────────────────────────────────
        "com.YoStar.AetherGazer" to "RedMagic 9 Pro",

        // ── Mobile Legends VN → Xiaomi 11T Pro ───────────────────────────────
        "com.proximabeta.mf.uamo" to "Xiaomi 11T Pro",
    )

    /** Returns the recommended profile name for a package, or null if none known. */
    fun getFor(packageName: String): String? = MAP[packageName]
}
