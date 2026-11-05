package com.example.charboost.hook;

import net.minecraft.world.entity.Entity;
import net.threetag.palladium.util.property.EntityPropertyHandler;

/** Added to Palladium's "register your properties" script event. */
public interface RegisterEventAccess {
    Entity charboost$entity();

    EntityPropertyHandler charboost$handler();
}
