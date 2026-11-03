package au.com.foodtruckcentre.app.data

/**
 * Converts the WordPress page HTML (Elementor output) into simple blocks for native rendering.
 * Only text structure is kept – no scripts, styles or remote embeds are ever executed.
 */
sealed interface HtmlBlock {
    data class Heading(val text: String, val number: Int?, val level: Int) : HtmlBlock
    data class Paragraph(val spans: List<Span>) : HtmlBlock
    data class Bullet(val spans: List<Span>) : HtmlBlock
    data class Image(val url: String) : HtmlBlock
}

data class Span(val text: String, val bold: Boolean = false, val italic: Boolean = false, val link: String? = null)

object HtmlParser {

    private val tagRegex = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>")
    private val hrefRegex = Regex("href\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val srcRegex = Regex("src\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

    fun parse(html: String): List<HtmlBlock> {
        val cleaned = html
            .replace(Regex("<(script|style|noscript|iframe|svg)[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<!--[\\s\\S]*?-->"), "")

        val blocks = mutableListOf<HtmlBlock>()
        var spans = mutableListOf<Span>()
        var bold = 0
        var italic = 0
        var link: String? = null
        var mode: String? = null // "p", "li", "h"
        var headingLevel = 2
        var inOl = false
        var olItems = 0
        var sectionNumber = 0
        val pendingOlBlocks = mutableListOf<List<Span>>()

        fun flush() {
            val merged = mergeSpans(spans)
            if (merged.isNotEmpty()) {
                when (mode) {
                    "h" -> blocks += HtmlBlock.Heading(merged.joinToString("") { it.text }.trim(), null, headingLevel)
                    "li" -> if (inOl) pendingOlBlocks += merged else blocks += HtmlBlock.Bullet(merged)
                    else -> {
                        // A paragraph that is entirely bold reads as a sub-heading on the website.
                        if (merged.all { it.bold || it.text.isBlank() } && merged.sumOf { it.text.length } < 90) {
                            blocks += HtmlBlock.Heading(merged.joinToString("") { it.text }.trim(), null, 3)
                        } else {
                            blocks += HtmlBlock.Paragraph(merged)
                        }
                    }
                }
            }
            spans = mutableListOf()
        }

        fun flushOl() {
            // The website formats section titles as single-item numbered lists with bold text.
            for (item in pendingOlBlocks) {
                val text = item.joinToString("") { it.text }.trim()
                if (item.all { it.bold || it.text.isBlank() } && text.length < 90) {
                    sectionNumber++
                    blocks += HtmlBlock.Heading(text, sectionNumber, 2)
                } else {
                    olItems++
                    blocks += HtmlBlock.Bullet(listOf(Span("$olItems. ")) + item)
                }
            }
            pendingOlBlocks.clear()
        }

        var index = 0
        for (match in tagRegex.findAll(cleaned)) {
            val text = decode(cleaned.substring(index, match.range.first))
            if (text.isNotEmpty()) spans += Span(text, bold > 0, italic > 0, link)
            index = match.range.last + 1

            val closing = match.groupValues[1] == "/"
            val tag = match.groupValues[2].lowercase()
            val attrs = match.groupValues[3]
            when (tag) {
                "p", "div", "section" -> { flush(); mode = if (closing) null else "p" }
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    flush()
                    if (closing) mode = null else { mode = "h"; headingLevel = tag[1].digitToInt().coerceIn(1, 4) }
                }
                "li" -> { flush(); mode = if (closing) null else "li" }
                "ol" -> { flush(); if (closing) { flushOl(); inOl = false } else { inOl = true } }
                "ul" -> flush()
                "br" -> spans += Span("\n", bold > 0, italic > 0, link)
                "strong", "b" -> bold = (bold + if (closing) -1 else 1).coerceAtLeast(0)
                "em", "i" -> italic = (italic + if (closing) -1 else 1).coerceAtLeast(0)
                "a" -> link = if (closing) null else hrefRegex.find(attrs)?.groupValues?.get(1)?.let(::decode)
                "img" -> if (!closing) {
                    flush()
                    srcRegex.find(attrs)?.groupValues?.get(1)?.let(::decode)
                        ?.takeIf { it.startsWith("https://") }
                        ?.let { blocks += HtmlBlock.Image(it) }
                }
            }
        }
        val tail = decode(cleaned.substring(index))
        if (tail.isNotEmpty()) spans += Span(tail, bold > 0, italic > 0, link)
        flush()
        flushOl()
        return blocks
    }

    private fun mergeSpans(spans: List<Span>): List<Span> {
        // Collapse whitespace like a browser would, then drop empty results.
        val out = mutableListOf<Span>()
        val ws = Regex("[\\s\\u00A0]+")
        for (s in spans) {
            val t = if (s.text == "\n") "\n" else s.text.replace(ws, " ")
            if (t.isEmpty()) continue
            val last = out.lastOrNull()
            if (last != null && last.bold == s.bold && last.italic == s.italic && last.link == s.link) {
                out[out.lastIndex] = last.copy(text = last.text + t)
            } else out += s.copy(text = t)
        }
        if (out.isEmpty()) return out
        out[0] = out[0].copy(text = out[0].text.trimStart())
        out[out.lastIndex] = out.last().copy(text = out.last().text.trimEnd())
        return out.filter { it.text.isNotEmpty() }.takeIf { list -> list.any { it.text.isNotBlank() } } ?: emptyList()
    }

    private val entities = mapOf(
        "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&#039;" to "'", "&#39;" to "'",
        "&nbsp;" to " ", "&rsquo;" to "’", "&lsquo;" to "‘", "&ldquo;" to "“", "&rdquo;" to "”",
        "&ndash;" to "–", "&mdash;" to "—", "&hellip;" to "…", "&copy;" to "©",
    )

    fun decode(s: String): String {
        var r = s
        entities.forEach { (k, v) -> r = r.replace(k, v) }
        r = Regex("&#(\\d+);").replace(r) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: "" }
        r = Regex("&#x([0-9a-fA-F]+);").replace(r) { m -> m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: "" }
        return r
    }

    fun plainText(html: String): String = parse(html).joinToString("\n") { b ->
        when (b) {
            is HtmlBlock.Heading -> b.text
            is HtmlBlock.Paragraph -> b.spans.joinToString("") { it.text }
            is HtmlBlock.Bullet -> b.spans.joinToString("") { it.text }
            is HtmlBlock.Image -> ""
        }
    }
}
