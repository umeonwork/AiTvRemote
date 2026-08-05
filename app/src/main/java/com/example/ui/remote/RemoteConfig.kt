package com.example.ui.remote

import androidx.compose.ui.graphics.Color
import com.example.model.DeviceEntity

enum class BrandType(val displayName: String) {
    XIAOMI("Mi / Xiaomi / Redmi TV"),
    TCL("TCL / iFFALCON"),
    VU("Vu Smart TV"),
    ONEPLUS("OnePlus TV"),
    REALME("Realme Smart TV"),
    HISENSE("Hisense (VIDAA / Android)"),
    SAMSUNG("Samsung Smart TV (Tizen)"),
    LG("LG webOS TV"),
    SONY("Sony Bravia"),
    ROKU("Roku TV / Express / Stick"),
    FIRE_TV("Amazon Fire TV"),
    MOTOROLA_NOKIA("Motorola / Nokia TV"),
    THOMSON_KODAK("Thomson / Kodak / Blaupunkt"),
    PANASONIC("Panasonic Smart TV"),
    TOSHIBA("Toshiba TV"),
    PHILIPS("Philips Smart TV"),
    SHARP("Sharp Aquos"),
    GENERIC_ANDROID_TV("Android TV / Google TV"),
    GENERIC_SMART_TV("Universal Smart TV");

    companion object {
        fun fromDeviceType(typeString: String?): BrandType {
            if (typeString == null) return GENERIC_SMART_TV
            val lower = typeString.lowercase()
            return when {
                lower.contains("roku") -> ROKU
                lower.contains("fire") || lower.contains("amazon") -> FIRE_TV
                lower.contains("mi") || lower.contains("xiaomi") || lower.contains("redmi") -> XIAOMI
                lower.contains("iffalcon") || lower.contains("tcl") -> TCL
                lower.contains("vu") -> VU
                lower.contains("oneplus") -> ONEPLUS
                lower.contains("realme") -> REALME
                lower.contains("hisense") || lower.contains("vidaa") -> HISENSE
                lower.contains("samsung") || lower.contains("tizen") -> SAMSUNG
                lower.contains("lg") || lower.contains("webos") -> LG
                lower.contains("sony") || lower.contains("bravia") -> SONY
                lower.contains("motorola") || lower.contains("nokia") -> MOTOROLA_NOKIA
                lower.contains("thomson") || lower.contains("kodak") || lower.contains("blaupunkt") -> THOMSON_KODAK
                lower.contains("panasonic") -> PANASONIC
                lower.contains("toshiba") -> TOSHIBA
                lower.contains("philips") -> PHILIPS
                lower.contains("sharp") -> SHARP
                lower.contains("google") || lower.contains("android") -> GENERIC_ANDROID_TV
                else -> GENERIC_SMART_TV
            }
        }
    }
}

enum class RemoteCommand {
    POWER, INPUT, MUTE, VOL_UP, VOL_DOWN, CH_UP, CH_DOWN,
    UP, DOWN, LEFT, RIGHT, SELECT,
    BACK, HOME, MENU, GUIDE, INFO,
    PLAY, PAUSE, STOP, REWIND, FAST_FORWARD,
    KEY_0, KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9,
    
    // Indian & Global App Shortcuts
    APP_HOTSTAR, APP_JIOCINEMA, APP_YOUTUBE, APP_NETFLIX, APP_PRIME, APP_SONYLIV, APP_ZEE5, APP_SPOTIFY,
    
    // Brand Specific Commands
    RED_BUTTON, GREEN_BUTTON, YELLOW_BUTTON, BLUE_BUTTON,
    LG_POINTER, LG_SMART_HOME,
    SAMSUNG_AMBIENT, SAMSUNG_SOURCE,
    SONY_SYNC_MENU, SONY_ACTION_MENU,
    XIAOMI_PATCHWALL, ONEPLUS_OXYGENPLAY, HISENSE_VIDAA_HOME
}

data class RemoteButtonConfig(
    val command: RemoteCommand,
    val label: String,
    val iconName: String? = null,
    val primaryColor: Color? = null,
    val isEnabled: Boolean = true
)

data class AppShortcut(
    val name: String,
    val command: RemoteCommand,
    val backgroundColor: Color,
    val textColor: Color
)

data class RemoteLayoutConfig(
    val brandType: BrandType,
    val showNumericKeypad: Boolean = true,
    val showColorButtons: Boolean = false,
    val showBrandSpecificControls: Boolean = true,
    val appShortcuts: List<AppShortcut> = defaultShortcuts(brandType)
) {
    companion object {
        fun defaultConfigFor(brandType: BrandType): RemoteLayoutConfig {
            return when (brandType) {
                BrandType.SONY, BrandType.LG -> RemoteLayoutConfig(
                    brandType = brandType,
                    showColorButtons = true,
                    showBrandSpecificControls = true
                )
                BrandType.SAMSUNG, BrandType.XIAOMI, BrandType.ONEPLUS, BrandType.TCL, BrandType.VU -> RemoteLayoutConfig(
                    brandType = brandType,
                    showColorButtons = false,
                    showBrandSpecificControls = true
                )
                else -> RemoteLayoutConfig(
                    brandType = brandType,
                    showColorButtons = false,
                    showBrandSpecificControls = false
                )
            }
        }

        private fun defaultShortcuts(brand: BrandType): List<AppShortcut> {
            return listOf(
                AppShortcut("Hotstar", RemoteCommand.APP_HOTSTAR, Color(0xFF0C2040), Color.White),
                AppShortcut("JioCinema", RemoteCommand.APP_JIOCINEMA, Color(0xFFD81B60), Color.White),
                AppShortcut("YouTube", RemoteCommand.APP_YOUTUBE, Color(0xFFFF0000), Color.White),
                AppShortcut("Prime", RemoteCommand.APP_PRIME, Color(0xFF00A8E1), Color.White),
                AppShortcut("Netflix", RemoteCommand.APP_NETFLIX, Color(0xFFE50914), Color.White),
                AppShortcut("SonyLIV", RemoteCommand.APP_SONYLIV, Color(0xFF0F172A), Color.White),
                AppShortcut("ZEE5", RemoteCommand.APP_ZEE5, Color(0xFF8257E5), Color.White)
            )
        }
    }
}
