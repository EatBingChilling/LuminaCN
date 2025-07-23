import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.constructors.Element
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.util.AssetManager

class ConfigManagerElement : Element(
    name = "config_manager",
    category = CheatCategory.Config,
    displayNameResId = AssetManager.getString("module_config_manager")
) {
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {

    }
} 