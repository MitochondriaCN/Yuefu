package com.xianliticn.yuefu.pages.scanstudio

enum class OmrModel(val label: String, val description: String) {
    QINGSHANG("清商（yuefu-qingshang-fp16-0.2）", "高响应速度模型，专为快速识谱优化。"),
    ZHENGSHENG("正声（yuefu-zhengsheng-fp32-0.2）", "极致解析乐谱细节，提高复杂乐谱的识别精度。")
}