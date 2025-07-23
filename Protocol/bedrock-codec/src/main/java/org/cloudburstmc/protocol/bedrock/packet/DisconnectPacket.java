package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.common.PacketSignal;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class DisconnectPacket implements BedrockPacket {
    public DisconnectFailReason reason = DisconnectFailReason.UNKNOWN;
    public boolean messageSkipped;
    public String kickMessage;
    /**
     * @since v712
     */
    public String filteredMessage = "";

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.DISCONNECT;
    }

    @Override
    public DisconnectPacket clone() {
        try {
            return (DisconnectPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

