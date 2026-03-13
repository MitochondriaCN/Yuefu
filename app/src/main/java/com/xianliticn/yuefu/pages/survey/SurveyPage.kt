package com.xianliticn.yuefu.pages.survey

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xianliticn.yuefu.ui.components.ChoiceGroup

@Composable
fun SurveyPage(
    viewModel: SurveyPageViewModel = hiltViewModel(),
    onFinished: (Boolean) -> Unit
) {

    val isFinished by viewModel.isFinished.collectAsState()

    LaunchedEffect(isFinished) {
        if (isFinished) {
            onFinished(true)
        }
    }

    SurveyPageContent(
        onFinished = {
            if (it.isNotEmpty()) {
                viewModel.handleFinished(it)
            } else {
                onFinished(false)
            }
        }
    )
}

/**
 * 问卷内容
 *
 * 这个函数比较乱，不过因为这项功能只用一小段时间，所以先这样了
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SurveyPageContent(
    modifier: Modifier = Modifier,
    onFinished: (Map<String, List<String>>) -> Unit = {}
) {
    var currentIndex by remember { mutableStateOf(-1) }

    val steps = getSurveySteps()
    val stepStatusMap = remember {
        mutableStateMapOf<SurveyPageStep, MutableList<String>>().apply {
            putAll(steps.associateWith { mutableStateListOf() })
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it })
                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it }))
            }
        ) { index ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = steps.getOrNull(index)?.title
                        ?: "请花费2分钟，\n帮助我们了解您的音乐背景",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = steps.getOrNull(index)?.description
                        ?: "这将有助于我们提供更加个性化的服务。我们确保这些信息仅用于研究，不会用于任何其他用途。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                steps.getOrNull(index)?.let { step ->
                    ChoiceGroup(
                        options = step.options,
                        selectedIndex = stepStatusMap[step]?.mapNotNull { optionText ->
                            step.options.indexOf(optionText).takeUnless { it == -1 }
                        }?.toIntArray() ?: intArrayOf(),
                        onOptionClick = { selectedIndex ->
                            val selectedOptionText = step.options[selectedIndex]
                            if (step.isMultipleChoice) {
                                // 多选：添加或移除选择
                                if (stepStatusMap[step]?.contains(selectedOptionText) == true) {
                                    stepStatusMap[step]?.remove(selectedOptionText)
                                } else {
                                    stepStatusMap[step]?.add(selectedOptionText)
                                }
                            } else {
                                // 单选：直接替换选择
                                stepStatusMap[step]?.clear()
                                stepStatusMap[step]?.add(selectedOptionText)
                            }
                        },
                        allowOther = step.allowOther,
                        onOtherChange = { newOtherText ->
                            val other =
                                ((stepStatusMap[step] ?: emptyList()).toSet()
                                        - step.options.toSet()).firstOrNull()
                            if (other != null)  // 存在其他项
                                stepStatusMap[step]?.remove(other)
                            stepStatusMap[step]?.add(newOtherText)
                        },
                        otherText = ((stepStatusMap[step] ?: emptyList()).toSet()
                                - step.options.toSet()).firstOrNull()
                    )
                }
                Spacer(if (index != -1) Modifier.weight(1f) else Modifier.height(32.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = {
                        if (index == steps.size - 1) {
                            onFinished(stepStatusMap.mapKeys { it.key.title })
                        } else {
                            if (currentIndex != -1
                                && stepStatusMap[steps[index]]?.isEmpty() == true
                            ) // 未选择任何选项
                                return@Button
                            currentIndex = index + 1
                        }
                    }) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = if (index == -1) "开始" else "下一步",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

private fun getSurveySteps() = listOf(
    SurveyPageStep(
        title = "您的音乐背景是？",
        description = "",
        options = listOf(
            "音乐初学者（学习时间 < 1年）",
            "业余音乐学习者（1–5年）",
            "进阶学习者 / 音乐爱好者",
            "音乐专业学生",
            "专业音乐从业者（教师 / 演奏者 / 编曲等）"
        ),
        isMultipleChoice = false,
        allowOther = false
    ),
    SurveyPageStep(
        title = "您主要使用的乐器是？",
        description = "",
        options = listOf(
            "钢琴",
            "小提琴",
            "吉他",
            "管乐器",
            "声乐",
            "作曲/编曲"
        ),
        isMultipleChoice = true,
        allowOther = true
    ),
    SurveyPageStep(
        title = "您平时获取乐谱的方式是？",
        description = "",
        options = listOf(
            "纸质教材 / 书籍",
            "PDF乐谱",
            "图片 / 拍照",
            "乐谱网站下载",
            "音乐软件自带乐谱",
            "自己编写"
        ),
        isMultipleChoice = true,
        allowOther = true
    ),
    SurveyPageStep(
        title = "当你拿到一份新乐谱时，你最希望做的事情是？",
        description = "",
        options = listOf(
            "立即试听整体效果",
            "分析旋律与节奏结构",
            "直接开始练习",
            "查看难度是否适合",
            "进行改编 / 编曲"
        ),
        isMultipleChoice = true,
        allowOther = true
    ),
    SurveyPageStep(
        title = "你在理解新乐谱时通常遇到哪些困难？",
        description = "",
        options = listOf(
            "很难快速想象音乐整体效果",
            "识谱速度慢",
            "乐谱复杂（多声部 / 节奏复杂）",
            "没有方便的试听工具",
            "图片或纸质乐谱无法直接播放",
            "需要手动输入乐谱才能试听"
        ),
        isMultipleChoice = true,
        allowOther = true
    ),
    SurveyPageStep(
        title = "您是否希望软件提供图片乐谱识别？",
        description = "系统能够自动识别图片或PDF中的五线谱，并将其转换为可播放、可编辑的数字乐谱。",
        options = listOf(
            "非常希望",
            "可以有",
            "无所谓",
            "不需要"
        ),
        isMultipleChoice = false,
        allowOther = false
    ),
    SurveyPageStep(
        title = "您是否希望软件能提供乐谱即时试听功能？",
        description = "系统能够根据识别结果自动生成音频播放，使用户无需演奏即可快速了解乐曲整体效果。",
        options = listOf(
            "非常希望",
            "可以有",
            "无所谓",
            "不需要"
        ),
        isMultipleChoice = false,
        allowOther = false
    ),
    SurveyPageStep(
        title = "您是否希望软件提供乐谱可视化功能？",
        description = "系统通过动态图形方式展示乐谱结构，帮助用户更直观地理解旋律、节奏和音高变化。",
        options = listOf(
            "非常希望",
            "可以有",
            "无所谓",
            "不需要"
        ),
        isMultipleChoice = false,
        allowOther = false
    ),
    SurveyPageStep(
        title = "您是否希望软件提供乐谱编辑功能？",
        description = "系统支持用户对识别后的乐谱进行修改和简单编辑，以满足个性化学习或创作需求。",
        options = listOf(
            "非常希望",
            "可以有",
            "无所谓",
            "不需要"
        ),
        isMultipleChoice = false,
        allowOther = false
    ),
    SurveyPageStep(
        title = "你是否希望软件提供 AI 辅助音乐功能？",
        description = "",
        options = listOf(
            "非常希望",
            "可以有",
            "无所谓",
            "不需要"
        ),
        isMultipleChoice = false,
        allowOther = false
    ),
    SurveyPageStep(
        title = "在音乐领域，你最感兴趣的 AI 功能是？",
        description = "",
        options = listOf(
            "AI自动编曲",
            "AI旋律润色",
            "AI生成伴奏",
            "AI分析乐曲难度",
            "AI推荐练习方式",
            "AI风格转换（古典 / 流行等）"
        ),
        isMultipleChoice = true,
        allowOther = false
    ),
)

@Preview(showBackground = true)
@Composable
fun SurveyPageContentPreview() {
    SurveyPageContent()
}

data class SurveyPageStep(
    val title: String,
    val description: String,
    val options: List<String>,
    val isMultipleChoice: Boolean,
    val allowOther: Boolean
)