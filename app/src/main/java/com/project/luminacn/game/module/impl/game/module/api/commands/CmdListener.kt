import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.GameManager
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.util.AssetManager

class CmdListener(private val moduleManager: GameManager) : Element(
    name = "ChatListener",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_chat_listener")
) {
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {

    }
} 