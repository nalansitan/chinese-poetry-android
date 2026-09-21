package com.nalansitan.chinesepoetry.presentation.util

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints

private val BREAK_PUNCTUATION = setOf('，', '。', '！', '？', '；', '：', '、', '）', '》', '」', '』')
private val COUNT_AS_CONTENT_REGEX = Regex("[，。！？；：、】【）》」』、\\s]+")

fun wrapPoemLine(
    text: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int
): List<String> {
    if (maxWidthPx <= 0 || fitsWidth(text, textMeasurer, style, maxWidthPx)) {
        return listOf(text)
    }

    val segments = mutableListOf<String>()
    var currentIndex = 0

    while (currentIndex < text.length) {
        var maxFitEnd = currentIndex + 1
        for (end in (currentIndex + 1)..text.length) {
            val candidate = text.substring(currentIndex, end)
            if (fitsWidth(candidate, textMeasurer, style, maxWidthPx)) {
                maxFitEnd = end
            } else {
                break
            }
        }

        if (maxFitEnd >= text.length) {
            segments += text.substring(currentIndex)
            break
        }

        val breakIndex = findPreferredBreak(text, currentIndex, maxFitEnd) ?: maxFitEnd
        val segment = text.substring(currentIndex, breakIndex).trim()
        if (segment.isNotEmpty()) {
            segments += segment
        }
        currentIndex = breakIndex
    }

    rebalanceTrailingShortLine(segments, textMeasurer, style, maxWidthPx)
    return segments.ifEmpty { listOf(text) }
}

private fun rebalanceTrailingShortLine(
    segments: MutableList<String>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int
) {
    if (segments.size < 2) return

    val lastIndex = segments.lastIndex
    val lastSegment = segments[lastIndex]
    if (effectiveLineLength(lastSegment) > 2) return

    val previous = segments[lastIndex - 1]
    val transferable = previous
        .takeLastWhile { it !in BREAK_PUNCTUATION }
        .takeLast(2)

    if (transferable.isEmpty() || transferable.length >= previous.length) return

    val candidatePrevious = previous.dropLast(transferable.length).trimEnd()
    val candidateLast = (transferable + lastSegment).trimStart()
    if (
        candidatePrevious.isNotEmpty() &&
        fitsWidth(candidatePrevious, textMeasurer, style, maxWidthPx) &&
        fitsWidth(candidateLast, textMeasurer, style, maxWidthPx)
    ) {
        segments[lastIndex - 1] = candidatePrevious
        segments[lastIndex] = candidateLast
    }
}

private fun fitsWidth(
    text: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int
): Boolean {
    return textMeasurer.measure(
        text = text,
        style = style,
        constraints = Constraints(maxWidth = maxWidthPx)
    ).lineCount <= 1
}

private fun findPreferredBreak(text: String, start: Int, maxFitEnd: Int): Int? {
    for (index in (maxFitEnd - 1) downTo start) {
        if (text[index] in BREAK_PUNCTUATION) {
            return index + 1
        }
    }
    return null
}

private fun effectiveLineLength(text: String): Int {
    return text.replace(COUNT_AS_CONTENT_REGEX, "").length
}
