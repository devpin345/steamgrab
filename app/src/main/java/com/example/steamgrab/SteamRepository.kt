package com.example.steamgrab

import in.dragonbra.javasteam.steam.client.SteamClient
import in.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import in.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import in.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import in.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import in.dragonbra.javasteam.depotdownloader.DepotDownloader
import in.dragonbra.javasteam.depotdownloader.IDownloadListener
import in.dragonbra.javasteam.depotdownloader.data.AppItem
import in.dragonbra.javasteam.steam.handlers.steamapps.License
import kotlinx.coroutines.delay

class SteamRepository {
    private val steamClient = SteamClient()
    private val manager = CallbackManager(steamClient)
    private var licenseList: List<License> = emptyList()
    private var running = true

    suspend fun login(
        username: String,
        password: String,
        guardCode: String?,
        onNeedGuardCode: () -> Unit,
        onError: (String) -> Unit,
        onSuccess: (List<GameInfo>) -> Unit
    ) {
        manager.subscribe(ConnectedCallback::class.java) {
            val details = LogOnDetails()
            details.username = username
            details.password = password
            if (guardCode != null) details.twoFactorCode = guardCode
            steamClient.getHandler(SteamUser::class.java)?.logOn(details)
        }

        manager.subscribe(LoggedOnCallback::class.java) { cb ->
            val result = cb.result.toString()
            when {
                result.contains("AccountLogonDenied") || result.contains("TwoFactorCodeMismatch") -> {
                    onNeedGuardCode()
                }
                result == "OK" -> {
                    // tunggu LicenseListCallback
                }
                else -> onError("Login gagal: $result")
            }
        }

        manager.subscribe(LicenseListCallback::class.java) { cb ->
            licenseList = cb.licenseList
            val games = licenseList.map { lic ->
                GameInfo(appId = lic.packageID, name = "App ${lic.packageID}", sizeLabel = "-")
            }
            onSuccess(games)
        }

        steamClient.connect()

        // loop callback — wajib jalan terus di background thread
        while (running) {
            manager.runWaitCallbacks(1000)
            delay(10)
        }
    }

    fun getCachedLicenseList(): List<GameInfo> =
        licenseList.map { GameInfo(appId = it.packageID, name = "App ${it.packageID}", sizeLabel = "-") }

    suspend fun downloadGame(
        appId: Int,
        savePath: String,
        onProgress: (Float, String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val depotDownloader = DepotDownloader(steamClient, licenseList, true)

            depotDownloader.addListener(object : IDownloadListener {
                override fun onProgress(percent: Float, bytesDownloaded: Long, totalBytes: Long) {
                    onProgress(percent, "${bytesDownloaded / 1_000_000}MB / ${totalBytes / 1_000_000}MB")
                }
                override fun onComplete(appItem: AppItem) {
                    onComplete()
                }
                override fun onError(error: Throwable) {
                    onError(error.message ?: "Unknown error")
                }
            })

            val appItem = AppItem(appId = appId, installDir = savePath)
            depotDownloader.add(appItem)
            depotDownloader.finishAdding()
            depotDownloader.awaitCompletion()
        } catch (e: Exception) {
            onError(e.message ?: "Download gagal")
        }
    }
}
