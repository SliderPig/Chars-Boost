package com.example.charboost.hook;

import com.example.charboost.Boost;
import com.example.charboost.Cfg;
import com.example.charboost.Opt;
import com.example.charboost.model.AiRules;
import com.example.charboost.model.RollingCost;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraftforge.common.Tags;

/**
 * The mob AI throttle. A mob far from every player runs its goals, brain and item search less often. Its
 * walking keeps going every tick (only the deciding gets slower), so it doesn't stutter.
 * Anything that matters is left alone: mobs with a target or just hit, bosses, tamed and named mobs,
 * raiders, mobs being ridden or in water, and anything holding a Palladium power.
 */
public final class MobAi {
    public static final int TARGET_GOALS = 0;
    public static final int GOALS = 1;
    public static final int TARGET_RUNNING = 2;
    public static final int RUNNING = 3;
    public static final int BRAIN = 4;
    public static final int PICKUP = 5;
    public static final int PUSH = 6;
    public static final int SLOTS = 7;

    private static final RollingCost GOAL_COST = new RollingCost();
    private static final RollingCost BRAIN_COST = new RollingCost();
    private static final Reference2BooleanOpenHashMap<EntityType<?>> EXEMPT_TYPES = new Reference2BooleanOpenHashMap<>();
    private static List<String> exemptSeen = null;

    private MobAi() {
    }

    /** 1 = every tick. Cheap: the full look only happens about once a second per mob. */
    public static int interval(Mob mob) {
        if (!Opt.MOB_AI.live()) {
            return 1;
        }
        // These can change between looks and must take effect straight away.
        if (mob.hurtTime > 0 || mob.getTarget() != null) {
            ((MobState) mob).charboost$nextLook(0);
            return 1;
        }
        MobState state = (MobState) mob;
        long now = Boost.tick;
        if (now >= state.charboost$nextLook()) {
            state.charboost$nextLook(now + 16 + (mob.getId() & 7));
            state.charboost$interval(work(mob));
        }
        return state.charboost$interval();
    }

    private static int work(Mob mob) {
        if (exempt(mob)) {
            return 1;
        }
        double best = Double.MAX_VALUE;
        List<? extends Player> players = mob.level().players();
        for (int i = 0; i < players.size(); i++) {
            double d = players.get(i).distanceToSqr(mob);
            if (d < best) {
                best = d;
            }
        }
        return AiRules.interval(best, Boost.level, Cfg.maxInterval);
    }

    private static boolean exempt(Mob mob) {
        return exemptReason(mob) != null;
    }

    /** Why this mob is left alone, or null if it isn't. */
    public static String exemptReason(Mob mob) {
        if (mob.isVehicle() || mob.isPassenger() || mob.isLeashed()) {
            return "riding or leashed";
        }
        if (mob.hasCustomName()) {
            return "named";
        }
        if (mob.isInWater() || mob.isInLava()) {
            return "in water or lava";
        }
        if (mob.getLastHurtByMob() != null) {
            return "recently hurt";
        }
        if (mob.getMaxHealth() >= Cfg.bossHealth || exemptType(mob.getType())) {
            return "boss or listed";
        }
        if (mob instanceof TamableAnimal tame && tame.isTame() || mob instanceof AbstractHorse horse && horse.isTamed()) {
            return "tamed";
        }
        if (mob instanceof Raider raider && (raider.hasActiveRaid() || raider.isPatrolLeader())) {
            return "raid";
        }
        if (mob instanceof Villager villager && villager.isTrading()) {
            return "trading";
        }
        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            return "has a target";
        }
        Set<String> tags = mob.getTags();
        if (!tags.isEmpty()) {
            for (String tag : Cfg.exemptTags) {
                if (tags.contains(tag)) {
                    return "tag " + tag;
                }
            }
        }
        return PowersHook.holdsPowers(mob) ? "holds a power" : null;
    }

    private static boolean exemptType(EntityType<?> type) {
        if (exemptSeen != Cfg.exemptMobs) {
            EXEMPT_TYPES.clear();
            exemptSeen = Cfg.exemptMobs;
        }
        if (EXEMPT_TYPES.containsKey(type)) {
            return EXEMPT_TYPES.getBoolean(type);
        }
        ResourceLocation id = EntityType.getKey(type);
        boolean out = type.is(Tags.EntityTypes.BOSSES);
        for (String entry : Cfg.exemptMobs) {
            if (entry.equals(id.toString()) || entry.endsWith(":*") && entry.substring(0, entry.length() - 2).equals(id.getNamespace())) {
                out = true;
            }
        }
        EXEMPT_TYPES.put(type, out);
        return out;
    }

    /** Called instead of GoalSelector.tick(). */
    public static void goals(Mob mob, GoalSelector selector, int slot) {
        Opt.MOB_AI.seen++;
        int interval = interval(mob);
        if (interval <= 1) {
            selector.tick();
            return;
        }
        long[] last = ((MobState) mob).charboost$last();
        if (AiRules.dueGoals(Boost.tick, last[slot], interval)) {
            last[slot] = Boost.tick;
            long start = System.nanoTime();
            selector.tick();
            GOAL_COST.add(System.nanoTime() - start);
        } else {
            Opt.MOB_AI.skips++;
            Opt.MOB_AI.savedNs += (long) GOAL_COST.average();
        }
    }

    /** Called instead of GoalSelector.tickRunningGoals(). */
    public static void running(Mob mob, GoalSelector selector, boolean all, int slot) {
        int interval = interval(mob);
        if (interval <= 1) {
            selector.tickRunningGoals(all);
            return;
        }
        long[] last = ((MobState) mob).charboost$last();
        if (AiRules.dueGoals(Boost.tick, last[slot], interval)) {
            last[slot] = Boost.tick;
            selector.tickRunningGoals(all);
        } else {
            Opt.MOB_AI.skips++;
        }
    }

    /** True when the brain should tick now. If not, its memories still count down (see BrainMixin). */
    public static boolean brainDue(LivingEntity entity) {
        Opt.MOB_AI.seen++;
        if (!(entity instanceof Mob mob)) {
            return true;
        }
        int interval = interval(mob);
        if (interval <= 1 || mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            return true;
        }
        long[] last = ((MobState) mob).charboost$last();
        if (AiRules.dueEvery(Boost.tick, last[BRAIN], interval)) {
            last[BRAIN] = Boost.tick;
            return true;
        }
        Opt.MOB_AI.skips++;
        Opt.MOB_AI.savedNs += (long) BRAIN_COST.average();
        return false;
    }

    public static void brainCost(long nanos) {
        BRAIN_COST.add(nanos);
    }

    public static boolean pickupDue(Mob mob) {
        int interval = interval(mob);
        if (interval <= 1) {
            return true;
        }
        long[] last = ((MobState) mob).charboost$last();
        if (AiRules.dueEvery(Boost.tick, last[PICKUP], interval)) {
            last[PICKUP] = Boost.tick;
            return true;
        }
        return false;
    }

    /** Should this living thing skip looking for things to push this tick? Players never do. */
    public static boolean skipPush(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || mob.level().isClientSide) {
            return false;
        }
        Opt.MOB_PUSH.seen++;
        if (!Opt.MOB_PUSH.live()) {
            return false;
        }
        int interval = Math.max(interval(mob), AiRules.pushAtLeast(Boost.level));
        if (interval <= 1 || mob.getTarget() != null) {
            return false;
        }
        long[] last = ((MobState) mob).charboost$last();
        if (AiRules.dueEvery(Boost.tick, last[PUSH], interval)) {
            last[PUSH] = Boost.tick;
            return false;
        }
        Opt.MOB_PUSH.skips++;
        Opt.MOB_PUSH.savedNs += 1500;
        return true;
    }
}
