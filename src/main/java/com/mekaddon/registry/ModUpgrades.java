package com.mekaddon.registry;

import com.mekaddon.MekAddon;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import mekanism.api.Upgrade;
import mekanism.api.text.EnumColor;
import mekanism.api.text.ILangEntry;

/**
 * Registers the mod's own {@link Upgrade} constant so the "Combining Slot Upgrade" item behaves exactly like a vanilla
 * Mekanism upgrade: it shows up in the upgrade window's <i>available upgrades</i> row, installs into the upgrade slot,
 * appears in the installed list and can be uninstalled again.
 * <p>
 * Mekanism ships {@link Upgrade} as a closed enum ({@code SPEED, ENERGY, FILTER, CHEMICAL, MUFFLING, ANCHOR,
 * STONE_GENERATOR}) and {@code UpgradeUtils#getItem} switches over the ordinals with an exhaustive
 * {@code tableswitch}. Extending the enum is therefore the only way to get a genuinely distinct upgrade type, and it
 * has to happen from inside {@code Upgrade.<clinit>} — before {@code $VALUES}, {@code BY_ID}, {@code CODEC} and
 * {@code EnumUtils.UPGRADES} are derived from {@code Upgrade.values()}, so every one of those picks the new constant
 * up for free.
 * <p>
 * The constant itself cannot be produced by {@code Constructor#newInstance} (the JDK explicitly blocks reflective
 * enum construction), so it is allocated with {@code Unsafe} and its fields are written directly. All of that is done
 * through reflection so the code still compiles under {@code --release 21}, which hides {@code sun.misc}.
 */
public final class ModUpgrades {

    /** Names the upgrade. Also the translation suffix and the key used in the available-upgrades tooltip. */
    public static final String NAME = "combining_slots";

    /** Ordinal of the new constant. Must directly follow {@code STONE_GENERATOR} (= 6). */
    public static final int ORDINAL = 7;

    private static Upgrade combiningSlots;

    private ModUpgrades() {
    }

    /** The mod's upgrade constant, or {@code null} if it could not be registered. */
    public static Upgrade combiningSlots() {
        if (combiningSlots == null) {
            // Touching Upgrade.values() forces the enum's static initialiser to run, which is what registers the
            // constant. Safe to call from anywhere except from inside that initialiser itself.
            Upgrade[] ignored = Upgrade.values();
        }
        return combiningSlots;
    }

    public static boolean isCombining(Upgrade upgrade) {
        return combiningSlots != null && upgrade == combiningSlots;
    }

    /**
     * Called from {@code Upgrade.<clinit>} (via {@code UpgradeMixin}) in place of {@code Upgrade.$values()}.
     *
     * @return the enum constant array {@code $VALUES} should be built from, with the mod's constant appended.
     */
    public static Upgrade[] valuesWithCombiningUpgrade() {
        // At this point all seven vanilla constants are already assigned to their static fields, so reading them is safe.
        Upgrade[] base = {
              Upgrade.SPEED, Upgrade.ENERGY, Upgrade.FILTER, Upgrade.CHEMICAL,
              Upgrade.MUFFLING, Upgrade.ANCHOR, Upgrade.STONE_GENERATOR
        };
        if (combiningSlots == null) {
            try {
                combiningSlots = createConstant();
            } catch (Throwable t) {
                MekAddon.LOGGER.error("Could not register the Combining Slot upgrade; the upgrade will be disabled", t);
                return base;
            }
        }
        Upgrade[] extended = Arrays.copyOf(base, base.length + 1);
        extended[base.length] = combiningSlots;
        MekAddon.LOGGER.info("Registered the '{}' upgrade constant at ordinal {}", NAME, combiningSlots.ordinal());
        return extended;
    }

    private static Upgrade createConstant() throws Exception {
        // Two independent routes, because both have environment-dependent failure modes: MethodHandles needs the
        // Mekanism module to be open for private lookup, while Unsafe needs sun.misc to be reachable (it lives in
        // jdk.unsupported, which opens sun.misc, so that one is expected to work).
        Upgrade created = viaMethodHandles();
        return created != null ? created : viaUnsafe();
    }

    private static Upgrade viaMethodHandles() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Upgrade.class, MethodHandles.lookup());
            MethodHandle ctor = lookup.findConstructor(Upgrade.class, MethodType.methodType(void.class,
                  String.class, int.class, String.class, ILangEntry.class, ILangEntry.class, int.class, EnumColor.class));
            return (Upgrade) ctor.invoke("COMBINING_SLOTS", ORDINAL, NAME, langKey(), descLangKey(), 1, EnumColor.YELLOW);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Allocates the constant without running a constructor and writes its fields directly.
     * <p>
     * {@code Constructor#newInstance} explicitly refuses to build enum objects, which is why the instance has to be
     * allocated and populated by hand. Everything goes through reflection so the code still compiles under
     * {@code --release 21}, which hides {@code sun.misc}.
     */
    private static Upgrade viaUnsafe() throws Exception {
        Class<?> unsafeClass;
        try {
            unsafeClass = Class.forName("sun.misc.Unsafe");
        } catch (ClassNotFoundException e) {
            unsafeClass = Class.forName("sun.misc.Unsafe", true, ClassLoader.getPlatformClassLoader());
        }
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);

        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        Method objectFieldOffset = unsafeClass.getMethod("objectFieldOffset", Field.class);
        Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
        Method putInt = unsafeClass.getMethod("putInt", Object.class, long.class, int.class);

        Upgrade constant = (Upgrade) allocateInstance.invoke(unsafe, Upgrade.class);

        // java.lang.Enum#name / #ordinal drive Enum#name(), Enum#ordinal() and Enum.valueOf lookups.
        putObject.invoke(unsafe, constant, objectFieldOffset.invoke(unsafe, Enum.class.getDeclaredField("name")),
              "COMBINING_SLOTS");
        putInt.invoke(unsafe, constant, objectFieldOffset.invoke(unsafe, Enum.class.getDeclaredField("ordinal")), ORDINAL);

        set(unsafe, objectFieldOffset, putObject, constant, "name", NAME);
        set(unsafe, objectFieldOffset, putObject, constant, "langKey", langKey());
        set(unsafe, objectFieldOffset, putObject, constant, "descLangKey", descLangKey());
        set(unsafe, objectFieldOffset, putInt, constant, "maxStack", 1);
        set(unsafe, objectFieldOffset, putObject, constant, "color", EnumColor.YELLOW);

        return constant;
    }

    private static ILangEntry langKey() {
        return () -> "upgrade.mekaddon." + NAME;
    }

    private static ILangEntry descLangKey() {
        return () -> "upgrade.mekaddon." + NAME + ".desc";
    }

    private static void set(Object unsafe, Method objectFieldOffset, Method setter, Object target, String field,
          Object value) throws Exception {
        long offset = (Long) objectFieldOffset.invoke(unsafe, Upgrade.class.getDeclaredField(field));
        setter.invoke(unsafe, target, offset, value);
    }
}
