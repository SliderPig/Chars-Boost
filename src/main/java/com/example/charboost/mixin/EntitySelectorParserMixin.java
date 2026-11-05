package com.example.charboost.mixin;

import com.example.charboost.hook.SelectorState;
import com.example.charboost.model.SelectorTags;
import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Notes which tags a selector insists on when it's read, while its text is still at hand. */
@Mixin(EntitySelectorParser.class)
public abstract class EntitySelectorParserMixin {
    @Shadow
    @Final
    private StringReader reader;
    @Shadow
    private int startPosition;

    @Inject(method = "parse", at = @At("RETURN"))
    private void charboost$readTags(CallbackInfoReturnable<EntitySelector> cir) {
        EntitySelector selector = cir.getReturnValue();
        if (selector instanceof SelectorState state) {
            int end = reader.getCursor();
            if (startPosition >= 0 && end <= reader.getString().length() && startPosition <= end) {
                state.charboost$tags(SelectorTags.required(reader.getString().substring(startPosition, end)));
            }
        }
    }
}
