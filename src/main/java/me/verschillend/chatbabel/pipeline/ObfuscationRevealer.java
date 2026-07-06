package me.verschillend.chatbabel.pipeline;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ObfuscationRevealer {

    private ObfuscationRevealer() {
    }

    public static boolean containsObfuscatedRun(Text text) {
        return text.visit((style, asString) -> style.isObfuscated() && !asString.isEmpty()
                ? Optional.of(Boolean.TRUE)
                : Optional.empty(), Style.EMPTY).isPresent();
    }

    public static MutableText reveal(Text text) {
        List<Segment> segments = new ArrayList<>();
        text.visit((style, asString) -> {
            if (!asString.isEmpty()) {
                segments.add(new Segment(asString, style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        MutableText result = null;
        for (Segment segment : segments) {
            Style style = segment.style();
            if (style.isObfuscated()) {
                HoverEvent obfuscatedHover = new HoverEvent.ShowText(Text.literal(segment.text()));
                style = style.withHoverEvent(obfuscatedHover);
            }
            MutableText part = Text.literal(segment.text()).setStyle(style);
            result = (result == null) ? part : result.append(part);
        }
        return result != null ? result : text.copy();
    }

    private record Segment(String text, Style style) {
    }
}
