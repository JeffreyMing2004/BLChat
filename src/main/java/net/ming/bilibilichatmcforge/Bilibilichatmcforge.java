package net.ming.bilibilichatmcforge;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.ming.bilibilichatmcforge.client.BilibiliConfigScreen;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import net.ming.bilibilichatmcforge.utils.BilibiliClient;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Bilibilichatmcforge.MODID)
public class Bilibilichatmcforge {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "bilibilichatmcforge";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static BilibiliClient bilibiliClient;
    
    public static void restartClient() {
        if (bilibiliClient != null) {
            bilibiliClient.stop();
            bilibiliClient.start();
        }
    }
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "bilibilichatmcforge" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "bilibilichatmcforge" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "bilibilichatmcforge:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "bilibilichatmcforge:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Creates a new food item with the id "bilibilichatmcforge:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().alwaysEat().nutrition(1).saturationMod(2f).build())));

    // Creates a creative tab with the id "bilibilichatmcforge:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> EXAMPLE_ITEM.get().getDefaultInstance()).displayItems((parameters, output) -> {
        output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
    }).build());

    public Bilibilichatmcforge(FMLJavaModLoadingContext context) {
        JsonConfigManager.load();
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // Register Config Screen
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, 
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, lastScreen) -> new BilibiliConfigScreen(lastScreen)));
    }

    private void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(net.minecraft.commands.Commands.literal("bilibili")
                .requires(source -> source.hasPermission(2))
                .then(net.minecraft.commands.Commands.literal("roomcode")
                        .then(net.minecraft.commands.Commands.argument("code", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .executes(context -> {
                                    String code = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "code");
                                    JsonConfigManager.setRoomCode(code);
                                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Bilibili Room Code updated to: " + code), true);
                                    
                                    if (bilibiliClient != null) {
                                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Restarting Bilibili Client..."), true);
                                        bilibiliClient.stop();
                                        bilibiliClient.start();
                                    }
                                    return 1;
                                }))));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (JsonConfigManager.getInstance().accessKey != null && !JsonConfigManager.getInstance().accessKey.isEmpty()) {
            LOGGER.info("Bilibili Access Key loaded");
        }
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        bilibiliClient = new BilibiliClient(event.getServer());
        bilibiliClient.start();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (bilibiliClient != null) {
            bilibiliClient.stop();
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
