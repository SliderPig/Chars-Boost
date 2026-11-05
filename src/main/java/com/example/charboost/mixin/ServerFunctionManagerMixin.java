package com.example.charboost.mixin;

import com.example.charboost.hook.Functions;
import java.util.Collection;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Times datapack functions by name and runs slow-listed tick ones less often. */
@Mixin(ServerFunctionManager.class)
public abstract class ServerFunctionManagerMixin {
    @Shadow
    @Final
    private static ResourceLocation TICK_FUNCTION_TAG;
    @Shadow
    public abstract int execute(CommandFunction function, CommandSourceStack source);

    @Redirect(method = "executeTagFunctions", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerFunctionManager;execute(Lnet/minecraft/commands/CommandFunction;Lnet/minecraft/commands/CommandSourceStack;)I"))
    private int charboost$tagFunction(ServerFunctionManager self, CommandFunction function, CommandSourceStack source, Collection<CommandFunction> functions, ResourceLocation tag) {
        if (tag == TICK_FUNCTION_TAG && !Functions.tickDue(function)) {
            return 0;
        }
        return execute(function, source);
    }

    @Inject(method = "execute(Lnet/minecraft/commands/CommandFunction;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/server/ServerFunctionManager$TraceCallbacks;)I", at = @At("HEAD"))
    private void charboost$start(CommandFunction function, CommandSourceStack source, ServerFunctionManager.TraceCallbacks tracer, CallbackInfoReturnable<Integer> cir) {
        Functions.open(function);
    }

    @Inject(method = "execute(Lnet/minecraft/commands/CommandFunction;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/server/ServerFunctionManager$TraceCallbacks;)I", at = @At("RETURN"))
    private void charboost$end(CommandFunction function, CommandSourceStack source, ServerFunctionManager.TraceCallbacks tracer, CallbackInfoReturnable<Integer> cir) {
        Functions.close();
    }
}
