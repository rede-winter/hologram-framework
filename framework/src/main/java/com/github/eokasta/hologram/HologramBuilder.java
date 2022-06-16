package com.github.eokasta.hologram;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@NoArgsConstructor
public class HologramBuilder {

    private final List<LineType> lines = new ArrayList<>();
    private HologramInteractHandler hologramInteractHandler = new HologramInteractHandler();

    public HologramBuilder addAction(HologramInteractAction action, Consumer<HologramInteractContext> consumer) {
        hologramInteractHandler.addAction(action, consumer);
        return this;
    }

    public HologramBuilder addLine(@NotNull String text) {
        addLine(text, String.class);
        return this;
    }

    public HologramBuilder addDynamicTextLine(@NotNull Function<Player, String> function) {
        return addLine(function, String.class);
    }

    private HologramBuilder addLine(@NotNull Object value, Class<?> type) {
        this.lines.add(new LineType(value, type));
        return this;
    }

    public Hologram build() {
        final List<AbstractHologramLine> lines = new ArrayList<>();
        final Hologram hologram = new Hologram(lines);
        hologram.setInteractHandler(hologramInteractHandler);

        this.lines.stream()
              .map(lineType -> resolveLineType(hologram, lineType))
              .forEach(line -> lines.add(0, line));

        return hologram;
    }

    public Hologram build(@NotNull HologramRegistry registry) {
        final Hologram hologram = build();
        registry.registerHologram(hologram);

        return hologram;
    }

    @SuppressWarnings("unchecked")
    private AbstractHologramLine resolveLineType(
          @NotNull Hologram hologram,
          @NotNull LineType lineType
    ) {
        final Object value = lineType.value;

        if (value instanceof BiFunction)
            return resolveDynamicFunctionType(hologram,
                  (BiFunction<AbstractHologramLine, Player, Object>) value,
                  lineType.type
            );

        if (value instanceof Function)
            return resolveDynamicFunctionType(
                  hologram,
                  ($, player) -> ((Function<Player, Object>) value).apply(player),
                  lineType.type
            );

        return createStaticLine(hologram, value);
    }

    private AbstractHologramLine resolveDynamicFunctionType(
          @NotNull Hologram hologram,
          final BiFunction<AbstractHologramLine, Player, Object> function,
          final Class<?> type
    ) {
        DynamicHologramLine dynamicHologramLine;
        if (type.equals(String.class))
            dynamicHologramLine = new TextHologramLine(hologram);
        else
            throw new IllegalArgumentException("Unsupported hologram type: " + type);

        dynamicHologramLine.setFunction(function);
        return dynamicHologramLine;
    }

    private AbstractHologramLine createStaticLine(
          @NotNull Hologram hologram,
          @NotNull Object value
    ) {
        if (value instanceof String) {
            final TextHologramLine line = new TextHologramLine(hologram);
            line.setText((String) value);
            return line;
        }

        throw new IllegalArgumentException("Unsupported hologram type: " + value.getClass().getName());
    }

    @RequiredArgsConstructor
    private static final class LineType {

        private final Object value;
        private final Class<?> type;

    }

}
