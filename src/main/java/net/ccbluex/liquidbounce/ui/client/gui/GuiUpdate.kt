/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.features.module.modules.client.HUDModule.guiColor
import net.ccbluex.liquidbounce.handler.api.ClientUpdate
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.performAllChecksAsync
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.io.get
import net.ccbluex.liquidbounce.utils.io.jsonBody
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBloom
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.GuiButton
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GuiUpdate : AbstractScreen() {

    private var isLoading = false
    private var loadProgress = 0
    private var errorMessage: String? = null

    private var latestReleaseText: String = "最新正式版: 加载中..."
    private var lastCommitText: String = "最新测试版: 加载中..."

    override fun initGui() {
        val j = height / 4 + 24

        +GuiButton(1, width / 2 + 2, j + 24 * 2, 98, 20, "忽略")
        +GuiButton(2, width / 2 - 100, j + 24 * 2, 98, 20, "前往下载页面")
        +GuiButton(3, width / 2 - 49, j + 24 * 3, 98, 20, "刷新API")
        +GuiButton(4, width / 2 - 49, j + 24 * 4, 98, 20, "加入Discord")

        loadGitMeta()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) = assumeNonVolatile {
        drawBackground(0)

        val messageYPosition = (height / 8f + 60).toInt()
        val titleYPosition = (height / 16f + 10).toInt()

        val mainMessage = "新版本可用！"
        val mainMessageWidth = Fonts.minecraftFont.getStringWidth(mainMessage)
        Fonts.minecraftFont.drawStringWithShadow(
            mainMessage,
            (width / 2f - mainMessageWidth / 2),
            messageYPosition.toFloat(),
            0xffffff
        )

        val subMessage = "点击\"下载\"访问我们的网站，或点击\"忽略\"关闭此消息。"
        val subMessageWidth = Fonts.minecraftFont.getStringWidth(subMessage)
        Fonts.minecraftFont.drawStringWithShadow(
            subMessage,
            (width / 2f - subMessageWidth / 2),
            (messageYPosition + Fonts.minecraftFont.FONT_HEIGHT).toFloat(),
            0xffffff
        )

        val releaseY = (messageYPosition + Fonts.minecraftFont.FONT_HEIGHT * 3).toFloat()
        val releaseWidth = Fonts.minecraftFont.getStringWidth(latestReleaseText)
        Fonts.minecraftFont.drawStringWithShadow(
            latestReleaseText,
            (width / 2f - releaseWidth / 2),
            releaseY,
            0xffffff
        )

        val commitY = releaseY + Fonts.minecraftFont.FONT_HEIGHT + 2
        val commitWidth = Fonts.minecraftFont.getStringWidth(lastCommitText)
        Fonts.minecraftFont.drawStringWithShadow(
            lastCommitText,
            (width / 2f - commitWidth / 2),
            commitY,
            0xffffff
        )

        glPushMatrix()
        glScalef(2F, 2F, 2F)
        val titleWidth = Fonts.minecraftFont.getStringWidth("新更新可用！") / 2
Fonts.minecraftFont.drawStringWithShadow(
    "新更新可用！",
    (width / 4f) - titleWidth,
    titleYPosition / 2f,
    Color(255, 0, 0).rgb
)
        glPopMatrix()

        if (isLoading) {
            drawLoadingBar()
        }

        errorMessage?.let {
            val errorWidth = Fonts.minecraftFont.getStringWidth(it)
            Fonts.minecraftFont.drawStringWithShadow(it, (width / 2f - errorWidth / 2), height - 30f, Color.RED.rgb)
        }

        drawBloom(mouseX - 5, mouseY - 5, 10, 10, 16, Color(guiColor))

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawLoadingBar() {
        val barWidth = width / 2
        val barX = (width - barWidth) / 2
        val barY = height - 50
        val filledWidth = (barWidth * (loadProgress / 100.0)).toInt()

        drawRect(barX, barY, barX + barWidth, barY + 10, Color(50, 50, 50, 150).rgb)
        drawRect(barX, barY, barX + filledWidth, barY + 10, Color(0, 200, 0).rgb)

        val progressText = "$loadProgress%"
        val textWidth = Fonts.minecraftFont.getStringWidth(progressText)
        Fonts.minecraftFont.drawStringWithShadow(
            progressText,
            (width / 2f - textWidth / 2),
            (barY - Fonts.minecraftFont.FONT_HEIGHT).toFloat(),
            0xffffff
        )

        if (isLoading) {
            loadProgress += 10
            if (loadProgress >= 100) {
                loadProgress = 100
                isLoading = false
            }
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            1 -> mc.displayGuiScreen(GuiMainMenu())
            2 -> MiscUtils.showURL("https://fdpinfo.github.io/download")
            3 -> {
                isLoading = true
                loadProgress = 0
                errorMessage = null
                screenScope.launch {
                    try {
                        performAllChecksAsync()
                    } catch (e: Exception) {
                        errorMessage = "刷新API失败: ${(e as? JsonIOException)?.message ?: e.message}"
                    } finally {
                        isLoading = false
                        loadProgress = 100
                    }
                }
            }
            4 -> MiscUtils.showURL("https://discord.com/invite/3XRFGeqEYD")
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
        } else {
            super.keyTyped(typedChar, keyCode)
        }
    }

    private fun loadGitMeta() {
        val abbrev = ClientUpdate.gitInfo.getProperty("git.commit.id.abbrev") ?: "unknown"
        val rawTime = ClientUpdate.gitInfo.getProperty("git.commit.time")
            ?: ClientUpdate.gitInfo.getProperty("git.build.time")
        val prettyDate = formatGitDate(rawTime)
        lastCommitText = "最后提交: $prettyDate ($abbrev)"

        screenScope.launch(Dispatchers.IO) {
            val rel: GithubRelease? = try {
                HttpClient.get("https://api.github.com/repos/SkidderMC/FDPClient/releases/latest")
                    .jsonBody<GithubRelease>()
            } catch (_: Exception) { null }

            withContext(Dispatchers.Main) {
                latestReleaseText = if (rel != null) {
                    "最新正式版: ${rel.tagName}"
                } else {
                    "最新正式版: 不可用"
                }
            }
        }
    }

    private fun formatGitDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "unknown"
        val out = SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val candidates = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "EEE MMM dd HH:mm:ss zzz yyyy"
        ).map { SimpleDateFormat(it, Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }

        for (fmt in candidates) {
            try { return out.format(fmt.parse(raw)) } catch (_: Exception) {}
        }
        return raw.take(10).replace('-', '.')
    }
}