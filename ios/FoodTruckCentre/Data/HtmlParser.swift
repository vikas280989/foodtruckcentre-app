import Foundation

/// Converts WordPress page HTML into simple blocks for native rendering (same rules as the Android app).
/// Only text structure is kept – no scripts, styles or embeds are ever run.
enum HtmlBlock: Equatable {
    case heading(text: String, number: Int?, level: Int)
    case paragraph([HtmlSpan])
    case bullet([HtmlSpan])
    case image(URL)
}

struct HtmlSpan: Equatable {
    var text: String
    var bold = false
    var italic = false
    var link: String? = nil
}

enum HtmlParser {
    private static let tagRegex = try! NSRegularExpression(pattern: "<(/?)([a-zA-Z0-9]+)([^>]*)>")
    private static let hrefRegex = try! NSRegularExpression(pattern: "href\\s*=\\s*[\"']([^\"']+)[\"']", options: .caseInsensitive)
    private static let srcRegex = try! NSRegularExpression(pattern: "src\\s*=\\s*[\"']([^\"']+)[\"']", options: .caseInsensitive)

    static func parse(_ html: String) -> [HtmlBlock] {
        var cleaned = html.replacingOccurrences(of: "<(script|style|noscript|iframe|svg)[\\s\\S]*?</\\1>", with: "", options: [.regularExpression, .caseInsensitive])
        cleaned = cleaned.replacingOccurrences(of: "<!--[\\s\\S]*?-->", with: "", options: .regularExpression)

        var blocks: [HtmlBlock] = []
        var spans: [HtmlSpan] = []
        var bold = 0, italic = 0
        var link: String? = nil
        var mode: String? = nil
        var headingLevel = 2
        var inOl = false
        var olItems = 0
        var sectionNumber = 0
        var pendingOl: [[HtmlSpan]] = []

        func flush() {
            let merged = merge(spans)
            if !merged.isEmpty {
                let text = merged.map(\.text).joined().trimmingCharacters(in: .whitespaces)
                switch mode {
                case "h": blocks.append(.heading(text: text, number: nil, level: headingLevel))
                case "li": if inOl { pendingOl.append(merged) } else { blocks.append(.bullet(merged)) }
                default:
                    if merged.allSatisfy({ $0.bold || $0.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) && text.count < 90 {
                        blocks.append(.heading(text: text, number: nil, level: 3))
                    } else {
                        blocks.append(.paragraph(merged))
                    }
                }
            }
            spans = []
        }

        func flushOl() {
            // The website formats section titles as single-item numbered lists in bold.
            for item in pendingOl {
                let text = item.map(\.text).joined().trimmingCharacters(in: .whitespaces)
                if item.allSatisfy({ $0.bold || $0.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) && text.count < 90 {
                    sectionNumber += 1
                    blocks.append(.heading(text: text, number: sectionNumber, level: 2))
                } else {
                    olItems += 1
                    blocks.append(.bullet([HtmlSpan(text: "\(olItems). ")] + item))
                }
            }
            pendingOl = []
        }

        let ns = cleaned as NSString
        var index = 0
        for m in tagRegex.matches(in: cleaned, range: NSRange(location: 0, length: ns.length)) {
            let text = decode(ns.substring(with: NSRange(location: index, length: m.range.location - index)))
            if !text.isEmpty { spans.append(HtmlSpan(text: text, bold: bold > 0, italic: italic > 0, link: link)) }
            index = m.range.location + m.range.length
            let closing = ns.substring(with: m.range(at: 1)) == "/"
            let tag = ns.substring(with: m.range(at: 2)).lowercased()
            let attrs = ns.substring(with: m.range(at: 3))
            switch tag {
            case "p", "div", "section": flush(); mode = closing ? nil : "p"
            case "h1", "h2", "h3", "h4", "h5", "h6":
                flush()
                if closing { mode = nil } else { mode = "h"; headingLevel = min(max(Int(String(tag.last!)) ?? 2, 1), 4) }
            case "li": flush(); mode = closing ? nil : "li"
            case "ol": flush(); if closing { flushOl(); inOl = false } else { inOl = true }
            case "ul": flush()
            case "br": spans.append(HtmlSpan(text: "\n", bold: bold > 0, italic: italic > 0, link: link))
            case "strong", "b": bold = max(0, bold + (closing ? -1 : 1))
            case "em", "i": italic = max(0, italic + (closing ? -1 : 1))
            case "a": link = closing ? nil : firstGroup(hrefRegex, attrs).map(decode)
            case "img":
                if !closing {
                    flush()
                    if let src = firstGroup(srcRegex, attrs).map(decode), src.hasPrefix("https://"), let u = URL(string: src) {
                        blocks.append(.image(u))
                    }
                }
            default: break
            }
        }
        let tail = decode(ns.substring(from: index))
        if !tail.isEmpty { spans.append(HtmlSpan(text: tail, bold: bold > 0, italic: italic > 0, link: link)) }
        flush()
        flushOl()
        return blocks
    }

    static func plainText(_ html: String) -> String {
        parse(html).map { b -> String in
            switch b {
            case .heading(let t, _, _): return t
            case .paragraph(let s), .bullet(let s): return s.map(\.text).joined()
            case .image: return ""
            }
        }.joined(separator: "\n")
    }

    private static func firstGroup(_ re: NSRegularExpression, _ s: String) -> String? {
        let ns = s as NSString
        guard let m = re.firstMatch(in: s, range: NSRange(location: 0, length: ns.length)) else { return nil }
        return ns.substring(with: m.range(at: 1))
    }

    private static func merge(_ spans: [HtmlSpan]) -> [HtmlSpan] {
        var out: [HtmlSpan] = []
        for s in spans {
            let t = s.text == "\n" ? "\n" : s.text.replacingOccurrences(of: "[\\s\\u{00A0}]+", with: " ", options: .regularExpression)
            if t.isEmpty { continue }
            if var last = out.last, last.bold == s.bold, last.italic == s.italic, last.link == s.link {
                last.text += t; out[out.count - 1] = last
            } else {
                var c = s; c.text = t; out.append(c)
            }
        }
        guard !out.isEmpty else { return out }
        // Trim spaces and line breaks around the block, like the Android app (trimStart / trimEnd).
        out[0].text = String(out[0].text.drop(while: { $0.isWhitespace }))
        while let last = out.last?.text.last, last.isWhitespace { out[out.count - 1].text.removeLast() }
        out = out.filter { !$0.text.isEmpty }
        guard out.contains(where: { !$0.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) else { return [] }
        // Never more than one blank line in a row.
        for i in out.indices {
            out[i].text = out[i].text.replacingOccurrences(of: "\n[ ]*\n([ ]*\n)+", with: "\n\n", options: .regularExpression)
        }
        return out
    }

    private static let entities: [(String, String)] = [
        ("&amp;", "&"), ("&lt;", "<"), ("&gt;", ">"), ("&quot;", "\""), ("&#039;", "'"), ("&#39;", "'"), ("&nbsp;", " "),
        ("&rsquo;", "’"), ("&lsquo;", "‘"), ("&ldquo;", "“"), ("&rdquo;", "”"), ("&ndash;", "–"), ("&mdash;", "—"),
        ("&hellip;", "…"), ("&copy;", "©"),
    ]

    static func decode(_ s: String) -> String {
        var r = s
        for (k, v) in entities { r = r.replacingOccurrences(of: k, with: v) }
        for (pattern, radix) in [("&#(\\d+);", 10), ("&#x([0-9a-fA-F]+);", 16)] {
            let re = try! NSRegularExpression(pattern: pattern)
            var ns = r as NSString
            for m in re.matches(in: r, range: NSRange(location: 0, length: ns.length)).reversed() {
                let code = ns.substring(with: m.range(at: 1))
                let rep = UInt32(code, radix: radix).flatMap(Unicode.Scalar.init).map { String(Character($0)) } ?? ""
                ns = ns.replacingCharacters(in: m.range, with: rep) as NSString
            }
            r = ns as String
        }
        return r
    }
}
