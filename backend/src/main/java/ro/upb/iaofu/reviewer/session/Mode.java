package ro.upb.iaofu.reviewer.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/** The three variants served through the same interface (F17). */
public enum Mode {
    PROPOSED("proposed"),
    BASELINE_A("baseline_a"),
    BASELINE_B("baseline_b");

    private final String wire;

    Mode(String wire) {
        this.wire = wire;
    }

    @JsonValue
    public String wire() {
        return wire;
    }

    @JsonCreator
    public static Mode fromWire(String value) {
        return Arrays.stream(values()).filter(m -> m.wire.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown mode '" + value + "'; expected proposed, baseline_a or baseline_b"));
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<Mode, String> {

        @Override
        public String convertToDatabaseColumn(Mode mode) {
            return mode == null ? null : mode.wire;
        }

        @Override
        public Mode convertToEntityAttribute(String value) {
            return value == null ? null : fromWire(value);
        }
    }
}
