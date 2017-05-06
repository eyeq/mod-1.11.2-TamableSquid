package eyeq.tamablesquid;

import eyeq.util.client.renderer.ResourceLocationFactory;
import eyeq.util.client.resource.ULanguageCreator;
import eyeq.util.client.resource.lang.LanguageResourceManager;
import eyeq.util.common.registry.UEntityRegistry;
import net.minecraft.client.renderer.entity.RenderSquid;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import eyeq.tamablesquid.entity.passive.EntitySquidTamed;
import eyeq.tamablesquid.event.TamableSquidEventHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

import static eyeq.tamablesquid.TamableSquid.MOD_ID;

@Mod(modid = MOD_ID, version = "1.0", dependencies = "after:eyeq_util")
public class TamableSquid {
    public static final String MOD_ID = "eyeq_tamablesquid";

    @Mod.Instance(MOD_ID)
    public static TamableSquid instance;

    private static final ResourceLocationFactory resource = new ResourceLocationFactory(MOD_ID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new TamableSquidEventHandler());
        registerEntities();
        if(event.getSide().isServer()) {
            return;
        }
        registerEntityRenderings();
        createFiles();
    }

    public static void registerEntities() {
        EntityList.EntityEggInfo egg = EntityList.ENTITY_EGGS.get(new ResourceLocation("squid"));
        UEntityRegistry.registerModEntity(resource, EntitySquidTamed.class, "TamedSquid", 0, instance, egg);
    }

    @SideOnly(Side.CLIENT)
    public static void registerEntityRenderings() {
        RenderingRegistry.registerEntityRenderingHandler(EntitySquidTamed.class, RenderSquid::new);
    }

    public static void createFiles() {
        File project = new File("../1.11.2-TamableSquid");

        LanguageResourceManager language = new LanguageResourceManager();

        language.register(LanguageResourceManager.EN_US, EntitySquidTamed.class, "Squid");
        language.register(LanguageResourceManager.JA_JP, EntitySquidTamed.class, "イカ");

        ULanguageCreator.createLanguage(project, MOD_ID, language);
    }
}
