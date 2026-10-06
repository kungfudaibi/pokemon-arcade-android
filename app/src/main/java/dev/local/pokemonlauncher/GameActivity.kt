package dev.local.pokemonlauncher

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.text.InputType
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.AspectRatioGLSurfaceView
import com.swordfish.libretrodroid.LibretroDroid
import com.swordfish.libretrodroid.ShaderConfig
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class GameActivity : ComponentActivity() {
    private lateinit var game: Game
    private lateinit var retro: GLRetroView
    private lateinit var library: RomLibrary
    private lateinit var root: FrameLayout
    private lateinit var gameFrame: FrameLayout
    private lateinit var dpad: FrameLayout
    private lateinit var actions: FrameLayout
    private lateinit var leftShoulder: TextView
    private lateinit var rightShoulder: TextView
    private lateinit var startSelect: LinearLayout
    private lateinit var menuButton: TextView
    private lateinit var speedButton: TextView
    private val turboHandler = Handler(Looper.getMainLooper())
    private val turboButtons = mutableMapOf<View, Pair<Int, Runnable>>()
    private var ready = false
    private val preferences by lazy { getSharedPreferences("emulator", MODE_PRIVATE) }
    private val ink = Color.rgb(13, 21, 27)
    private val accent = Color.rgb(115, 226, 187)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        library = RomLibrary(this)
        game = library.game(intent.getStringExtra("gameId") ?: "") ?: run { finish(); return }
        retro = GLRetroView(this, GLRetroViewData(this).apply {
            coreFilePath = "libmgba_libretro_android.so"
            gameFilePath = game.path
            systemDirectory = filesDir.absolutePath
            savesDirectory = File(filesDir, "gba").absolutePath
            saveRAMState = library.saveFile(game).takeIf { it.exists() }?.readBytes()
            shader = shaderFromIndex(preferences.getInt("shader", 0))
            preferLowLatencyAudio = true
        })
        retro.setResizeMode(AspectRatioGLSurfaceView.RESIZE_MODE_FILL)
        lifecycle.addObserver(retro)
        buildScreen()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = showMenu()
        })
        lifecycleScope.launch {
            retro.getGLRetroEvents().collect { event ->
                if (event is GLRetroView.GLRetroEvents.FrameRendered && !ready) {
                    ready = true
                    retro.frameSpeed = preferences.getInt("speed", 1)
                    updateSpeedButton()
                    retro.audioEnabled = preferences.getBoolean("audio", true)
                    applyCheats()
                }
            }
        }
        lifecycleScope.launch {
            retro.getGLRetroErrors().collect { error -> message("模拟器加载失败（错误 $error）") }
        }
    }

    private fun buildScreen() {
        root = FrameLayout(this).apply { setBackgroundColor(ink) }
        setContentView(root)
        gameFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(retro, FrameLayout.LayoutParams(-1, -1))
        }
        root.addView(gameFrame)
        dpad = FrameLayout(this)
        root.addView(dpad)
        val arrows = listOf(
            Triple("↑", KeyEvent.KEYCODE_DPAD_UP, 1 to 0),
            Triple("←", KeyEvent.KEYCODE_DPAD_LEFT, 0 to 1),
            Triple("→", KeyEvent.KEYCODE_DPAD_RIGHT, 2 to 1),
            Triple("↓", KeyEvent.KEYCODE_DPAD_DOWN, 1 to 2)
        )
        arrows.forEach { (label, code, position) ->
            dpad.addView(touchButton(label, code, false), FrameLayout.LayoutParams(dp(46), dp(46)).apply {
                leftMargin = dp(position.first * 46)
                topMargin = dp(position.second * 46)
            })
        }
        actions = FrameLayout(this)
        root.addView(actions)
        actions.addView(touchButton("B", KeyEvent.KEYCODE_BUTTON_B, true),
            FrameLayout.LayoutParams(dp(58), dp(58)).apply { leftMargin = dp(5); topMargin = dp(67) })
        actions.addView(touchButton("A", KeyEvent.KEYCODE_BUTTON_A, true),
            FrameLayout.LayoutParams(dp(58), dp(58)).apply { leftMargin = dp(76); topMargin = dp(23) })
        updateTurboLabels()
        leftShoulder = touchButton("L", KeyEvent.KEYCODE_BUTTON_L1, false)
        rightShoulder = touchButton("R", KeyEvent.KEYCODE_BUTTON_R1, false)
        root.addView(leftShoulder)
        root.addView(rightShoulder)
        startSelect = LinearLayout(this).apply { gravity = Gravity.CENTER }
        root.addView(startSelect)
        startSelect.addView(touchButton("SELECT", KeyEvent.KEYCODE_BUTTON_SELECT, false),
            LinearLayout.LayoutParams(dp(74), dp(38)).apply { marginEnd = dp(12) })
        startSelect.addView(touchButton("START", KeyEvent.KEYCODE_BUTTON_START, false),
            LinearLayout.LayoutParams(dp(74), dp(38)))
        menuButton = TextView(this).apply {
            text = "☰"
            textSize = 23f
            setTextColor(accent)
            gravity = Gravity.CENTER
            background = buttonBackground(false)
            contentDescription = "游戏菜单"
            setOnClickListener { showMenu() }
        }
        root.addView(menuButton, FrameLayout.LayoutParams(dp(48), dp(42), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(6)
        })
        speedButton = TextView(this).apply {
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = buttonBackground(false)
            contentDescription = "切换游戏速度"
            setOnClickListener { showSpeedSelector() }
        }
        root.addView(speedButton)
        updateSpeedButton()
        root.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            layoutScreen(right - left, bottom - top)
        }
        applyControlStyle()
    }

    private fun layoutScreen(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val controlsVisible = preferences.getBoolean("controls", true)
        val fill = preferences.getBoolean("fillScreen", true)
        val frameWidth = if (fill) width else min(width, (height * 1.5f).toInt())
        gameFrame.layoutParams = FrameLayout.LayoutParams(frameWidth, height,
            Gravity.CENTER).apply { }
        retro.setResizeMode(AspectRatioGLSurfaceView.RESIZE_MODE_FILL)
        // Shoulder keys own the outer corners; utility controls sit inside them.
        menuButton.layoutParams = FrameLayout.LayoutParams(dp(54), dp(42), Gravity.TOP or Gravity.LEFT)
            .apply { leftMargin = dp(132); topMargin = dp(12) }
        speedButton.layoutParams = FrameLayout.LayoutParams(dp(92), dp(42), Gravity.TOP or Gravity.RIGHT)
            .apply { rightMargin = dp(132); topMargin = dp(12) }
        val visible = if (controlsVisible) View.VISIBLE else View.GONE
        listOf(dpad, actions, leftShoulder, rightShoulder, startSelect).forEach { it.visibility = visible }
        if (!controlsVisible) return
        dpad.layoutParams = FrameLayout.LayoutParams(dp(138), dp(138), Gravity.LEFT or Gravity.BOTTOM).apply {
            leftMargin = dp(7); bottomMargin = dp(48)
        }
        actions.layoutParams = FrameLayout.LayoutParams(dp(138), dp(138), Gravity.RIGHT or Gravity.BOTTOM).apply {
            rightMargin = dp(7); bottomMargin = dp(48)
        }
        leftShoulder.layoutParams = FrameLayout.LayoutParams(dp(92), dp(42), Gravity.TOP or Gravity.LEFT).apply {
            leftMargin = dp(24); topMargin = dp(12)
        }
        rightShoulder.layoutParams = FrameLayout.LayoutParams(dp(92), dp(42), Gravity.TOP or Gravity.RIGHT).apply {
            rightMargin = dp(24); topMargin = dp(12)
        }
        startSelect.layoutParams = FrameLayout.LayoutParams(-2, dp(42),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(5) }
    }

    private fun touchButton(label: String, code: Int, round: Boolean): TextView = TextView(this).apply {
        text = label
        textSize = if (label.length > 2) 11f else 21f
        setTextColor(Color.WHITE)
        setTypeface(null, Typeface.BOLD)
        gravity = Gravity.CENTER
        contentDescription = label
        isClickable = true
        alpha = 1f
        background = buttonBackground(round)
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (isTurboEnabled(code)) startTurbo(view, code)
                    else if (ready) retro.sendKeyEvent(KeyEvent.ACTION_DOWN, code)
                    view.alpha = .7f
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopTurbo(view, code)
                    view.alpha = 1f
                    true
                }
                else -> true
            }
        }
    }

    private fun isTurboEnabled(code: Int): Boolean {
        val mode = preferences.getInt("turboMode", 1)
        return when (code) {
            KeyEvent.KEYCODE_BUTTON_A -> mode == 1 || mode == 3
            KeyEvent.KEYCODE_BUTTON_B -> mode == 2 || mode == 3
            else -> false
        }
    }

    private fun updateTurboLabels() {
        if (!::actions.isInitialized || actions.childCount < 2) return
        (actions.getChildAt(0) as TextView).apply {
            val turbo = isTurboEnabled(KeyEvent.KEYCODE_BUTTON_B)
            text = if (turbo) "B↻" else "B"
            contentDescription = if (turbo) "B 键，长按连按" else "B 键"
        }
        (actions.getChildAt(1) as TextView).apply {
            val turbo = isTurboEnabled(KeyEvent.KEYCODE_BUTTON_A)
            text = if (turbo) "A↻" else "A"
            contentDescription = if (turbo) "A 键，长按连按" else "A 键"
        }
    }

    private fun startTurbo(view: View, code: Int) {
        if (!ready) return
        stopTurbo(view, code)
        val halfPeriodMs = (500L / preferences.getInt("turboRate", 8)).coerceAtLeast(25L)
        var pressed = true
        retro.sendKeyEvent(KeyEvent.ACTION_DOWN, code)
        val pulse = object : Runnable {
            override fun run() {
                if (turboButtons[view]?.second !== this || !ready) return
                pressed = !pressed
                retro.sendKeyEvent(if (pressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, code)
                turboHandler.postDelayed(this, halfPeriodMs)
            }
        }
        turboButtons[view] = code to pulse
        // A quick tap is one ordinary press. Repeat only after a deliberate hold.
        turboHandler.postDelayed(pulse, 300L)
    }

    private fun stopTurbo(view: View, code: Int) {
        turboButtons.remove(view)?.let { turboHandler.removeCallbacks(it.second) }
        if (ready) retro.sendKeyEvent(KeyEvent.ACTION_UP, code)
    }

    private fun buttonBackground(round: Boolean) = GradientDrawable().apply {
        setColor(Color.argb(176, 24, 35, 44))
        cornerRadius = dp(if (round) 40 else 13).toFloat()
        setStroke(dp(1), Color.rgb(100, 140, 145))
    }

    private fun applyControlStyle() {
        val scale = preferences.getInt("controlScale", 90) / 100f
        val opacity = preferences.getInt("controlOpacity", 85) / 100f
        listOf(dpad, actions, leftShoulder, rightShoulder, startSelect).forEach {
            it.scaleX = scale
            it.scaleY = scale
            it.alpha = opacity
        }
    }

    private fun showControlSettings() {
        val scale = preferences.getInt("controlScale", 90)
        val opacity = preferences.getInt("controlOpacity", 85)
        dialog().setTitle("触屏按键")
            .setItems(arrayOf("按键大小：$scale%", "按键透明度：$opacity%")) { _, index ->
                if (index == 0) {
                    val values = intArrayOf(80, 90, 100, 110, 120)
                    dialog().setTitle("按键大小")
                        .setSingleChoiceItems(values.map { "$it%" }.toTypedArray(), values.indexOf(scale)) { dialog, choice ->
                            preferences.edit().putInt("controlScale", values[choice]).apply()
                            applyControlStyle()
                            dialog.dismiss()
                        }.show()
                } else {
                    val values = intArrayOf(55, 70, 85, 100)
                    dialog().setTitle("按键透明度")
                        .setSingleChoiceItems(values.map { "$it%" }.toTypedArray(), values.indexOf(opacity)) { dialog, choice ->
                            preferences.edit().putInt("controlOpacity", values[choice]).apply()
                            applyControlStyle()
                            dialog.dismiss()
                        }.show()
                }
            }.show()
    }

    private fun showCheatList() {
        val cheats = library.cheats(game)
        val labels = arrayOf("＋ 添加代码") + cheats.map {
            "${if (it.enabled) "●" else "○"} ${it.name}  ·  ${if (it.enabled) "已启用" else "已停用"}"
        }
        dialog().setTitle("金手指 · ${game.title}")
            .setMessage("支持每行 8+4 或 8+8 位十六进制代码。代码必须对应这款 ROM 改版；已加载不代表在游戏内一定有效。")
            .setItems(labels) { _, index ->
                if (index == 0) editCheat(null) else showCheatActions(index - 1)
            }.show()
    }

    private fun showCheatActions(index: Int) {
        val cheats = library.cheats(game)
        val current = cheats.getOrNull(index) ?: return
        val options = arrayOf(if (current.enabled) "停用" else "启用", "编辑代码", "删除代码")
        dialog().setTitle(current.name).setItems(options) { _, choice ->
            when (choice) {
                0 -> {
                    val updated = cheats.toMutableList()
                    updated[index] = current.copy(enabled = !current.enabled)
                    library.saveCheats(game, updated)
                    applyCheats()
                    showCheatList()
                }
                1 -> editCheat(index)
                2 -> dialog().setMessage("删除「${current.name}」？")
                    .setNegativeButton("取消", null).setPositiveButton("删除") { _, _ ->
                        library.saveCheats(game, cheats.filterIndexed { i, _ -> i != index })
                        applyCheats()
                        showCheatList()
                    }.show()
            }
        }.show()
    }

    private fun editCheat(index: Int?) {
        val original = index?.let { library.cheats(game).getOrNull(it) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), 0)
        }
        val nameField = EditText(this).apply {
            hint = "名称，例如：道具数量"
            setSingleLine(true)
            setText(original?.name ?: "")
        }
        val codeField = EditText(this).apply {
            hint = "每行一组代码，例如 12345678 1234"
            minLines = 3
            maxLines = 6
            setText(original?.code?.replace("+", "\n") ?: "")
        }
        column.addView(nameField)
        column.addView(codeField)
        val dialog = dialog()
            .setTitle(if (index == null) "添加金手指" else "编辑金手指")
            .setView(column)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    val name = nameField.text.toString().trim()
                    require(name.isNotEmpty() && name.length <= 40) { "请输入 1 到 40 字的名称" }
                    val code = normalizeCheat(codeField.text.toString())
                    val updated = library.cheats(game).toMutableList()
                    val entry = Cheat(name, code, original?.enabled ?: true)
                    if (index == null) updated.add(entry) else updated[index] = entry
                    library.saveCheats(game, updated)
                    applyCheats()
                }.onSuccess {
                    dialog.dismiss()
                    message("代码已送入核心，请在游戏内核对效果")
                }.onFailure { codeField.error = it.message }
            }
        }
        dialog.show()
    }

    private fun normalizeCheat(raw: String): String {
        val lines = raw.split(Regex("[+\\r\\n]+")).map { it.replace(Regex("\\s+"), "").uppercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
        require(lines.isNotEmpty() && lines.size <= 20) { "请输入 1 到 20 行代码" }
        require(lines.all { (it.length == 12 || it.length == 16) && it.all { c -> c in '0'..'9' || c in 'A'..'F' } }) {
            "每行应为 8+4 或 8+8 位十六进制字符"
        }
        return lines.joinToString("+") { it.substring(0, 8) + " " + it.substring(8) }
    }

    private fun applyCheats() {
        if (!ready) return
        val enabled = library.cheats(game).filter { it.enabled }
        retro.queueEvent {
            runCatching {
                LibretroDroid.resetCheat()
                enabled.forEachIndexed { index, cheat -> LibretroDroid.setCheat(index, true, cheat.code) }
            }.onFailure { error ->
                runOnUiThread { message("代码应用失败：${error.message}") }
            }
        }
    }

    private fun showMenu() {
        dialog().setTitle(game.title).setItems(arrayOf(
            "继续游戏", "即时存档与读取", "游戏设置", "金手指", "重新开始", "返回游戏馆"
        )) { _, index ->
            when (index) {
                1 -> dialog().setTitle("即时进度")
                    .setItems(arrayOf("保存到槽位…", "从槽位读取…")) { _, action ->
                        selectStateSlot(action == 0)
                    }.show()
                2 -> showSettings()
                3 -> showCheatList()
                4 -> dialog().setMessage("重新开始会丢失当前尚未保存的进度。")
                    .setNegativeButton("取消", null).setPositiveButton("重新开始") { _, _ ->
                        if (ready) retro.reset()
                    }.show()
                5 -> finish()
            }
        }.show()
    }

    private fun showSettings() {
        val speed = preferences.getInt("speed", 1)
        val controls = preferences.getBoolean("controls", true)
        val audio = preferences.getBoolean("audio", true)
        val turboMode = preferences.getInt("turboMode", 1)
        dialog().setTitle("游戏设置").setItems(arrayOf(
            "运行速度：${speed}×",
            "画面比例：${if (preferences.getBoolean("fillScreen", true)) "铺满屏幕" else "原始 3:2"}",
            "画面滤镜",
            "音效：${if (audio) "开" else "关"}",
            "触屏按键：${if (controls) "显示" else "隐藏"}",
            "按键大小与透明度",
            "长按连按：${arrayOf("关闭", "A", "B", "A + B").getOrElse(turboMode) { "A" }}"
        )) { _, index ->
            when (index) {
                0 -> showSpeedSelector()
                1 -> showDisplaySelector()
                2 -> selectShader()
                3 -> {
                    retro.audioEnabled = !audio
                    preferences.edit().putBoolean("audio", !audio).apply()
                }
                4 -> {
                    preferences.edit().putBoolean("controls", !controls).apply()
                    layoutScreen(root.width, root.height)
                }
                5 -> showControlSettings()
                6 -> showTurboSettings()
            }
        }.show()
    }

    private fun showTurboSettings() {
        val mode = preferences.getInt("turboMode", 1)
        val rate = preferences.getInt("turboRate", 8)
        dialog().setTitle("长按连按")
            .setMessage("轻点按一次；按住约 0.3 秒后自动连按，松手停止。A 键默认开启，B 键可单独开启。")
            .setItems(arrayOf("关闭", "A 键连按", "B 键连按", "A、B 键都连按", "连按频率：每秒 $rate 次")) { _, choice ->
                if (choice < 4) {
                    preferences.edit().putInt("turboMode", choice).apply()
                    updateTurboLabels()
                    message(if (choice == 0) "已关闭长按连按" else "轻点单次，长按约 0.3 秒后连按")
                } else {
                    val rates = intArrayOf(6, 8, 10, 12)
                    dialog().setTitle("连按频率")
                        .setSingleChoiceItems(rates.map { "每秒 $it 次" }.toTypedArray(), rates.indexOf(rate)) { picker, index ->
                            preferences.edit().putInt("turboRate", rates[index]).apply()
                            picker.dismiss()
                        }.show()
                }
            }.show()
    }

    private fun updateSpeedButton() {
        if (::speedButton.isInitialized) speedButton.text = "快进 ${preferences.getInt("speed", 1)}×"
    }

    private fun showSpeedSelector() {
        val speeds = intArrayOf(1, 2, 4, 8)
        dialog().setTitle("游戏速度")
            .setItems(arrayOf("1× · 正常", "2×", "4×", "8×", "自定义整数倍…")) { _, index ->
                if (index < speeds.size) setSpeed(speeds[index]) else showCustomSpeed()
            }.show()
    }

    private fun showCustomSpeed() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(preferences.getInt("speed", 1).toString())
            selectAll()
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }
        val picker = dialog().setTitle("自定义倍速")
            .setMessage("输入 1–16 的整数。实际速度受手机性能和游戏改版影响。")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("应用", null)
            .create()
        picker.setOnShowListener {
            picker.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val speed = input.text.toString().toIntOrNull()
                if (speed == null || speed !in 1..16) {
                    input.error = "请输入 1–16 的整数"
                } else {
                    setSpeed(speed)
                    picker.dismiss()
                }
            }
        }
        picker.show()
    }

    private fun setSpeed(speed: Int) {
        preferences.edit().putInt("speed", speed).apply()
        if (ready) retro.frameSpeed = speed
        updateSpeedButton()
        message("目标速度 ${speed}×")
    }

    private fun showDisplaySelector() {
        val fill = preferences.getBoolean("fillScreen", true)
        dialog().setTitle("画面比例")
            .setSingleChoiceItems(arrayOf("铺满屏幕 · 宽屏会拉伸画面", "原始 3:2 · 不拉伸"), if (fill) 0 else 1) { dialog, index ->
                preferences.edit().putBoolean("fillScreen", index == 0).apply()
                layoutScreen(root.width, root.height)
                dialog.dismiss()
            }.show()
    }

    private fun dialog(): android.app.AlertDialog.Builder =
        android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)

    private fun selectStateSlot(save: Boolean) {
        val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val labels = (1..5).map { slot ->
            val file = library.stateFile(game, slot)
            "槽位 $slot  ·  ${if (file.exists()) format.format(Date(file.lastModified())) else "空"}"
        }.toTypedArray()
        dialog().setTitle(if (save) "保存即时进度" else "读取即时进度")
            .setItems(labels) { _, index ->
                val slot = index + 1
                if (save) saveState(slot) else loadState(slot)
            }.show()
    }

    private fun saveState(slot: Int) {
        if (!ready) { message("游戏尚未加载完成"); return }
        val file = library.stateFile(game, slot)
        val doSave = {
            runCatching {
                val state = retro.serializeState()
                require(state.isNotEmpty()) { "模拟核心未返回进度" }
                atomicWrite(file, state)
            }.onSuccess { message("已保存到槽位 $slot") }
                .onFailure { message("保存失败：${it.message}") }
        }
        if (file.exists()) dialog().setMessage("覆盖槽位 $slot 中的即时进度？")
            .setNegativeButton("取消", null).setPositiveButton("覆盖") { _, _ -> doSave() }.show()
        else doSave()
    }

    private fun loadState(slot: Int) {
        if (!ready) { message("游戏尚未加载完成"); return }
        val file = library.stateFile(game, slot)
        if (!file.exists()) { message("槽位 $slot 还没有进度"); return }
        dialog().setMessage("读取槽位 $slot 会替换当前未保存的进度。")
            .setNegativeButton("取消", null).setPositiveButton("读取") { _, _ ->
                runCatching { retro.unserializeState(file.readBytes()) }
                    .onSuccess { message(if (it) "已读取槽位 $slot" else "进度与游戏不兼容") }
                    .onFailure { message("读取失败：${it.message}") }
            }.show()
    }

    private fun selectShader() {
        val labels = arrayOf("像素清晰", "平滑", "CRT 扫描线", "LCD 网格")
        dialog().setTitle("画面滤镜")
            .setSingleChoiceItems(labels, preferences.getInt("shader", 0)) { dialog, index ->
                preferences.edit().putInt("shader", index).apply()
                retro.shader = shaderFromIndex(index)
                dialog.dismiss()
            }.show()
    }

    private fun shaderFromIndex(index: Int): ShaderConfig = when (index) {
        1 -> ShaderConfig.Default
        2 -> ShaderConfig.CRT
        3 -> ShaderConfig.LCD
        else -> ShaderConfig.Sharp
    }

    private fun atomicWrite(target: File, data: ByteArray) {
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.outputStream().use { output -> output.write(data); output.fd.sync() }
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    override fun onPause() {
        turboButtons.toMap().forEach { (view, button) -> stopTurbo(view, button.first) }
        if (::retro.isInitialized && ready) runCatching {
            val bytes = retro.serializeSRAM()
            if (bytes.isNotEmpty()) atomicWrite(library.saveFile(game), bytes)
        }.onFailure { android.util.Log.e("PokemonLauncher", "SaveRAM failed", it) }
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (::retro.isInitialized && event.isGamepadButton()) {
            if (ready) retro.sendKeyEvent(KeyEvent.ACTION_DOWN, mappedGamepadKey(keyCode))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (::retro.isInitialized && event.isGamepadButton()) {
            if (ready) retro.sendKeyEvent(KeyEvent.ACTION_UP, mappedGamepadKey(keyCode))
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (::retro.isInitialized && ready &&
            event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            retro.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD,
                event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y))
            retro.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y))
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun KeyEvent.isGamepadButton(): Boolean =
        source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            source and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD

    private fun mappedGamepadKey(code: Int): Int = when (code) {
        KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_BUTTON_B
        KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BUTTON_A
        KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_BUTTON_Y
        KeyEvent.KEYCODE_BUTTON_Y -> KeyEvent.KEYCODE_BUTTON_X
        else -> code
    }

    private fun message(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

