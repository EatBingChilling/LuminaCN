import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.util.AssetManager

class CmdListener(private val moduleManager: GameManager) : Element(
    name = "ChatListener",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_chat_listener")
) {
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {

    }
} 