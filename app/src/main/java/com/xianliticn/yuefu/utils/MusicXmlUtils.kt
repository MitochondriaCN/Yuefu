package com.xianliticn.yuefu.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

suspend fun isValidMusicXml(file: File): Boolean =
    withContext(Dispatchers.IO) {
        try {
            file.inputStream().use { i ->
                val xmlParser = XmlPullParserFactory.newInstance().newPullParser()
                xmlParser.setInput(i, null)

                //一个标签一个标签地看
                var eventType = xmlParser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        return@withContext xmlParser.name == "score-partwise" || xmlParser.name == "score-timewise"
                    }
                    eventType = xmlParser.next()
                }
                return@withContext false
            }
        } catch (_: Exception) {
            return@withContext false
        }
    }
