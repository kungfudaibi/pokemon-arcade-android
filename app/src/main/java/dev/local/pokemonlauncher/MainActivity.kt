package dev.local.pokemonlauncher

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import net.pokerogue.livewrapper.MainActivity as RogueActivity
import java.io.File

class MainActivity : ComponentActivity() {
    private val library by lazy { RomLibrary(this) }
    private val ink = Color.rgb(33, 39, 46)
    private val panel = Color.WHITE
    private val white = Color.rgb(33, 39, 46)
    private val muted = Color.rgb(127, 135, 143)
    private val mint = Color.rgb(242, 112, 52)
    private val canvas = Color.rgb(247, 248, 250)
    private var selectedGame: Game? = null
    private fun pendingGame(): Game? = selectedGame ?: library.game(
        getPreferences(MODE_PRIVATE).getString("pendingSaveGame", "") ?: ""
    )

    private val chooseRom = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        Thread {
            val result = runCatching { library.importRom(uri) }
            runOnUiThread {
                result.onSuccess { game ->
                    getPreferences(MODE_PRIVATE).edit().putString("lastGame", game.id).apply()
                    render()
                    if (File(game.path).length() > 32L * 1024 * 1024)
                        message("已导入 ${game.title}；此 ROM 超过 32 MB，当前核心不保证能运行该改版")
                    else message("已加入游戏库：${game.title}")
                }.onFailure { message(it.message ?: "导入失败") }
            }
        }.start()
    }
    private val chooseSave = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val game = pendingGame() ?: return@registerForActivityResult
        if (uri == null) return@registerForActivityResult
        Thread {
            val result = runCatching { library.importSave(game, uri) }
            runOnUiThread {
                result.onSuccess { message("存档文件已导入并备份旧存档；请重新进入游戏确认能否读取。") }
                    .onFailure { message(it.message ?: "导入存档失败") }
                render()
            }
        }.start()
    }
    private val exportSave = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val game = pendingGame() ?: return@registerForActivityResult
        if (uri == null) return@registerForActivityResult
        runCatching {
            val save = library.saveFile(game)
            require(save.exists()) { "这款游戏还没有普通存档" }
            contentResolver.openOutputStream(uri)?.use { output -> save.inputStream().use { it.copyTo(output) } }
                ?: error("无法写入目标文件")
        }.onSuccess { message("存档已导出") }.onFailure { message(it.message ?: "导出失败") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = canvas
        window.navigationBarColor = canvas
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(canvas) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(32))
        }
        scroll.addView(column)
        setContentView(scroll)

        label(column, "本机游戏", 30, white, true)
        val games = library.all()
        label(column, "${games.size} 款 GBA 游戏 · 2 款在线游戏", 13, muted)
        action(column, "＋  导入 GBA 游戏", true) { chooseRom.launch(arrayOf("*/*")) }
        section(column, "GBA", "本机")
        if (games.isEmpty()) {
            val empty = card(column, panel, dp(14))
            label(empty, "游戏会出现在这里", 17, white, true)
            label(empty, "导入自己的 .gba 或 .zip 游戏文件即可开始。", 13, muted)
        }
        games.forEach { game ->
            val item = card(column, panel, dp(15))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            item.addView(row)
            row.addView(TextView(this).apply {
                text = "GBA"; textSize = 17f; setTypeface(null, Typeface.BOLD)
                setTextColor(mint); gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.rgb(255, 239, 225)); cornerRadius = dp(12).toFloat()
                }
            }, LinearLayout.LayoutParams(dp(60), dp(60)))
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(13), 0, 0, 0) }
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            label(details, game.title, 16, white, true).maxLines = 2
            label(details, if (library.saveFile(game).exists()) "GBA · 有存档" else "GBA · 尚无存档", 12, muted)
            smallAction(row, "启动") { openGame(game) }
            val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            item.addView(tools, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
            textAction(tools, "存档导入／导出", dp(73)) { showSaveActions(game) }
            textAction(tools, "游戏管理", 0) { showGameActions(game) }
        }

        section(column, "在线游戏", "内置 · 需要联网")
        appCard(column, R.drawable.showdown_icon, "Pokémon Showdown", "对战、配队与观战", dev.local.showdownnative.MainActivity::class.java)
        appCard(column, R.drawable.rogue_icon, "PokéRogue", "实时更新的肉鸽冒险", RogueActivity::class.java)
        label(column, "GBA 游戏使用 mGBA 核心", 11, muted).apply {
            setPadding(0, dp(15), 0, 0)
        }
    }

    private fun openGame(game: Game) {
        getPreferences(MODE_PRIVATE).edit().putString("lastGame", game.id).apply()
        startActivity(Intent(this, GameActivity::class.java).putExtra("gameId", game.id))
    }

    private fun showSaveActions(game: Game) {
        selectedGame = game
        getPreferences(MODE_PRIVATE).edit().putString("pendingSaveGame", game.id).apply()
        val options = arrayOf("导入普通存档 (.sav)", "导出普通存档 (.sav)")
        android.app.AlertDialog.Builder(this).setTitle(game.title).setItems(options) { _, index ->
            if (index == 0) chooseSave.launch(arrayOf("*/*"))
            else if (library.saveFile(game).exists()) exportSave.launch("${game.title}.sav")
            else message("这款游戏还没有普通存档")
        }.show()
    }

    private fun showGameActions(game: Game) {
        android.app.AlertDialog.Builder(this).setTitle(game.title)
            .setItems(arrayOf("修改游戏名称", "从游戏馆移出（保留文件与存档）")) { _, index ->
                if (index == 0) {
                    val input = EditText(this).apply { setText(game.title); selectAll() }
                    android.app.AlertDialog.Builder(this).setTitle("修改游戏名称")
                        .setView(input).setNegativeButton("取消", null)
                        .setPositiveButton("保存") { _, _ ->
                            runCatching { library.rename(game, input.text.toString()) }
                                .onSuccess { render() }.onFailure { message(it.message ?: "修改失败") }
                        }.show()
                } else {
                    android.app.AlertDialog.Builder(this).setMessage("只从列表移出 ${game.title}。ROM 和存档仍保留；重新导入同一 ROM 可恢复。")
                        .setNegativeButton("取消", null).setPositiveButton("移出") { _, _ ->
                            library.removeFromLibrary(game)
                            render()
                        }.show()
                }
            }.show()
    }

    private fun appCard(parent: LinearLayout, icon: Int, title: String, subtitle: String, activity: Class<*>) {
        val line = card(parent, panel, dp(12))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        line.addView(row)
        row.addView(ImageView(this).apply { setImageResource(icon); scaleType = ImageView.ScaleType.FIT_CENTER },
            LinearLayout.LayoutParams(dp(50), dp(50)))
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        label(copy, title, 17, white, true)
        label(copy, subtitle, 12, muted)
        action(line, "启动", false) { startActivity(Intent(this, activity)) }
    }

    private fun section(parent: LinearLayout, title: String, aside: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22); bottomMargin = dp(9) })
        label(row, title, 20, white, true).layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        label(row, aside, 12, muted)
    }

    private fun card(parent: LinearLayout, color: Int, padding: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            background = GradientDrawable().apply {
                setColor(color); cornerRadius = dp(15).toFloat()
                setStroke(dp(1), Color.rgb(230, 233, 236))
            }
        }
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
        return card
    }

    private fun action(parent: LinearLayout, title: String, strong: Boolean, weight: Float? = null, click: () -> Unit) {
        val button = TextView(this).apply {
            text = title; textSize = 14f; setTypeface(null, Typeface.BOLD)
            setTextColor(if (strong) Color.WHITE else ink)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(if (strong) mint else Color.rgb(240, 242, 244)); cornerRadius = dp(11).toFloat()
            }
            setOnClickListener { click() }
        }
        val params = if (weight == null) LinearLayout.LayoutParams(-1, dp(48))
            else LinearLayout.LayoutParams(0, dp(46), weight)
        params.topMargin = dp(11)
        if (weight != null) params.marginEnd = dp(5)
        parent.addView(button, params)
    }

    private fun smallAction(parent: LinearLayout, title: String, click: () -> Unit) {
        val button = TextView(this).apply {
            text = title; textSize = 14f; setTypeface(null, Typeface.BOLD)
            setTextColor(mint); gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(9).toFloat()
                setStroke(dp(1), mint)
            }
            setOnClickListener { click() }
        }
        parent.addView(button, LinearLayout.LayoutParams(dp(66), dp(42)))
    }

    private fun textAction(parent: LinearLayout, title: String, leftPadding: Int, click: () -> Unit) {
        parent.addView(TextView(this).apply {
            text = title; textSize = 12f; setTextColor(muted)
            setPadding(leftPadding, dp(5), dp(14), dp(5))
            setOnClickListener { click() }
        })
    }

    private fun label(parent: LinearLayout, value: String, size: Int, color: Int, bold: Boolean = false): TextView {
        val view = TextView(this).apply {
            text = value; textSize = size.toFloat(); setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
        parent.addView(view)
        return view
    }

    private fun message(text: String) = android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
