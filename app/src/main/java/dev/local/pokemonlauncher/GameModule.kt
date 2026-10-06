package dev.local.pokemonlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class GameModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val packageName: String,
    val repository: String,
    val icon: Int,
)

data class ModuleRelease(val tag: String, val url: String, val digest: String, val size: Long)

/** Downloads separately signed APKs; Android remains responsible for installing and updating them. */
class GameModules(private val context: Context) {
    val entries = listOf(
        GameModule("showdown", "Pokémon Showdown", "对战、配队与观战", "dev.local.showdownnative", "pokemon-showdown-android-native", R.drawable.showdown_icon),
        GameModule("pokerogue", "PokéRogue", "实时更新的肉鸽冒险", "net.pokerogue.livewrapper", "pokerogue-live-android", R.drawable.rogue_icon),
    )

    fun installed(module: GameModule): PackageInfo? = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(module.packageName, 0)
    } catch (_: Exception) { null }

    fun launch(module: GameModule): Intent? = context.packageManager.getLaunchIntentForPackage(module.packageName)

    fun hasUpdate(installed: PackageInfo, release: ModuleRelease): Boolean {
        val current = installed.versionName.orEmpty().removePrefix("v").substringBefore('-')
        val available = release.tag.removePrefix("v").substringBefore('-')
        val oldParts = current.split('.').mapNotNull { it.toIntOrNull() }
        val newParts = available.split('.').mapNotNull { it.toIntOrNull() }
        if (oldParts.size != 3 || newParts.size != 3) return current != available
        for (i in 0..2) if (oldParts[i] != newParts[i]) return newParts[i] > oldParts[i]
        return false
    }

    fun latest(module: GameModule): ModuleRelease {
        val api = "https://api.github.com/repos/kungfudaibi/${module.repository}/releases?per_page=10"
        val connection = (URL(api).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "PokemonArcade-Android")
        }
        try {
            require(connection.responseCode == 200) { "版本检查失败：HTTP ${connection.responseCode}" }
            val releases = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
            for (i in 0 until releases.length()) {
                val item = releases.getJSONObject(i)
                if (item.optBoolean("draft")) continue
                val tag = item.optString("tag_name")
                val assets = item.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
                    val url = asset.optString("browser_download_url")
                    val expectedPrefix = "https://github.com/kungfudaibi/${module.repository}/releases/download/"
                    require(url.startsWith(expectedPrefix)) { "模块下载地址不符合预期" }
                    val digest = asset.optString("digest").removePrefix("sha256:")
                    require(digest.matches(Regex("[a-fA-F0-9]{64}"))) { "发布包缺少 SHA-256 校验值" }
                    val size = asset.optLong("size")
                    require(size in 1..MAX_APK_SIZE) { "发布包大小异常" }
                    return ModuleRelease(tag, url, digest.lowercase(), size)
                }
            }
            error("暂时没有可下载的 APK")
        } finally { connection.disconnect() }
    }

    fun download(module: GameModule, release: ModuleRelease, progress: (Int) -> Unit): File {
        val directory = File(context.cacheDir, "modules").apply { mkdirs() }
        val safeTag = release.tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destination = File(directory, "${module.id}-$safeTag.apk")
        if (destination.exists() && verify(destination, module, release)) return destination
        destination.delete()
        val temporary = File.createTempFile("${module.id}-", ".apk", directory)
        try {
            val connection = (URL(release.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "PokemonArcade-Android")
            }
            try {
                require(connection.responseCode == 200) { "下载失败：HTTP ${connection.responseCode}" }
                require(connection.url.protocol == "https") { "下载连接不安全" }
                var received = 0L
                var lastPercent = -1
                connection.inputStream.use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            received += count
                            require(received <= MAX_APK_SIZE) { "下载文件过大" }
                            output.write(buffer, 0, count)
                            val percent = (received * 100 / release.size).toInt().coerceIn(0, 100)
                            if (percent >= lastPercent + 5) { lastPercent = percent; progress(percent) }
                        }
                    }
                }
                require(received == release.size) { "下载不完整" }
            } finally { connection.disconnect() }
            require(verify(temporary, module, release)) { "安装包校验失败" }
            require(temporary.renameTo(destination)) { "无法保存安装包" }
            progress(100)
            return destination
        } finally { temporary.delete() }
    }

    private fun verify(file: File, module: GameModule, release: ModuleRelease): Boolean {
        if (file.length() != release.size) return false
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        val actual = hash.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        if (!actual.equals(release.digest, ignoreCase = true)) return false
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        return packageInfo?.packageName == module.packageName
    }

    companion object { private const val MAX_APK_SIZE = 100L * 1024 * 1024 }
}
