package tech.palei.isupportalerts.utils;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;


public class SopUtil {

    public static String computeHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

// Chunk by headings dynamically (h1–h6)
    public static Map<String, String> chunkHtml(String html) {
        Map<String, String> chunks = new LinkedHashMap<>();
        Document doc = Jsoup.parse(html);

        Elements headings = doc.select("h1, h2, h3, h4, h5, h6");
        for (Element heading : headings) {
            StringBuilder chunk = new StringBuilder();
            chunk.append("Heading: ").append(heading.text()).append("\n");

            Element sibling = heading.nextElementSibling();
            while (sibling != null && !sibling.tagName().matches("h1|h2|h3|h4|h5|h6")) {
                chunk.append(sibling.text()).append("\n");
                sibling = sibling.nextElementSibling();
            }

            String chunkText = chunk.toString().trim();
            String chunkId = heading.text().toLowerCase().replaceAll("\\s+", "_"); // simple ID
            chunks.put(chunkId, chunkText);
        }
        return chunks;
    }

}
