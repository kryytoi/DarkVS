package dev.darkvisuals.modules.impl.render.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LrcParser {

    private static final Pattern TIMESTAMP_PREFIX = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]");

    private static final Pattern ENHANCED_WORD_TIMING = Pattern.compile("<\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?>");

    private LrcParser() {}

    public static List<LyricLine> parse(String lrcContent) {
        if (lrcContent == null || lrcContent.isBlank()) {
            return Collections.emptyList();
        }

        List<LyricLine> lines = new ArrayList<>();
        String[] rawLines = lrcContent.split("\\r?\\n");

        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.matches("^\\[[a-zA-Z]+:.*\\]$")) continue;

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

                    long millis = 0;
                    if (fractionStr != null && !fractionStr.isEmpty()) {
                        millis = Long.parseLong(fractionStr);
                        if (fractionStr.length() == 1) {
                            millis *= 100;
                        } else if (fractionStr.length() == 2) {
                            millis *= 10;
                        }
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
