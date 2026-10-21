package com.my.mealjournal.domain

/** 某一天的打卡状态。 */
enum class DayStatus(val label: String) {
    /** 早/午/晚各至少一条。 */
    COMPLETE("完整"),

    /** 有记录但三餐未记满。 */
    PARTIAL("部分"),

    /** 一条记录都没有。 */
    MISSING("缺失"),
}
