package com.astral.wbtn.ui

object RangeParser {
    fun parseRangeString(input: String): Set<Int> {
        if (input.isBlank()) return emptySet()
        val result = mutableSetOf<Int>()
        val parts = input.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val rangeParts = trimmed.split("-")
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].trim().toIntOrNull()
                    val end = rangeParts[1].trim().toIntOrNull()
                    if (start != null && end != null) {
                        val rangeStart = minOf(start, end)
                        val rangeEnd = maxOf(start, end)
                        for (i in rangeStart..rangeEnd) {
                            result.add(i)
                        }
                    }
                }
            } else {
                val num = trimmed.toIntOrNull()
                if (num != null) {
                    result.add(num)
                }
            }
        }
        return result
    }
}
