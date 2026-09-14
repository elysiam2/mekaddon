package com.mekaddon.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * Gates optional-integration mixin configs on the target classes actually being present.
 * <p>
 * Declared only by {@code mekaddon.mekenergistics.mixins.json}. Every target in that config belongs to another mod,
 * and the config is loaded unconditionally (Mixin has no mod-condition concept), so without this gate an absent
 * integration would at best spam "target not found" warnings. {@link #shouldApplyMixin} is consulted once per
 * target class, which is exactly the granularity needed for a multi-target compat mixin.
 * <p>
 * Loaded by the Mixin service long before FML constructs any mod, so this class must only touch Mixin/ASM APIs -
 * never Minecraft, NeoForge or mod classes.
 */
public class MekAddonMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            return MixinService.getService().getBytecodeProvider().getClassNode(targetClassName) != null;
        } catch (Throwable t) {
            // ClassNotFoundException when the mod is absent, but also any IO/linkage hiccup: in every case the safe
            // answer is "do not apply", because an applied-but-unresolvable mixin is a hard crash instead.
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
