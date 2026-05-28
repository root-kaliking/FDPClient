/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.FDPClient.CLIENT_NAME
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.normal.Main
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.objects.Drag
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.Scroll
import net.minecraft.util.ResourceLocation

enum class Category(
    val displayName: String,
    val configName: String,
    val htmlIcon: String,
    initialPosX: Int,
    initialPosY: Int,
    val clicked: Boolean = false,
    val showMods: Boolean = true,
    val subCategories: Array<SubCategory>
) {
    COMBAT("战斗", "Combat", "&#xe000;", 15, 15, subCategories = arrayOf(SubCategory.COMBAT_RAGE, SubCategory.COMBAT_LEGIT)),
    PLAYER("玩家", "Player", "&#xe7fd;", 15, 180, subCategories = arrayOf(SubCategory.PLAYER_COUNTER, SubCategory.PLAYER_ASSIST)),
    MOVEMENT("移动", "Movement", "&#xe566;", 330, 15, subCategories = arrayOf(SubCategory.MOVEMENT_MAIN, SubCategory.MOVEMENT_EXTRAS)),
    VISUAL("视觉", "Visual", "&#xe417;", 225, 15, subCategories = arrayOf(SubCategory.RENDER_SELF, SubCategory.RENDER_OVERLAY)),
    CLIENT("客户端", "Client", "&#xe869;", 15, 330, subCategories = arrayOf(SubCategory.CLIENT_GENERAL, SubCategory.CONFIGS)),
    OTHER("其他", "Other", "&#xe5d3;", 15, 330, subCategories = arrayOf(SubCategory.MISCELLANEOUS)),
    EXPLOIT("利用", "Exploit", "&#xe868;", 120, 180, subCategories = arrayOf(SubCategory.EXPLOIT_EXTRAS));

    var posX: Int = 40 + (Main.categoryCount * 120)
    var posY: Int = initialPosY

    val scroll = Scroll()
    val drag = Drag(posX.toFloat(), posY.toFloat())

    init {
        Main.categoryCount++
    }

    val iconResourceLocation = ResourceLocation("${CLIENT_NAME.lowercase()}/texture/category/${name.lowercase()}.png")

    enum class SubCategory(val displayName: String, val icon: String) {
        // Combat
        COMBAT_RAGE("暴力", "a"),
        COMBAT_LEGIT("合法", "e"),

        // Movement
        MOVEMENT_MAIN("主要", "g"),
        MOVEMENT_EXTRAS("额外", "f"),

        // Visual
        RENDER_SELF("自身", "m"),
        RENDER_OVERLAY("覆盖", "h"),

        // Player
        PLAYER_COUNTER("反击", "n"),
        PLAYER_ASSIST("辅助", "l"),

        // Client / Configs
        CLIENT_GENERAL("客户端", "h"),
        CONFIGS("配置", "x"),

        // Other
        MISCELLANEOUS("杂项", "\ue5d3"),

        // Exploit
        EXPLOIT_EXTRAS("额外", "j"),

        // Fallback
        GENERAL("通用", "h");

        override fun toString() = displayName
    }
}