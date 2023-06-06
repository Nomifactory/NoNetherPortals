package co.neeve.nonetherportals;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod(modid = Tags.MODID, version = Tags.VERSION, name = Tags.MODNAME, acceptedMinecraftVersions = "[1.12.2]")
public class NoNetherPortals {
    private final Map<UUID, Long> lastMessageTimes = new HashMap<>();
    private static final long MESSAGE_COOLDOWN = 60 * 20; // 60 seconds in ticks

    private static Configuration config;
    private static String portalMessage;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = new File(event.getModConfigurationDirectory(), "nonetherportals.cfg");
        config = new Configuration(configFile);

        syncConfig();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPortalSpawnEvent(BlockEvent.PortalSpawnEvent event) {
        event.setCanceled(true);

        int range = 10;
        List<EntityPlayerMP> players = event.getWorld().getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(event.getPos()).grow(range));
        long currentTime = event.getWorld().getTotalWorldTime();

        for (EntityPlayerMP player : players) {
            UUID playerId = player.getUniqueID();
            if (lastMessageTimes.containsKey(playerId)) {
                long lastMessageTime = lastMessageTimes.get(playerId);
                if (currentTime - lastMessageTime < MESSAGE_COOLDOWN) {
                    continue;
                }
            }

            player.sendMessage(new TextComponentTranslation(portalMessage));

            lastMessageTimes.put(playerId, currentTime);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        lastMessageTimes.remove(event.player.getUniqueID());
    }

    private void syncConfig() {
        config.load();
        Property propMessage = config.get(Configuration.CATEGORY_GENERAL, "portalMessage", "Portals are disabled.");
        propMessage.setComment("The message to be displayed when portals are disabled.");
        portalMessage = propMessage.getString();

        if (config.hasChanged()) {
            config.save();
        }
    }

    @EventHandler
    public void onServerStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandReloadConfig());
    }

    public class CommandReloadConfig extends CommandBase {
        @Override
        @NotNull
        public String getName() {
            return "nonetherportals";
        }

        @Override
        @NotNull
        public String getUsage(@NotNull ICommandSender sender) {
            return "/nonetherportals reload";
        }

        @Override
        public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender, String[] args) {
            if (args.length > 0 && args[0].equals("reload")) {
                syncConfig();
                sender.sendMessage(new TextComponentString("NoNetherPortals config reloaded."));
            } else {
                sender.sendMessage(new TextComponentString("Usage: /nonetherportals reload"));
            }
        }
    }
}
