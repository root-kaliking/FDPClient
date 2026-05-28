/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.gui

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.client.SpotifyModule
import net.ccbluex.liquidbounce.features.module.modules.client.SpotifyModule.BrowserAuthStatus
import net.ccbluex.liquidbounce.handler.spotify.SpotifyIntegration
import net.ccbluex.liquidbounce.ui.client.spotify.SpotifyConnectionChangedEvent
import net.ccbluex.liquidbounce.ui.client.spotify.SpotifyConnectionState
import net.ccbluex.liquidbounce.ui.client.spotify.SpotifyState
import net.ccbluex.liquidbounce.ui.client.spotify.SpotifyStateChangedEvent
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.io.get
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.resources.I18n
import net.minecraft.util.ResourceLocation
import okhttp3.Response
import org.lwjgl.input.Keyboard
import java.io.IOException
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

/**
 * Adapted SpotifyCraft display that works inside the FDPClient Spotify module.
 */
class GuiSpotify(private val prevGui: GuiScreen?) : AbstractScreen(), Listenable {

    private lateinit var clientIdField: GuiTextField
    private lateinit var clientSecretField: GuiTextField
    private lateinit var refreshTokenField: GuiTextField

    private val configurationFieldStart = 100
    private val configurationSpacing = 28

    private lateinit var saveButton: GuiButton
    private lateinit var browserButton: GuiButton
    private lateinit var modeButton: GuiButton
    private lateinit var autoReconnectButton: GuiButton
    private lateinit var pollInfoButton: GuiButton
    private lateinit var pollDecreaseButton: GuiButton
    private lateinit var pollIncreaseButton: GuiButton
    private lateinit var moduleToggleButton: GuiButton
    private lateinit var dashboardButton: GuiButton
    private lateinit var guideButton: GuiButton
    private lateinit var playerButton: GuiButton
    private lateinit var backButton: GuiButton

    private var playbackState: SpotifyState? = SpotifyModule.currentState
    private var connectionState: SpotifyConnectionState = SpotifyModule.connectionState
    private var connectionError: String? = SpotifyModule.lastErrorMessage

    private var listening = true

    private var authStatus: Pair<BrowserAuthStatus, String>? = null
    private var authStatusTimestamp = 0L

    private var coverTexture: ResourceLocation? = null
    private var coverUrl: String? = null
    private val coverCache = mutableMapOf<String, ResourceLocation>()

    private val connectionHandler = handler<SpotifyConnectionChangedEvent>(always = true) { event ->
        connectionState = event.state
        connectionError = event.errorMessage ?: SpotifyModule.lastErrorMessage
    }

    private val stateHandler = handler<SpotifyStateChangedEvent>(always = true) { event ->
        playbackState = event.state
        updateCoverTexture(event.state)
    }

    override fun handleEvents(): Boolean = listening

    override fun initGui() {
        super.initGui()
        Keyboard.enableRepeatEvents(true)
        listening = true
        buttonList.clear()
        textFields.clear()

        val rightColumnX = width / 2 + 10
        val fieldWidth = width / 2 - 60
        clientIdField = textField(101, mc.fontRendererObj, rightColumnX + 10, configurationFieldStart, fieldWidth, 18)
        clientIdField.text = SpotifyModule.clientId
        clientSecretField = textField(102, mc.fontRendererObj, rightColumnX + 10, configurationFieldStart + configurationSpacing, fieldWidth, 18)
        clientSecretField.text = SpotifyModule.clientSecret
        refreshTokenField = textField(103, mc.fontRendererObj, rightColumnX + 10, configurationFieldStart + configurationSpacing * 2, fieldWidth, 18)
        refreshTokenField.text = SpotifyModule.refreshToken

        saveButton = +GuiButton(
            BUTTON_SAVE,
            rightColumnX + 10,
            configurationFieldStart + configurationSpacing * 3 + 6,
            fieldWidth,
            20,
            "保存凭证"
        )
        browserButton = +GuiButton(
            BUTTON_BROWSER,
            rightColumnX + 10,
            saveButton.yPosition + saveButton.height + 4,
            fieldWidth,
            20,
            "启动浏览器授权"
        )
        modeButton = +GuiButton(
            BUTTON_MODE,
            rightColumnX + 10,
            browserButton.yPosition + browserButton.height + 4,
            fieldWidth,
            20,
            ""
        )
        autoReconnectButton = +GuiButton(
            BUTTON_AUTO_RECONNECT,
            rightColumnX + 10,
            modeButton.yPosition + modeButton.height + 4,
            fieldWidth,
            20,
            ""
        )
        pollInfoButton = +GuiButton(
            BUTTON_POLL_INFO,
            rightColumnX + 10,
            autoReconnectButton.yPosition + autoReconnectButton.height + 4,
            fieldWidth - 48,
            20,
            ""
        ).apply { enabled = false }
        pollDecreaseButton = +GuiButton(
            BUTTON_POLL_DOWN,
            pollInfoButton.xPosition + pollInfoButton.width + 2,
            pollInfoButton.yPosition,
            22,
            20,
            "-"
        )
        pollIncreaseButton = +GuiButton(
            BUTTON_POLL_UP,
            pollDecreaseButton.xPosition + pollDecreaseButton.width + 2,
            pollInfoButton.yPosition,
            22,
            20,
            "+"
        )

        moduleToggleButton = +GuiButton(
            BUTTON_TOGGLE,
            rightColumnX + 10,
            pollInfoButton.yPosition + pollInfoButton.height + 8,
            fieldWidth,
            20,
            ""
        )
        dashboardButton = +GuiButton(
            BUTTON_DASHBOARD,
            rightColumnX + 10,
            moduleToggleButton.yPosition + moduleToggleButton.height + 4,
            (fieldWidth - 4) / 2,
            20,
            "打开仪表盘"
        )
        guideButton = +GuiButton(
            BUTTON_GUIDE,
            dashboardButton.xPosition + dashboardButton.width + 4,
            dashboardButton.yPosition,
            (fieldWidth - 4) / 2,
            20,
            "授权指南"
        )

        playerButton = +GuiButton(
            BUTTON_PLAYER,
            rightColumnX + 10,
            guideButton.yPosition + guideButton.height + 4,
            fieldWidth,
            20,
            "打开音乐浏览器"
        )

        backButton = +GuiButton(
            BUTTON_BACK,
            width - 110,
            height - 30,
            90,
            20,
            I18n.format("gui.done")
        )

        updateModeState()
        updateCoverTexture(playbackState)
        authStatus = null
    }

    override fun onGuiClosed() {
        listening = false
        Keyboard.enableRepeatEvents(false)
        super.onGuiClosed()
    }

    override fun updateScreen() {
        super.updateScreen()
        textFields.forEach(GuiTextField::updateCursorCounter)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }

        for (field in textFields) {
            if (field.textboxKeyTyped(typedChar, keyCode)) {
                return
            }
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) {
            return
        }

        when (button.id) {
            BUTTON_BACK -> mc.displayGuiScreen(prevGui)
            BUTTON_SAVE -> handleSave()
            BUTTON_BROWSER -> beginBrowserAuthorization()
            BUTTON_MODE -> {
                SpotifyModule.cycleAuthMode()
                updateModeState()
            }

            BUTTON_AUTO_RECONNECT -> {
                SpotifyModule.toggleAutoReconnect()
                updateModeState()
            }

            BUTTON_POLL_DOWN -> adjustPollInterval(-1)
            BUTTON_POLL_UP -> adjustPollInterval(1)
            BUTTON_TOGGLE -> {
                SpotifyModule.state = !SpotifyModule.state
                updateModeState()
            }

            BUTTON_DASHBOARD -> SpotifyIntegration.openDashboard()
            BUTTON_GUIDE -> SpotifyIntegration.openGuide()
            BUTTON_PLAYER -> {
                listening = false
                SpotifyModule.openPlayerScreen()
            }
        }
    }

    private fun handleSave() {
        val updated = SpotifyModule.updateCredentials(
            clientIdField.text,
            clientSecretField.text,
            refreshTokenField.text,
        )
        if (updated) {
            showStatus(BrowserAuthStatus.SUCCESS, "已保存手动凭证。")
        } else {
            showStatus(BrowserAuthStatus.ERROR, "手动模式需要客户端ID、密钥和刷新令牌。")
        }
    }

    private fun beginBrowserAuthorization() {
        val started = SpotifyModule.beginBrowserAuthorization { status, message ->
            mc.addScheduledTask {
                showStatus(status, message)
            }
        }
        if (!started) {
            showStatus(BrowserAuthStatus.ERROR, "无法启动浏览器授权。")
        }
    }

    private fun showStatus(status: BrowserAuthStatus, message: String) {
        authStatus = status to message
        authStatusTimestamp = System.currentTimeMillis()
    }

    private fun adjustPollInterval(delta: Int) {
        val next = (SpotifyModule.pollIntervalSeconds + delta).coerceIn(3, 60)
        SpotifyModule.setPollInterval(next)
        updateModeState()
    }

    private fun updateModeState() {
        val manualMode = SpotifyModule.authMode == SpotifyModule.SpotifyAuthMode.MANUAL
        val quickSupported = SpotifyModule.supportsQuickConnect()

        clientIdField.setEnabled(manualMode)
        clientSecretField.setEnabled(manualMode)
        refreshTokenField.setEnabled(manualMode)
        saveButton.enabled = manualMode

        modeButton.displayString = SpotifyModule.authModeLabel()
        autoReconnectButton.displayString = "自动重连: ${if (SpotifyModule.autoReconnect) "开" else "关"}"
        pollInfoButton.displayString = "轮询间隔: ${SpotifyModule.pollIntervalSeconds}秒"
        moduleToggleButton.displayString = if (SpotifyModule.state) "禁用Spotify模块" else "启用Spotify模块"
        browserButton.displayString =
            "${if (quickSupported) "关联账号" else "授权"} (${SpotifyModule.authMode.displayName})"
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        drawTitle()
        drawPlaybackColumn()
        drawConfigurationColumn()

        super.drawScreen(mouseX, mouseY, partialTicks)
        textFields.forEach(GuiTextField::drawTextBox)
        drawStatusMessage()
    }

    private fun drawTitle() {
        val title = "SpotifyCraft 显示"
        mc.fontRendererObj.drawString(
            title,
            width / 2 - mc.fontRendererObj.getStringWidth(title) / 2,
            15,
            0xFFFFFF
        )
        val subtitle = "在FDPClient内控制Spotify播放"
        mc.fontRendererObj.drawString(
            subtitle,
            width / 2 - mc.fontRendererObj.getStringWidth(subtitle) / 2,
            28,
            0xFF9EA3AD.toInt()
        )
    }

    private fun drawPlaybackColumn() {
        val left = 20
        val top = 45
        val right = width / 2 - 10
        val bottom = height - 40
        drawRect(left, top, right, bottom, 0xAA050505.toInt())

        val padding = 12
        val coverSize = max(64, min(180, bottom - top - 120))
        val coverX = left + padding
        val coverY = top + padding
        val coverTex = coverTexture
        if (coverTex != null) {
            GlStateManager.color(1f, 1f, 1f, 1f)
            mc.textureManager.bindTexture(coverTex)
            drawModalRectWithCustomSizedTexture(coverX, coverY, 0f, 0f, coverSize, coverSize, coverSize.toFloat(), coverSize.toFloat())
        } else {
            drawRect(coverX, coverY, coverX + coverSize, coverY + coverSize, 0x33000000)
            mc.fontRendererObj.drawString("封面不可用", coverX + 6, coverY + coverSize / 2 - 4, 0xFF777777.toInt())
        }

        val textX = coverX + coverSize + 10
        val textWidth = right - padding - textX
        val state = playbackState
        val track = state?.track

        if (!SpotifyModule.state) {
            mc.fontRendererObj.drawSplitString(
                "启用Spotify模块以开始同步播放。",
                textX,
                coverY,
                textWidth,
                0xFFE05757.toInt()
            )
        } else if (track != null) {
            val title = track.title
            val artist = track.artists
            val album = track.album
            mc.fontRendererObj.drawString(title, textX, coverY, 0xFFFFFFFF.toInt())
            mc.fontRendererObj.drawString(artist, textX, coverY + 12, 0xFF9EA3AD.toInt())
            mc.fontRendererObj.drawString(album, textX, coverY + 24, 0xFF9EA3AD.toInt())

            val progress = computeProgress(state)
            val duration = max(track.durationMs, 1)
            val barWidth = textWidth
            val barY = coverY + 50
            drawRect(textX, barY, textX + barWidth, barY + 4, 0x33000000)
            val fillWidth = (barWidth * progress) / duration
            drawRect(textX, barY, textX + fillWidth, barY + 4, 0xFF1DB954.toInt())
            val timeText = "${formatDuration(progress)} / ${formatDuration(duration)}"
            mc.fontRendererObj.drawString(timeText, textX, barY + 8, 0xFF9EA3AD.toInt())

            val statusText = if (state.isPlaying) "播放中" else "已暂停"
            mc.fontRendererObj.drawString(
                "状态: $statusText",
                textX,
                barY + 20,
                if (state.isPlaying) 0xFF1DB954.toInt() else 0xFFE0A924.toInt()
            )
        } else {
            mc.fontRendererObj.drawSplitString(
                "等待Spotify播放数据。请在任意设备上启动Spotify并保持播放。",
                textX,
                coverY,
                textWidth,
                0xFF9EA3AD.toInt()
            )
        }

        val infoStartY = coverY + coverSize + 16
        val infoLines = mutableListOf(
            "连接: ${connectionState.displayName}",
            "模块状态: ${if (SpotifyModule.state) "启用" else "禁用"}",
            SpotifyModule.authModeLabel(),
            "自动重连: ${if (SpotifyModule.autoReconnect) "开" else "关"}",
            "轮询间隔: ${SpotifyModule.pollIntervalSeconds}秒",
        )
        if (connectionState == SpotifyConnectionState.ERROR && !connectionError.isNullOrBlank()) {
            infoLines += "上次错误: ${connectionError!!.take(64)}"
        }
        if (SpotifyModule.supportsQuickConnect()) {
            infoLines += "快速连接: 可用"
        }

        var lineY = infoStartY
        infoLines.forEach { line ->
            mc.fontRendererObj.drawString(line, left + padding, lineY, 0xFFEEEEEE.toInt())
            lineY += 12
        }
    }

    private fun drawConfigurationColumn() {
        val left = width / 2 + 10
        val top = 45
        val right = width - 20
        val bottom = height - 40
        drawRect(left, top, right, bottom, 0xAA050505.toInt())

        val padding = 12
        val textColor = 0xFFEEEEEE.toInt()
        mc.fontRendererObj.drawString("账号关联", left + padding, top + padding, textColor)

        val manualMode = SpotifyModule.authMode == SpotifyModule.SpotifyAuthMode.MANUAL
        val helperText = if (manualMode) {
            "使用Spotify应用客户端ID/密钥和刷新令牌。"
        } else {
            "快速连接将存储自身凭证。只需运行浏览器授权即可。"
        }
        mc.fontRendererObj.drawSplitString(helperText, left + padding, top + padding + 12, right - left - padding * 2, 0xFF9EA3AD.toInt())

        val disabledColor = 0xFF5A5A5A.toInt()
        val labelColor = if (manualMode) textColor else disabledColor
        val labels = listOf("客户端ID", "客户端密钥", "刷新令牌")
        labels.forEachIndexed { index, label ->
            val labelY = configurationFieldStart + configurationSpacing * index - 12
            mc.fontRendererObj.drawString(label, left + padding, labelY, labelColor)
        }
    }

    private fun drawStatusMessage() {
        val message = authStatus ?: return
        if (System.currentTimeMillis() - authStatusTimestamp > 15000L) {
            return
        }
        val color = when (message.first) {
            BrowserAuthStatus.INFO -> 0xFFE0A924.toInt()
            BrowserAuthStatus.SUCCESS -> 0xFF1DB954.toInt()
            BrowserAuthStatus.ERROR -> 0xFFE05757.toInt()
        }
        val text = message.second
        val widthText = mc.fontRendererObj.getStringWidth(text) + 10
        val x = width / 2 - widthText / 2
        val y = height - 30
        drawRect(x - 4, y - 4, x + widthText + 4, y + 16, 0xCC050505.toInt())
        mc.fontRendererObj.drawString(text, x, y, color)
    }

    private fun computeProgress(state: SpotifyState): Int {
        val track = state.track ?: return state.progressMs
        val elapsed = if (state.isPlaying) (System.currentTimeMillis() - state.updatedAt).toInt() else 0
        return min(track.durationMs, max(0, state.progressMs + elapsed))
    }

    private fun formatDuration(durationMs: Int): String {
        val totalSeconds = max(0, durationMs / 1000)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun updateCoverTexture(state: SpotifyState?) {
        val url = state?.track?.coverUrl
        if (url.isNullOrBlank()) {
            coverUrl = null
            coverTexture = null
            return
        }
        if (url == coverUrl && coverTexture != null) {
            return
        }
        coverUrl = url
        coverTexture = coverCache[url]
        if (coverTexture != null) {
            return
        }

        SharedScopes.IO.launch {
            val imageResult = runCatching {
                HttpClient.get(url).use { response ->
                    ensureSuccess(response)
                    response.body.byteStream().use { stream ->
                        ImageIO.read(stream) ?: throw IOException("Cover art was empty")
                    }
                }
            }
            imageResult.onSuccess { image ->
                mc.addScheduledTask {
                    runCatching {
                        val texture = DynamicTexture(image)
                        val location = mc.textureManager.getDynamicTextureLocation(
                            "spotify/" + UUID.randomUUID(),
                            texture,
                        )
                        coverCache[url] = location
                        if (coverUrl == url) {
                            coverTexture = location
                        }
                    }.onFailure {
                        LOGGER.warn("[Spotify][GUI] Failed to upload album art from $url", it)
                    }
                }
            }.onFailure {
                LOGGER.warn("[Spotify][GUI] Failed to load album art from $url", it)
            }
        }
    }

    private fun ensureSuccess(response: Response) {
        if (!response.isSuccessful) {
            throw IOException("HTTP ${'$'}{response.code} while loading cover art")
        }
    }

    companion object {
        private const val BUTTON_BACK = 0
        private const val BUTTON_SAVE = 1
        private const val BUTTON_BROWSER = 2
        private const val BUTTON_MODE = 3
        private const val BUTTON_AUTO_RECONNECT = 4
        private const val BUTTON_POLL_INFO = 5
        private const val BUTTON_POLL_DOWN = 6
        private const val BUTTON_POLL_UP = 7
        private const val BUTTON_TOGGLE = 8
        private const val BUTTON_DASHBOARD = 9
        private const val BUTTON_GUIDE = 10
        private const val BUTTON_PLAYER = 11
    }
}