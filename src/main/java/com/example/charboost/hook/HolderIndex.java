package com.example.charboost.hook;

import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;

/** Added to Palladium's power holder: its abilities by type. */
public interface HolderIndex {
    AbilityInstance[] charboost$ofType(Ability ability);
}
