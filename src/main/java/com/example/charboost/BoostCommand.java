package com.example.charboost;

import com.example.charboost.hook.AsyncSaves;
import com.example.charboost.hook.Census;
import com.example.charboost.hook.Functions;
import com.example.charboost.hook.Scripts;
import com.example.charboost.mixin.MixinGate;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/** /charboost, ops only. */
public final class BoostCommand {
    private static final int PINK = 0xFF7AB8;
    private static final int PLUM = 0xC89BD9;

    private BoostCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("charboost").requires(s -> s.hasPermission(2))
                .executes(BoostCommand::status)
                .then(Commands.literal("status").executes(BoostCommand::status))
                .then(Commands.literal("on").executes(c -> master(c, true))
                        .then(Commands.argument("name", StringArgumentType.word()).suggests(BoostCommand::names).executes(c -> one(c, true))))
                .then(Commands.literal("off").executes(c -> master(c, false))
                        .then(Commands.argument("name", StringArgumentType.word()).suggests(BoostCommand::names).executes(c -> one(c, false))))
                .then(Commands.literal("level")
                        .then(Commands.literal("auto").executes(c -> pin(c, -1)))
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 3)).executes(c -> pin(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("scripts").executes(c -> scripts(c, 12))
                        .then(Commands.literal("reset").executes(BoostCommand::resetScripts))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 200)).executes(c -> scripts(c, IntegerArgumentType.getInteger(c, "count")))))
                .then(Commands.literal("functions").executes(c -> functions(c, 12))
                        .then(Commands.literal("reset").executes(BoostCommand::resetFunctions))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 200)).executes(c -> functions(c, IntegerArgumentType.getInteger(c, "count")))))
                .then(Commands.literal("reset").executes(BoostCommand::resetAll)));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> names(CommandContext<CommandSourceStack> c, com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        List<String> keys = new ArrayList<>();
        for (Opt o : Opt.values()) {
            keys.add(o.key);
        }
        return SharedSuggestionProvider.suggest(keys, b);
    }

    private static void say(CommandContext<CommandSourceStack> c, Component line) {
        c.getSource().sendSuccess(() -> line, false);
    }

    private static Component text(String s, int color) {
        return Component.literal(s).withStyle(style -> style.withColor(color));
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        String pressure = Boost.level + "/3" + (Boost.pinnedLevel >= 0 ? " (set by hand)" : " (auto)");
        say(c, text("Char's Boost", PINK).copy()
                .append(text(String.format(Locale.ROOT, "  %.1f ms a tick, %.1f TPS, pressure %s", Boost.mspt, Boost.tps, pressure), PLUM)));
        if (!Boost.enabled) {
            say(c, Component.literal("Switched off. /charboost on to start it again.").withStyle(ChatFormatting.RED));
        }
        for (Opt o : Opt.values()) {
            String state;
            ChatFormatting color;
            if (!o.on) {
                state = "off";
                color = ChatFormatting.DARK_GRAY;
            } else if (o.live()) {
                state = "on";
                color = ChatFormatting.GREEN;
            } else {
                state = "ready";
                color = ChatFormatting.GRAY;
            }
            String hook = o.seen == 0 && o != Opt.SAVES && o != Opt.AFK ? " (not run yet)" : "";
            say(c, Component.literal(String.format(Locale.ROOT, "%-15s ", o.key)).withStyle(style -> style.withColor(PINK))
                    .append(Component.literal(String.format(Locale.ROOT, "%-5s ", state)).withStyle(color))
                    .append(text(String.format(Locale.ROOT, "%s skips, saved %.2f ms a tick, %s in all%s",
                            shortCount(o.skips), o.savedMsPerTick, shortMillis(o.savedNs), hook), PLUM)));
        }
        say(c, text(String.format(Locale.ROOT, "%d hooks in, %d files saved off the main thread (%d waiting), tag count %s, %d entities counted",
                MixinGate.APPLIED.size(), AsyncSaves.FILES.get(), AsyncSaves.pending(),
                Census.working() ? "ok" : "not in use", Census.loaded), PLUM));
        return 1;
    }

    private static String shortCount(long n) {
        if (n >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", n / 1e6);
        }
        if (n >= 10_000) {
            return String.format(Locale.ROOT, "%dk", n / 1000);
        }
        return Long.toString(n);
    }

    private static String shortMillis(long nanos) {
        double ms = nanos / 1e6;
        return ms >= 10_000 ? String.format(Locale.ROOT, "%.0f s", ms / 1000) : String.format(Locale.ROOT, "%.0f ms", ms);
    }

    private static int master(CommandContext<CommandSourceStack> c, boolean on) {
        Boost.enabled = on;
        Cfg.saveEnabled();
        if (!on) {
            Boost.level = 0;
        }
        String extra = on && MixinGate.APPLIED.isEmpty() ? " It was off when the server started, so restart for it to take effect." : "";
        say(c, text("Char's Boost is " + (on ? "on" : "off") + "." + extra, PINK));
        return 1;
    }

    private static int one(CommandContext<CommandSourceStack> c, boolean on) {
        String name = StringArgumentType.getString(c, "name");
        Opt o = Opt.byKey(name);
        if (o == null) {
            c.getSource().sendFailure(Component.literal("No optimisation called " + name));
            return 0;
        }
        o.on = on;
        Cfg.save(o);
        say(c, text(o.key + " is " + (on ? "on" : "off") + ". " + o.about, PINK));
        return 1;
    }

    private static int pin(CommandContext<CommandSourceStack> c, int level) {
        Boost.pinnedLevel = level;
        if (level >= 0) {
            Boost.level = level;
        }
        say(c, text(level < 0 ? "Pressure is automatic again." : "Pressure held at " + level + " until /charboost level auto.", PINK));
        return 1;
    }

    private static int scripts(CommandContext<CommandSourceStack> c, int count) {
        long ticks = Scripts.ticksCounted();
        long all = 0;
        List<Scripts.Entry> top = Scripts.top();
        for (Scripts.Entry e : top) {
            all += e.cost.total();
        }
        say(c, text(String.format(Locale.ROOT, "KubeJS scripts: %.2f ms a tick over %d ticks. Tick scripts marked T, on the slow list S.", all / 1e6 / ticks, ticks), PINK));
        int shown = 0;
        for (Scripts.Entry e : top) {
            if (shown++ >= count || e.cost.calls() == 0) {
                break;
            }
            say(c, text(String.format(Locale.ROOT, "%6.3f ms/t %6.0f us/call %s%s %s", e.cost.total() / 1e6 / ticks, e.cost.average() / 1e3,
                    e.tickEvent ? "T" : " ", e.slow ? "S" : " ", e.name), PLUM));
        }
        return 1;
    }

    private static int functions(CommandContext<CommandSourceStack> c, int count) {
        long ticks = Functions.ticksCounted();
        long all = 0;
        List<Functions.Entry> top = Functions.top();
        for (Functions.Entry e : top) {
            all += e.cost.total();
        }
        say(c, text(String.format(Locale.ROOT, "Datapack functions: %.2f ms a tick over %d ticks. On the slow list S.", all / 1e6 / ticks, ticks), PINK));
        int shown = 0;
        for (Functions.Entry e : top) {
            if (shown++ >= count || e.cost.calls() == 0) {
                break;
            }
            say(c, text(String.format(Locale.ROOT, "%6.3f ms/t %6.0f us/call %s %s", e.cost.total() / 1e6 / ticks, e.cost.average() / 1e3,
                    e.slow ? "S" : " ", e.name), PLUM));
        }
        return 1;
    }

    private static int resetScripts(CommandContext<CommandSourceStack> c) {
        Scripts.reset();
        say(c, text("Script timings cleared.", PINK));
        return 1;
    }

    private static int resetFunctions(CommandContext<CommandSourceStack> c) {
        Functions.reset();
        say(c, text("Function timings cleared.", PINK));
        return 1;
    }

    private static int resetAll(CommandContext<CommandSourceStack> c) {
        Scripts.reset();
        Functions.reset();
        for (Opt o : Opt.values()) {
            o.skips = 0;
            o.savedNs = 0;
            o.windowStart = 0;
            o.savedMsPerTick = 0;
        }
        say(c, text("All timings and scores cleared.", PINK));
        return 1;
    }
}
