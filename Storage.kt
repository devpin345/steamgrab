package com.example.steamgrab

import android.os.Build
import android.os.Environment
import java.io.File

object Storage {
    // Bisa dibuka dari PC lewat USB karena berada di folder publik Download
    fun steamLibraryDir(): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOAD),
        "SteamGames/steamapps"
    )

    fun gameDir(installDir: String): File = File(steamLibraryDir(), "common/$installDir")

    fun manifestFile(appId: Int): File = File(steamLibraryDir(), "appmanifest_$appId.acf")

    fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
}
