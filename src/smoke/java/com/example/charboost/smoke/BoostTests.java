package com.example.charboost.smoke;

import com.example.charboost.Boost;
import com.example.charboost.Opt;
import com.example.charboost.hook.AsyncSaves;
import com.example.charboost.hook.Census;
import com.example.charboost.hook.MobAi;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Real headless server checks with the throttling at full strength. No players exist here, so every mob counts as far away
 * and every hopper as far from players: the hardest case. Only compiled with -Psmoke, never in the jar.
 */
@GameTestHolder(Boost.MODID)
@PrefixGameTestTemplate(false)
public class BoostTests {
    private static void fullPressure() {
        Boost.pinnedLevel = 3;
        Boost.level = 3;
    }

    private static CommandSourceStack source(GameTestHelper h) {
        return h.getLevel().getServer().createCommandSourceStack().withSuppressedOutput().withLevel(h.getLevel()).withPermission(4);
    }

    private static List<? extends Entity> select(GameTestHelper h, String selector) {
        try {
            return new EntitySelectorParser(new StringReader(selector)).parse().findEntities(source(h));
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(selector, e);
        }
    }

    private static <T extends BlockEntity> T be(GameTestHelper h, BlockPos pos) {
        @SuppressWarnings("unchecked")
        T out = (T) h.getBlockEntity(pos);
        return out;
    }

    private static int count(Container c) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            n += c.getItem(i).getCount();
        }
        return n;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void farMobIsThrottledButExemptOnesAreNot(GameTestHelper h) {
        fullPressure();
        Zombie plain = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Zombie named = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
        named.setCustomName(Component.literal("Bob"));
        Wolf tame = h.spawn(EntityType.WOLF, new BlockPos(3, 2, 1));
        tame.setTame(true);
        Zombie tagged = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        tagged.addTag("charpowers.clone");
        Zombie hunter = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
        Pig prey = h.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
        hunter.setTarget(prey);
        h.runAfterDelay(30, () -> {
            h.assertTrue(MobAi.interval(plain) == 16, "a plain far zombie should be at 16, is " + MobAi.interval(plain));
            h.assertTrue(MobAi.interval(named) == 1, "named mob throttled");
            h.assertTrue(MobAi.interval(tame) == 1, "tamed mob throttled");
            h.assertTrue(MobAi.interval(tagged) == 1, "tagged mob throttled");
            h.assertTrue(MobAi.interval(hunter) == 1, "mob with a target throttled");
            h.assertTrue(Opt.MOB_AI.skips > 0, "nothing was ever skipped");
            h.succeed();
        });
    }

    private static void attackCheck(GameTestHelper h) {
        // A villager as the prey: zombies drop anything else for any villager nearby. On the ground: a mob with no AI doesn't fall.
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 0, 1));
        Villager prey = h.spawn(EntityType.VILLAGER, new BlockPos(3, 0, 1));
        prey.setNoAi(true);
        zombie.setTarget(prey);
        h.succeedWhen(() -> h.assertTrue(prey.getHealth() < prey.getMaxHealth(), "the zombie hasn't hit its prey: target " + zombie.getTarget()
                + " zombie at " + zombie.position() + " prey at " + prey.position() + " aggressive " + zombie.isAggressive()
                + " alive " + zombie.isAlive() + " on ground " + zombie.onGround() + " path " + zombie.getNavigation().getPath()));
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "attack")
    public static void throttledMobWithATargetStillAttacks(GameTestHelper h) {
        fullPressure();
        attackCheck(h);
    }

    @net.minecraft.gametest.framework.BeforeBatch(batch = "plain")
    public static void switchOff(net.minecraft.server.level.ServerLevel level) {
        Boost.enabled = false;
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = "plain")
    public static void switchOn(net.minecraft.server.level.ServerLevel level) {
        Boost.enabled = true;
    }

    /** The same check with the mod switched off, to tell a fault in the check from a fault in the mod. */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "plain")
    public static void plainGameMobWithATargetAttacks(GameTestHelper h) {
        attackCheck(h);
    }

    @GameTest(template = "empty", timeoutTicks = 800)
    public static void throttledMobStillFindsATargetByItself(GameTestHelper h) {
        fullPressure();
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Villager villager = h.spawn(EntityType.VILLAGER, new BlockPos(3, 0, 1));
        villager.setNoAi(true);
        h.succeedWhen(() -> h.assertTrue(zombie.getTarget() == villager, "still no target"));
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void throttledMobStillWalksWhereItIsTold(GameTestHelper h) {
        fullPressure();
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        BlockPos goal = h.absolutePos(new BlockPos(4, 2, 4));
        // On the first tick it isn't on the ground yet and the game refuses to plan a path.
        h.runAfterDelay(30, () -> zombie.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 1.0));
        h.succeedWhen(() -> h.assertTrue(zombie.blockPosition().distManhattan(goal) <= 2, "hasn't arrived, at " + zombie.blockPosition()));
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void brainMemoriesKeepRealTime(GameTestHelper h) {
        fullPressure();
        Villager villager = h.spawn(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        h.runAfterDelay(25, () -> {
            h.assertTrue(MobAi.interval(villager) == 16, "villager should be far");
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.GOLEM_DETECTED_RECENTLY, true, 30L);
            h.assertTrue(villager.getBrain().hasMemoryValue(MemoryModuleType.GOLEM_DETECTED_RECENTLY), "memory didn't take");
            h.runAfterDelay(40, () -> {
                h.assertFalse(villager.getBrain().hasMemoryValue(MemoryModuleType.GOLEM_DETECTED_RECENTLY), "memory outlived its 30 ticks");
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void farHopperKeepsItsFullSpeed(GameTestHelper h) {
        fullPressure();
        BlockPos top = new BlockPos(1, 3, 1);
        BlockPos hopper = new BlockPos(1, 2, 1);
        BlockPos bottom = new BlockPos(1, 1, 1);
        h.setBlock(top, Blocks.CHEST);
        h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.setBlock(bottom, Blocks.CHEST);
        ChestBlockEntity source = be(h, top);
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        h.runAfterDelay(200, () -> {
            ChestBlockEntity out = be(h, bottom);
            // A normal hopper moves one item per 8 ticks: 25 in 200 ticks.
            int moved = count(out);
            h.assertTrue(moved >= 22, "a working hopper moved only " + moved + " items in 200 ticks");
            h.assertTrue(moved <= 26, "a hopper moved too fast: " + moved);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void restingHopperStillPicksUpDrops(GameTestHelper h) {
        fullPressure();
        BlockPos hopper = new BlockPos(2, 2, 2);
        h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.runAfterDelay(40, () -> {
            HopperBlockEntity be = be(h, hopper);
            h.assertTrue(count(be) == 0, "hopper wasn't empty");
            long before = Opt.HOPPERS.skips;
            h.assertTrue(before > 0, "the idle hopper never rested");
            BlockPos above = h.absolutePos(hopper.above());
            ItemEntity item = new ItemEntity(h.getLevel(), above.getX() + 0.5, above.getY() + 0.1, above.getZ() + 0.5, new ItemStack(Items.DIAMOND));
            item.setDeltaMovement(0, 0, 0);
            h.getLevel().addFreshEntity(item);
            // A rest is 8 ticks at the very most.
            h.runAfterDelay(12, () -> {
                h.assertTrue(count(be) == 1, "the drop wasn't picked up");
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void furnaceKeepsWorking(GameTestHelper h) {
        fullPressure();
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, Blocks.FURNACE);
        FurnaceBlockEntity furnace = be(h, pos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        furnace.setItem(1, new ItemStack(Items.COAL, 4));
        h.runAfterDelay(215, () -> {
            h.assertTrue(furnace.getItem(2).getCount() >= 1, "nothing smelted in 215 ticks");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tagSelectorsSeeEveryChange(GameTestHelper h) {
        ArmorStand stand = h.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
        String tag = "charboost_test_tag";
        String selector = "@e[tag=" + tag + "]";
        h.assertTrue(Census.working(), "the tag count isn't running");
        h.assertTrue(select(h, selector).isEmpty(), "found somebody before tagging");

        long skipped = Opt.SELECTOR_TAGS.skips;
        select(h, selector);
        h.assertTrue(Opt.SELECTOR_TAGS.skips > skipped, "nobody has the tag, it should have skipped the search");

        stand.addTag(tag);
        h.assertTrue(select(h, selector).size() == 1, "addTag not seen");
        stand.removeTag(tag);
        h.assertTrue(select(h, selector).isEmpty(), "removeTag not seen");
        stand.getTags().add(tag);
        h.assertTrue(select(h, selector).size() == 1, "changing getTags() directly not seen");
        stand.getTags().removeIf(tag::equals);
        h.assertTrue(select(h, selector).isEmpty(), "removeIf not seen");

        CompoundTag saved = stand.saveWithoutId(new CompoundTag());
        h.assertFalse(saved.contains("Tags"), "tags should be gone from the save");
        stand.addTag(tag);
        saved = stand.saveWithoutId(new CompoundTag());
        h.assertTrue(saved.getList("Tags", 8).size() == 1, "tag missing from the save");
        stand.load(saved);
        h.assertTrue(select(h, selector).size() == 1, "tag lost after loading from save");

        h.assertTrue(select(h, "@e[tag=" + tag + ",tag=charboost_nobody_has]").isEmpty(), "two tags, one on nobody");
        h.assertTrue(select(h, "@e[tag=!" + tag + ",tag=charboost_nobody_has]").isEmpty(), "not plus nobody");
        h.assertTrue(select(h, "@e[tag=" + tag + ",type=armor_stand]").size() == 1, "tag plus type");

        stand.discard();
        h.runAfterDelay(1, () -> {
            h.assertTrue(select(h, selector).isEmpty(), "removed entity still found");
            h.assertTrue(Boost.CENSUS.count(tag) == 0, "removed entity still counted");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void typeSelectorsFindExactlyWhatTheFullSearchFinds(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Pig one = h.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        Pig two = h.spawn(EntityType.PIG, new BlockPos(2, 2, 1));
        h.spawn(EntityType.COW, new BlockPos(3, 2, 1));
        long skipped = Opt.SELECTOR_INDEX.skips;

        int fast = level.getEntities(EntityType.PIG, e -> true).size();
        int slow = 0;
        for (Entity e : level.getAllEntities()) {
            if (e.getType() == EntityType.PIG) {
                slow++;
            }
        }
        h.assertTrue(fast == slow, "index found " + fast + ", full search " + slow);
        h.assertTrue(fast >= 2, "pigs missing");
        h.assertTrue(Opt.SELECTOR_INDEX.skips > skipped, "the index wasn't used");

        List<? extends Entity> found = select(h, "@e[type=pig,limit=1]");
        h.assertTrue(found.size() == 1, "limit=1 gave " + found.size());
        int all = select(h, "@e[type=pig]").size();
        h.assertTrue(all == fast, "selector found " + all);

        one.discard();
        two.discard();
        h.runAfterDelay(1, () -> {
            int after = level.getEntities(EntityType.PIG, e -> true).size();
            h.assertTrue(after == fast - 2, "removed pigs still indexed: " + after);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void droppedItemSelectorsFindTheRightItem(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        ItemEntity stone = new ItemEntity(h.getLevel(), at.getX(), at.getY(), at.getZ(), new ItemStack(Items.STONE));
        stone.setNeverPickUp();
        // Right at the start of a batch the area may not be ready to hold entities yet.
        h.runAfterDelay(10, () -> {
            h.getLevel().addFreshEntity(stone);
            h.assertTrue(select(h, "@e[type=item,nbt={Item:{id:\"minecraft:stone\"}}]").contains(stone), "stone not found");
            h.assertFalse(select(h, "@e[type=item,nbt={Item:{id:\"minecraft:elytra\"}}]").contains(stone), "found the wrong item");
            h.assertTrue(select(h, "@e[type=item,nbt={Item:{id:\"stone\"}}]").isEmpty(), "an id without its namespace never matched before either");
            h.assertTrue(select(h, "@e[type=item,nbt={Item:{id:\"charboost:no_such_item\"}}]").isEmpty(), "found an item that doesn't exist");
            h.assertTrue(select(h, "@e[type=item,nbt=!{Item:{id:\"minecraft:elytra\"}}]").contains(stone), "not-elytra should find stone");
            stone.discard();
            h.succeed();
        });
    }

    private static final class TestData extends SavedData {
        int value = 12345;

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("value", value);
            return tag;
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void savedDataReachesTheDisk(GameTestHelper h) throws IOException {
        Path file = h.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve("charboost_test.dat");
        Files.deleteIfExists(file);
        TestData data = h.getLevel().getServer().overworld().getDataStorage()
                .computeIfAbsent(tag -> new TestData(), TestData::new, "charboost_test");
        data.setDirty();
        h.getLevel().getServer().overworld().getDataStorage().save();
        h.assertFalse(data.isDirty(), "still marked unsaved");
        AsyncSaves.flush(20);
        h.assertTrue(Files.exists(file), "the file wasn't written");
        CompoundTag read = NbtIo.readCompressed(file.toFile());
        h.assertTrue(read.getCompound("data").getInt("value") == 12345, "file has the wrong data: " + read);
        h.assertFalse(Files.exists(file.resolveSibling("charboost_test.dat.charboost")), "temp file left behind");
        h.assertTrue(AsyncSaves.FILES.get() > 0, "it didn't go through the save thread");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void masterSwitchMakesItAllPassThrough(GameTestHelper h) {
        boolean was = Boost.enabled;
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Boost.enabled = false;
        try {
            for (Opt o : Opt.values()) {
                h.assertFalse(o.live(), o.key + " still live with the master switch off");
            }
            h.assertTrue(MobAi.interval(zombie) == 1, "mob still throttled with the master switch off");
        } finally {
            Boost.enabled = was;
        }
        h.succeed();
    }

}
