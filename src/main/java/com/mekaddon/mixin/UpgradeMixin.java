package com.mekaddon.mixin;

import com.mekaddon.registry.ModUpgrades;
import mekanism.api.Upgrade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Appends the mod's own upgrade constants to {@link Upgrade}'s constant pool.
 * <p>
 * <b>This must run while the enum class is still initialising.</b> {@code Upgrade.<clinit>} builds the private
 * {@code $VALUES} array first and then derives everything else from {@code Upgrade.values()} — the name lookup for
 * {@code CODEC}, the {@code BY_ID} table behind {@code STREAM_CODEC}, and (in a separate class)
 * {@code EnumUtils.UPGRADES}, which is what the upgrade window iterates. Replacing the {@code $values()} result here is
 * therefore enough for the new constants to appear everywhere a vanilla upgrade does.
 * <p>
 * {@code $values()} is private and static, which is fine: Mixin resolves it by name+descriptor and {@code <clinit>} is a
 * regular injection target.
 */
@Mixin(Upgrade.class)
public abstract class UpgradeMixin {

    @Redirect(
          method = "<clinit>",
          at = @At(value = "INVOKE", target = "Lmekanism/api/Upgrade;$values()[Lmekanism/api/Upgrade;"),
          remap = false
    )
    private static Upgrade[] mekaddon$appendModUpgrades() {
        return ModUpgrades.valuesWithModUpgrades();
    }
}
