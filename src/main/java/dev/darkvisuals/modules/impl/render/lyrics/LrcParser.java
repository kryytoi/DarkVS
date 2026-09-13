package dev.darkvisuals.modules.impl.render.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LrcParser {
    // Таймкод в начале строки: [mm:ss.xx] или [mm:ss.xxx] (метаданные [ar:...] не матчатся)
    private static final Pattern TIMESTAMP_PREFIX = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]");
    // Убираем enhanced-LRC словесные метки вида <00:12.34> внутри текста строки
    private static final Pattern ENHANCED_WORD_TIMING = Pattern.compile("<\\d{2}:\\d{2}\\.\\d{2,3}>");

    private LrcParser() {}

    /**
     * Parses a raw LRC string into a chronologically sorted List of LyricLine.
     */
    public static List<LyricLine> parse(String lrcContent) {
        if (lrcContent == null || lrcContent.isBlank()) {
            return Collections.emptyList();
        }

        List<LyricLine> lines = new ArrayList<>();
        String[] rawLines = lrcContent.split("\\r?\\n");

        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Строка может начинаться с нескольких таймкодов подряд: [00:12.00][00:15.00]text
            String remaining = trimmed;
            List<Long> timestamps = new ArrayList<>();

            while (true) {
                Matcher start = TIMESTAMP_PREFIX.matcher(remaining);
                if (!start.find() || start.start() != 0) {
                    break;
                }

                try {
                    long minutes = Long.parseLong(start.group(1));
                    long seconds = Long.parseLong(start.group(2));
                    String fractionStr = start.group(3);

                    long millis = Long.parseLong(fractionStr);
                    if (fractionStr.length() == 2) {
                        millis *= 10;
                    }

                    timestamps.add((minutes * 60_000L) + (seconds * 1_000L) + millis);
                } catch (NumberFormatException ignored) {
                }

                remaining = remaining.substring(start.end());
            }

            if (timestamps.isEmpty()) continue;

            String text = remaining.trim();
            if (!text.isEmpty()) {
                text = ENHANCED_WORD_TIMING.matcher(text).replaceAll("").trim();
            }
            if (text.isEmpty()) continue;

            for (long ts : timestamps) {
                lines.add(new LyricLine(ts, text));
            }
        }

        Collections.sort(lines);
        return lines;
    }
}
