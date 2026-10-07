package dev.local.pokemonlauncher

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class Game(val id: String, val title: String, val path: String)
data class Cheat(val name: String, val code: String, val enabled: Boolean)

class RomLibrary(private val context: Context) {
    private val root = File(context.filesDir, "gba").apply { mkdirs() }
    private val catalog = File(root, "library.json")

    fun all(): List<Game> {
        if (!catalog.exists()) return emptyList()
        return try {
            val list = JSONArray(catalog.readText())
            (0 until list.length()).map { index ->
                val item = list.getJSONObject(index)
                Game(item.getString("id"), item.getString("title"), item.getString("path"))
            }.filter { File(it.path).isFile }
        } catch (_: Exception) { emptyList() }
    }

    fun game(id: String): Game? = all().firstOrNull { it.id == id }
    fun saveFile(game: Game): File = File(root, "${game.id}.sav")
    fun quickStateFile(game: Game): File = File(root, "${game.id}.quick.state")
    fun stateFile(game: Game, slot: Int = 1): File {
        require(slot in 1..5)
        val legacy = File(root, "${game.id}.state")
        return if (slot == 1 && legacy.exists()) legacy else File(root, "${game.id}.state$slot")
    }
    private fun cheatsFile(game: Game): File = File(root, "${game.id}.cheats.json")

    fun cheats(game: Game): List<Cheat> = runCatching {
        val file = cheatsFile(game)
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        (0 until array.length()).map { index ->
            val entry = array.getJSONObject(index)
            Cheat(entry.getString("name"), entry.getString("code"), entry.getBoolean("enabled"))
        }
    }.getOrDefault(emptyList())

    fun saveCheats(game: Game, cheats: List<Cheat>) {
        val array = JSONArray()
        cheats.forEach { array.put(JSONObject().put("name", it.name).put("code", it.code).put("enabled", it.enabled)) }
        val file = cheatsFile(game)
        val temporary = File(root, "${game.id}.cheats.tmp")
        temporary.writeText(array.toString())
        if (!temporary.renameTo(file)) temporary.copyTo(file, overwrite = true)
        temporary.delete()
    }

    fun rename(game: Game, title: String) {
        val clean = title.trim()
        require(clean.isNotEmpty() && clean.length <= 80) { "名称需要在 1 到 80 字之间" }
        writeCatalog(all().map { if (it.id == game.id) it.copy(title = clean) else it })
    }

    fun removeFromLibrary(game: Game) {
        writeCatalog(all().filterNot { it.id == game.id })
    }

    fun importRom(uri: Uri): Game {
        val name = displayName(uri)
        val zipped = name.endsWith(".zip", ignoreCase = true)
        require(zipped || name.endsWith(".gba", ignoreCase = true)) { "请选择 .gba 或包含它的 .zip 文件" }
        val temporary = File(root, "importing.gba")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            var romName = name
            context.contentResolver.openInputStream(uri).use { raw ->
                require(raw != null) { "无法读取游戏文件" }
                val input = if (zipped) ZipInputStream(raw).also { zip ->
                    var entry = zip.nextEntry
                    while (entry != null && !entry.name.endsWith(".gba", ignoreCase = true)) {
                        entry = zip.nextEntry
                    }
                    require(entry != null) { "ZIP 中没有 .gba 游戏文件" }
                    romName = entry.name.substringAfterLast('/')
                } else raw
                input.use { source ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var size = 0L
                        while (true) {
                            val count = source.read(buffer)
                            if (count < 0) break
                            size += count
                            require(size <= 128L * 1024 * 1024) { "游戏文件超过 128 MB" }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                        require(size >= 192) { "游戏文件太小" }
                    }
                }
            }
            val id = digest.digest().take(16).joinToString("") { "%02x".format(it) }
            val target = File(root, "$id.gba")
            if (!target.exists() && !temporary.renameTo(target)) temporary.copyTo(target, overwrite = true)
            val game = Game(id, romName.dropLast(4), target.absolutePath)
            val updated = all().filterNot { it.id == id } + game
            writeCatalog(updated)
            return game
        } finally { temporary.delete() }
    }

    fun importSave(game: Game, uri: Uri) {
        val target = saveFile(game)
        val staged = File(root, "${game.id}.incoming")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                require(input != null) { "无法读取存档文件" }
                staged.outputStream().use { output -> input.copyTo(output) }
            }
            require(staged.length() in 1..(4L * 1024 * 1024)) { "存档文件大小不符合预期" }
            if (target.exists()) target.copyTo(File(root, "${game.id}.sav.bak"), overwrite = true)
            staged.copyTo(target, overwrite = true)
        } finally { staged.delete() }
    }

    private fun writeCatalog(games: List<Game>) {
        val data = JSONArray()
        games.forEach { game ->
            data.put(JSONObject().put("id", game.id).put("title", game.title).put("path", game.path))
        }
        val temporary = File(root, "library.tmp")
        temporary.writeText(data.toString())
        if (!temporary.renameTo(catalog)) temporary.copyTo(catalog, overwrite = true)
        temporary.delete()
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0) ?: "game.gba"
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "game.gba"
    }
}
