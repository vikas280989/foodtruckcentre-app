package au.com.foodtruckcentre.app

import au.com.foodtruckcentre.app.data.HtmlBlock
import au.com.foodtruckcentre.app.data.HtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlParserTest {
    @Test fun numberedSectionHeadingsAndBullets() {
        val html = "<p><strong>Privacy Policy</strong></p><ol><li><strong> Overview</strong></li></ol><p>FTC is committed.</p>" +
            "<ol><li><strong> Information We Collect</strong></li></ol><ul><li>Name, email address</li><li>Event details</li></ul>"
        val blocks = HtmlParser.parse(html)
        assertEquals(HtmlBlock.Heading("Privacy Policy", null, 3), blocks[0])
        assertEquals(HtmlBlock.Heading("Overview", 1, 2), blocks[1])
        assertTrue(blocks[2] is HtmlBlock.Paragraph)
        assertEquals(HtmlBlock.Heading("Information We Collect", 2, 2), blocks[3])
        assertEquals(2, blocks.count { it is HtmlBlock.Bullet })
    }

    @Test fun featuringPageLinkAndImage() {
        val html = "<h2>This month we are featuring thepizzaboys food truck.</h2><p>To obtain a quote from <em><strong>thepizzaboys</strong></em> food truck" +
            "<strong><a href=\"https://foodtruckcentre.com.au/#form\">click HERE</a></strong> and then select the Pizza / Italian<br>cuisine.</p>" +
            "<img src=\"https://foodtruckcentre.com.au/wp-content/uploads/2026/04/Untitled-Presentation.png\">"
        val blocks = HtmlParser.parse(html)
        assertEquals("This month we are featuring thepizzaboys food truck.", (blocks[0] as HtmlBlock.Heading).text)
        val p = blocks[1] as HtmlBlock.Paragraph
        assertTrue(p.spans.any { it.link == "https://foodtruckcentre.com.au/#form" && it.text.contains("click HERE") })
        assertTrue(p.spans.any { it.italic && it.bold && it.text == "thepizzaboys" })
        assertTrue(blocks.last() is HtmlBlock.Image)
    }

    @Test fun scriptsAndEntitiesHandled() {
        val blocks = HtmlParser.parse("<p>Fish &amp; chips&nbsp;&#8211; <script>alert(1)</script>tasty</p><img src=\"http://insecure/x.png\">")
        val text = (blocks.single() as HtmlBlock.Paragraph).spans.joinToString("") { it.text }
        assertEquals("Fish & chips – tasty", text)
    }

    @Test fun emptyContentGivesNoBlocks() {
        assertTrue(HtmlParser.parse("<div>  </div><p>&nbsp;</p>").isEmpty())
    }
}
