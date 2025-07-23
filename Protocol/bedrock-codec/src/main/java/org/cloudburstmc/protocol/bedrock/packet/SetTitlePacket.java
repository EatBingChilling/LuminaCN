package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.protocol.common.PacketSignal;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class SetTitlePacket implements BedrockPacket {
    public Type type;
    public String text;
    public int fadeInTime;
    public int stayTime;
    public int fadeOutTime;
    /**
     * @since v448
     */
    public String xuid;
    /**
     * @since v448
     */
    public String platformOnlineId;
    /**
     * @since v712
     */
    public String filteredTitleText = "";

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.SET_TITLE;
    }

    public enum Type {
        CLEAR,
        RESET,
        TITLE,
        SUBTITLE,
        ACTIONBAR,
        TIMES,
        TITLE_JSON,
        SUBTITLE_JSON,
        ACTIONBAR_JSON
    }

    @Override
    public SetTitlePacket clone() {
        try {
            return (SetTitlePacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

