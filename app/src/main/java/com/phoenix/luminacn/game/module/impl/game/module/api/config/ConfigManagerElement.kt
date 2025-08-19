import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.util.AssetManager

class ConfigManagerElement : Element(
    name = "config_manager",
    category = CheatCategory.Config,
    displayNameResId = AssetManager.getString("module_config_manager")
) {
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {

    }
} 