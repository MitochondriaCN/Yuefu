package com.xianliticn.yuefu.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dom4j.Document
import org.dom4j.io.SAXReader
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

suspend fun readXml(file: File): Document =
    withContext(Dispatchers.IO) {
        val reader = SAXReader()
        return@withContext reader.read(file)
    }


suspend fun Document.getTitle(): String? =
    withContext(Dispatchers.IO) {
        //尝试直接获取movement-title
        val movementTitle = this@getTitle.selectSingleNode("//score-partwise/movement-title")?.text
        if (!movementTitle.isNullOrBlank()) {
            return@withContext movementTitle
        }

        //如果没有，尝试获取work/work-title
        val workTitle = this@getTitle.selectSingleNode("//score-partwise/work/work-title")?.text
        if (!workTitle.isNullOrBlank()) {
            return@withContext workTitle
        }

        return@withContext null
    }


suspend fun Document.getAuthor(): String? =
    withContext(Dispatchers.IO) {
        val composer =
            this@getAuthor.selectSingleNode("//score-partwise/identification/creator[@type='composer']")?.text
        if (!composer.isNullOrBlank()) {
            return@withContext composer
        }

        //如果没有明确标记为composer的，尝试获取任意creator节点
        val creator =
            this@getAuthor.selectSingleNode("//score-partwise/identification/creator")?.text
        if (!creator.isNullOrBlank()) {
            return@withContext creator
        }

        return@withContext null
    }
