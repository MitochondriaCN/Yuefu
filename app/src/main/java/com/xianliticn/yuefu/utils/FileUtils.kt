package com.xianliticn.yuefu.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

suspend fun File.getHash(): String =
    withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        this@getHash.inputStream().use {
            val byteArray = ByteArray(1024 * 8) //8KB缓冲
            var bytesCount: Int
            while (it.read(byteArray).also { bytesCount = it } != -1) {
                digest.update(byteArray, 0, bytesCount)
            }
            val bytes = digest.digest()
            bytes.toHexString()
        }
    }