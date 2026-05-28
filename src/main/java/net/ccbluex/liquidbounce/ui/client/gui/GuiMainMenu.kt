/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.gui

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.FDPClient.CLIENT_NAME
import net.ccbluex.liquidbounce.FDPClient.clientVersionText
import net.ccbluex.liquidbounce.features.module.modules.client.HUDModule.guiColor
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.handler.api.ClientUpdate
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.gui.button.ImageButton
import net.ccbluex.liquidbounce.ui.client.gui.button.QuitButton
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.Fonts.minecraftFont
import net.ccbluex.liquidbounce.ui.font.fontmanager.GuiFontManager
import net.ccbluex.liquidbounce.utils.client.JavaVersion
import net.ccbluex.liquidbounce.utils.client.javaVersion
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.bugs
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.canConnect
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.changelogs
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.isLatest
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.io.get
import net.ccbluex.liquidbounce.utils.io.jsonBody
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBloom
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawShadowRect
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.GuiModList
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.lwjgl.input.Mouse

data class GithubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("published_at")
    val publishedAt: String,
    val body: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    val prerelease: Boolean,
)

class GuiMainMenu : AbstractScreen(), GuiYesNoCallback {

    private var popup: PopupScreen? = null
    private var popupOnce = false

    init {
        if (!popupOnce) {
            javaVersion?.let {
                when {
                    it.major == 1 && it.minor == 8 && it.update < 100 -> showOutdatedJava8Warning()
                    it.major > 8 -> showJava11Warning()
                }
            }
            if (FileManager.firstStart) {
                showWelcomePopup()
            } else {
                checkGithubUpdate()
                checkOutdatedVersionPopup()
            }
            popupOnce = true
        }
    }

    private var logo: ResourceLocation? = null

    private lateinit var btnSinglePlayer: GuiButton
    private lateinit var btnMultiplayer: GuiButton
    private lateinit var btnClientOptions: GuiButton
    private lateinit var btnFontManager: GuiButton
    private lateinit var btnCheckUpdate: GuiButton

    private lateinit var btnClickGUI: ImageButton
    private lateinit var btnCommitInfo: ImageButton
    private lateinit var btnCosmetics: ImageButton
    private lateinit var btnMinecraftOptions: ImageButton
    private lateinit var btnLanguage: ImageButton
    private lateinit var btnForgeModList: ImageButton
    private lateinit var btnAddAccount: ImageButton

    private lateinit var btnQuit: QuitButton

    override fun initGui() {
        val basePath = "${CLIENT_NAME.lowercase()}/texture/mainmenu/"
        logo = ResourceLocation("${CLIENT_NAME.lowercase()}/texture/mainmenu/logo.png")

        val centerY = height / 2 - 80
        val buttonWidth = 133
        val buttonHeight = 20

        btnSinglePlayer = +GuiButton(0, width / 2 - 66, centerY + 70, buttonWidth, buttonHeight, "单机游戏")
        btnMultiplayer = +GuiButton(1, width / 2 - 66, centerY + 93, buttonWidth, buttonHeight, "多人游戏")
        btnClientOptions = +GuiButton(2, width / 2 - 66, centerY + 116, buttonWidth, buttonHeight, "设置")
        btnFontManager = +GuiButton(3, width / 2 - 66, centerY + 139, buttonWidth, buttonHeight, "字体管理")
        btnCheckUpdate = GuiButton(4, width / 2 - 66, centerY + 162, buttonWidth, buttonHeight, "§a检查更新")

        buttonList.addAll(listOf(btnSinglePlayer, btnMultiplayer, btnClientOptions, btnFontManager, btnCheckUpdate))

        val bottomY = height - 20
        btnClickGUI = ImageButton("功能界面", ResourceLocation("${basePath}clickgui.png"), width / 2 - 45, bottomY)
        btnCommitInfo = ImageButton("提交信息", ResourceLocation("${basePath}github.png"), width / 2 - 30, bottomY)
        btnCosmetics = ImageButton("装饰", ResourceLocation("${basePath}cosmetics.png"), width / 2 - 15, bottomY)
        btnMinecraftOptions = ImageButton("Minecraft设置", ResourceLocation("${basePath}cog.png"), width / 2, bottomY)
        btnLanguage = ImageButton("语言", ResourceLocation("${basePath}globe.png"), width / 2 + 15, bottomY)
        btnForgeModList = ImageButton("Forge模组", ResourceLocation("${basePath}forge.png"), width / 2 + 30, bottomY)
        btnAddAccount = ImageButton("帐号管理", ResourceLocation("${basePath}add-account.png"), width - 55, 7)
        btnQuit = QuitButton(width - 17, 7)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
        if (popup != null) {
            popup!!.mouseClicked(mouseX, mouseY, button)
            return
        }
        buttonList.forEach { guiButton ->
            if (guiButton.mousePressed(mc, mouseX, mouseY)) {
                actionPerformed(guiButton)
            }
        }
        when {
            btnQuit.hoverFade > 0 -> mc.shutdown()
            btnMinecraftOptions.hoverFade > 0 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            btnLanguage.hoverFade > 0 -> mc.displayGuiScreen(GuiLanguage(this, mc.gameSettings, mc.languageManager))
            btnCommitInfo.hoverFade > 0 -> mc.displayGuiScreen(GuiCommitInfo())
            btnForgeModList.hoverFade > 0 -> mc.displayGuiScreen(GuiModList(mc.currentScreen))
            btnCosmetics.hoverFade > 0 -> mc.displayGuiScreen(GuiCommitInfo())
            btnClickGUI.hoverFade > 0 -> {
                try {
                    mc.displayGuiScreen(ClickGui)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ClickGui.initGui()
                    mc.displayGuiScreen(ClickGui)
                }
            }
            btnAddAccount.hoverFade > 0 -> mc.displayGuiScreen(GuiAltManager(this))
        }
    }

    override fun actionPerformed(button: GuiButton) {
        if (popup != null) return

        when (button.id) {
            0 -> mc.displayGuiScreen(GuiSelectWorld(this))
            1 -> mc.displayGuiScreen(GuiMultiplayer(this))
            2 -> mc.displayGuiScreen(GuiInfo(this))
            3 -> mc.displayGuiScreen(GuiFontManager(this))
            4 -> mc.displayGuiScreen(GuiUpdate())
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        assumeNonVolatile = true
        drawBackground(0)
        if (popup != null) {
            popup?.drawScreen(width, height, mouseX, mouseY)
            assumeNonVolatile = false
            return
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            mc.displayGuiScreen(ClickGui)
        }
        GlStateManager.pushMatrix()
        drawShadowRect(
            (width / 2 - 100).toFloat(),
            (height / 2 - 80).toFloat(),
            (width / 2 + 100).toFloat(),
            (height / 2 + 112).toFloat(),
            15F,
            Color(44, 43, 43, 100).rgb
        )

        GlStateManager.disableAlpha()
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f)
        mc.textureManager.bindTexture(logo)

        drawModalRectWithCustomSizedTexture(width / 2 - 25, height / 2 - 68, 0f, 0f, 49, 49, 49f, 49f)
        val apiMessage = if (canConnect) "§e正常" else "§c异常"
        val apiTextX = width - 10f - minecraftFont.getStringWidth("API连接: $apiMessage")
        minecraftFont.drawStringWithShadow("API连接: $apiMessage", apiTextX, 32f, Color(255, 255, 255, 140).rgb)
        val clientNameX = width - 4f - minecraftFont.getStringWidth(CLIENT_NAME)
        minecraftFont.drawStringWithShadow(CLIENT_NAME, clientNameX, height - 23f, Color(255, 255, 255, 140).rgb)
        val uiMessage = when {
            canConnect && isLatest -> " §e(最新)"
            !canConnect && isLatest -> " §c(API已失效)"
            else -> " §c(已过时)"
        }
        val buildInfoText = "当前版本为 $clientVersionText$uiMessage"
        val buildInfoX = width - 4f - minecraftFont.getStringWidth(buildInfoText)
        minecraftFont.drawStringWithShadow(buildInfoText, buildInfoX, height - 12f, Color(255, 255, 255, 140).rgb)

        minecraftFont.drawStringWithShadow("更新日志:", 3f, 32f, Color(255, 255, 255, 150).rgb)

        var changeY = 48
        val changeDetails = changelogs.split("\n")
        for (line in changeDetails) {
            if (line.startsWith("* ")) continue
            val formatted = formatChangelogLine(line)
            minecraftFont.drawStringWithShadow(formatted, 4f, changeY.toFloat(), Color(255, 255, 255, 150).rgb)
            changeY += 8
        }

        val bugsFixedText = "已修复错误:"
        val bugsLabelX = width - 10f - minecraftFont.getStringWidth(bugsFixedText)
        minecraftFont.drawStringWithShadow(bugsFixedText, bugsLabelX, 43f, Color(255, 255, 255, 140).rgb)

        val bugLines = bugs.split("\n").filter { !it.startsWith("#") }
        val displayBugLines = if (bugLines.size > 39) bugLines.takeLast(39) else bugLines

        var bugsY = 55

        for (line in displayBugLines) {
            val formatted = if (line.startsWith("*")) line.substring(1).trim() + " §7[§e*§7]" else line
            val lineWidth = minecraftFont.getStringWidth(formatted)
            val xPos = width - 12f - lineWidth
            minecraftFont.drawStringWithShadow(formatted, xPos, bugsY.toFloat(), Color(255, 255, 255, 140).rgb)
            bugsY += 11
        }

        Fonts.InterMedium_15.drawCenteredStringShadow("by Zywl <3 ", width / 2f, height / 2f - 25, Color(255, 255, 255, 100).rgb)

        buttonList.forEach { it.drawButton(mc, mouseX, mouseY) }

        listOf(btnClickGUI, btnCommitInfo, btnCosmetics, btnMinecraftOptions, btnLanguage, btnForgeModList, btnAddAccount, btnQuit)
            .forEach { it.drawButton(mc, mouseX, mouseY) }
        val branch = FDPClient.clientBranch
        val commitIdAbbrev = ClientUpdate.gitInfo.getProperty("git.commit.id.abbrev")
        val infoStr = "$CLIENT_NAME($branch/$commitIdAbbrev) | Minecraft 1.8.9"
        Fonts.fontSemibold35.drawCenteredString(infoStr, 7F, (height - 11).toFloat(), Color(255, 255, 255, 100).rgb)

        drawBloom(mouseX - 5, mouseY - 5, 10, 10, 16, Color(guiColor))

        GlStateManager.popMatrix()

        assumeNonVolatile = false
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    /**
     * Quick method to colorize changelog lines by prefix:
     * "~ " => "[~]"
     * "+ " => "[+]"
     * "- " => "[-]"
     */
    private fun formatChangelogLine(line: String): String {
        return when {
            line.startsWith("~ ") -> "§7[§r~§7]  §r" + line.removePrefix("~ ").trim()
            line.startsWith("+ ") -> "§7[§a+§7]  §r" + line.removePrefix("+ ").trim()
            line.startsWith("- ") -> "§7[§c-§7]  §r" + line.removePrefix("- ").trim()
            else -> line
        }
    }

    private fun showWelcomePopup() {
        popup = PopupScreen {
            title("§a§l欢迎!")
            message(
                """
                §e感谢您下载并安装 §b$CLIENT_NAME§e!
        
                §6以下是一些有用的信息:
                §a- 功能界面: 按下 §7[右Shift]§f 打开功能界面。
                §a- 右键点击带'+'的模块以进行编辑。
                §a- 将鼠标悬停在模块上可查看其说明。
        
                §6重要命令:
                §a- .bind <模块> <按键> / .bind <模块> none
                §a- .config load <名称> / .config list
        
                §b需要帮助？联系我们！
                - §f作者: §9https://github.com/opZywl
                - §fDiscord: §9https://discord.gg/WV6qPzyqTx
                - §fGithub: §9https://github.com/SkidderMC/FDPClient
                - §fYouTube: §9https://www.youtube.com/@opZywl
                """.trimIndent()
            )
            button("§a确定")
            onClose { popup = null }
        }
    }

    private fun checkGithubUpdate() {
        screenScope.launch(Dispatchers.IO) {
            val githubRelease = fetchLatestGithubRelease()
            if (githubRelease != null && githubRelease.tagName != clientVersionText) {
                withContext(Dispatchers.Main) {
                    showUpdatePopup(githubRelease)
                }
            }
        }
    }

    private fun fetchLatestGithubRelease(): GithubRelease? = try {
        HttpClient.get("https://api.github.com/repos/SkidderMC/FDPClient/releases/latest")
            .jsonBody<GithubRelease>()
    } catch (e: Exception) {
        null
    }

    private fun showUpdatePopup(githubRelease: GithubRelease) {
        val updateType = if (!githubRelease.prerelease) "版本" else "测试版"
        val dateFormatter = SimpleDateFormat("yyyy年MM月dd日 EEEE h a z", Locale.CHINESE)
        val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        inputFormatter.timeZone = TimeZone.getTimeZone("UTC")
        val publishedDate = inputFormatter.parse(githubRelease.publishedAt)
        val formattedDate = dateFormatter.format(publishedDate)

        popup = PopupScreen {
            title("§b新版本可用!")
            message(
                """
                §e$CLIENT_NAME 有一个新的${updateType}可用！
        
                - §a版本:§r ${githubRelease.tagName}
                - §a发布日期:§r $formattedDate
        
                §6更新内容:§r
                ${githubRelease.body}
        
                §b立即升级以享受最新功能和改进！§r
                """.trimIndent()
            )
            button("§a下载") { MiscUtils.showURL(githubRelease.htmlUrl) }
            onClose { popup = null }
        }
    }

    private fun showOutdatedJava8Warning() {
        popup = PopupScreen {
            title("§c§lJava运行环境版本过旧")
            message(
                """
                §6§l您正在使用一个过时的Java 8版本 (${javaVersion?.raw ?: "未知"})。
                
                §f这可能会导致意外的 §c§l错误§f。
                请更新到8u101以上版本，或从网上重新下载新版本。
                """.trimIndent()
            )
            button("§a下载Java") { MiscUtils.showURL(JavaVersion.DOWNLOAD_PAGE) }
            button("§e我已知晓")
            onClose { popup = null }
        }
    }

    private fun showJava11Warning() {
        popup = PopupScreen {
            title("§c§l不兼容的Java运行环境")
            message(
                """
                §6§l此版本的 $CLIENT_NAME 是为Java 8环境设计的。
                
                §f更高版本的Java可能导致错误或崩溃。
                建议安装JRE 8。
                """.trimIndent()
            )
            button("§a下载Java") { MiscUtils.showURL(JavaVersion.DOWNLOAD_PAGE) }
            button("§e我已知晓")
            onClose { popup = null }
        }
    }

    private fun checkOutdatedVersionPopup() {
        if (!isLatest && canConnect) {
            popup = PopupScreen {
                title("§b新版本可用!")
                message(
                    """
                    §e您正在使用 $CLIENT_NAME 的旧版本。
                    请更新到最新版本以获得新功能和改进。
                    """.trimIndent()
                )
                button("§a下载更新") { MiscUtils.showURL("https://github.com/SkidderMC/FDPClient/releases/latest") }
                onClose { popup = null }
            }
        }
    }

    override fun handleMouseInput() {
        if (popup != null) {
            val eventDWheel = Mouse.getEventDWheel()
            if (eventDWheel != 0) {
                popup!!.handleMouseWheel(eventDWheel)
            }
        }

        super.handleMouseInput()
    }
}