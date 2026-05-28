/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.yzygui.category

import net.ccbluex.liquidbounce.FDPClient.CLIENT_NAME
import net.ccbluex.liquidbounce.features.module.Category
import net.minecraft.util.ResourceLocation
import java.awt.Color
import java.util.*

/**
 * @author opZywl - Category
 */
enum class yzyCategory(val parent: Category, val displayName: String, val color: Color, val iconName: String = displayName) {
    COMBAT(Category.COMBAT, "战斗", Color(-0x19b2c6), "Combat"),
    PLAYER(Category.PLAYER, "玩家", Color(-0x71ba52), "Player"),
    MOVEMENT(Category.MOVEMENT, "移动", Color(-0xd13291), "Movement"),
    VISUAL(Category.VISUAL, "视觉", Color(-0xc9fe32), "Visual"),
    CLIENT(Category.CLIENT, "客户端", Color(0xCBFF02), "Client"),
    OTHER(Category.OTHER, "其他", Color(0xFFC200), "Other"),
    EXPLOIT(Category.EXPLOIT, "漏洞利用", Color(-0xcc6727), "Exploit");

    fun getIcon(): ResourceLocation {
        return ResourceLocation("${CLIENT_NAME.lowercase()}/texture/clickgui/${iconName.lowercase(Locale.getDefault())}.png")
    }

    companion object {
        fun of(category: Category): yzyCategory? {
            return entries.find { it.parent == category }
        }
    }
}