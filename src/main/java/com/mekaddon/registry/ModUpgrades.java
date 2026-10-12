package com.mekaddon.registry;

import com.mekaddon.MekAddon;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Upgrade;
import mekanism.api.text.EnumColor;
import mekanism.api.text.ILangEntry;

/**
 * Registers the mod's own {@link Upgrade} constants so the mod's items behave exactly like vanilla Mekanism upgrades:
 * they show up in the upgrade window's <i>available upgrades</i> row, install into the upgrade slot, appear in the
 * installed list and can be uninstalled again.
 * <p>
 * Two constants are added, both appended after {@code STONE_GENERATOR}:
 * <ul>
 *     <li>{@code COMBINING_SLOTS} (ordinal 7, max 1) — replaces the factory's single shared secondary input slot with
 *     one per process. See {@code CombiningFactoryExtraSlotsMixin}.</li>
 *     <li>{@code EXTRA_STACK} (ordinal 8, max 8) — multiplies the stack limit of those per-process secondary input
 *     slots by {@code 2^installed}. See {@code com.mekaddon.state.ExtraStackState}.</li>
 * </ul>
 * Mekanism ships {@link Upgrade} as a closed enum ({@code SPEED, ENERGY, FILTER, CHEMICAL, MUFFLING, ANCHOR,
 * STONE_GENERATOR}) and {@code UpgradeUtils#getItem} switches over the ordinals with an exhaustive
 * {@code tableswitch}. Extending the enum is therefore the only way to get a genuinely distinct upgrade type, and it
 * has to happen from inside {@code Upgrade.<clinit>} — before {@code $VALUES}, {@code BY_ID}, {@code CODEC} and
 * {@code EnumUtils.UPGRADES} are derived from {@code Upgrade.values()}, so every one of those picks the new constants
 * up for free.
 * <p>
 * The constants themselves cannot be produced by {@code Constructor#newInstance} (the JDK explicitly blocks reflective
 * enum construction), so each is allocated with {@code Unsafe} and its fields are written directly. All of that is done
 * through reflection so the code still compiles under {@code --release 21}, which hides {@code sun.misc}.
 */
public final class ModUpgrades {

    // ------------------------------------------------------------------ combining slot upgrade (ordinal 7)

    /** Names the "extra secondary row" upgrade. Also the translation suffix and the key used in tooltips. */
    public static final String COMBINING_NAME = "combining_slots";

    /** Ordinal of that constant. Must directly follow {@code STONE_GENERATOR} (= 6). */
    public static final int COMBINING_ORDINAL = 7;

    /** Only one is ever installed: a second one would add nothing. */
    public static final int COMBINING_MAX = 1;

    // ------------------------------------------------------------------ extra stack upgrade (ordinal 8)

    /** Names the "stack limit multiplier" upgrade. */
    public static final String STACK_NAME = "extra_stack";

    /** Ordinal of that constant. Must directly follow {@link #COMBINING_ORDINAL}. */
    public static final int STACK_ORDINAL = 8;

    /** Levels 1..8 give a stack limit of x2 .. x256. */
    public static final int STACK_MAX = 8;

    /** Largest stack multiplier the upgrade can reach, i.e. {@code 2^STACK_MAX}. */
    public static final int STACK_MAX_MULTIPLIER = 1 << STACK_MAX;

    private static Upgrade combiningSlots;
    private static Upgrade extraStack;

    private ModUpgrades() {
    }

    /** The "extra secondary row" upgrade constant, or {@code null} if it could not be registered. */
    public static Upgrade combiningSlots() {
        if (combiningSlots == null) {
            // Touching Upgrade.values() forces the enum's static initialiser to run, which is what registers the
            // constants. Safe to call from anywhere except from inside that initialiser itself.
            Upgrade[] ignored = Upgrade.values();
        }
        return combiningSlots;
    }

    /** The "extra stack upgrade" constant, or {@code null} if it could not be registered. */
    public static Upgrade extraStack() {
        if (extraStack == null) {
            Upgrade[] ignored = Upgrade.values();
        }
        return extraStack;
    }

    public static boolean isCombining(Upgrade upgrade) {
        return combiningSlots != null && upgrade == combiningSlots;
    }

    public static boolean isExtraStack(Upgrade upgrade) {
        return extraStack != null && upgrade == extraStack;
    }

    /** {@code true} for either of the mod's upgrade constants. */
    public static boolean isModUpgrade(Upgrade upgrade) {
        return isCombining(upgrade) || isExtraStack(upgrade);
    }

    /**
     * Called from {@code Upgrade.<clinit>} (via {@code UpgradeMixin}) in place of {@code Upgrade.$values()}.
     *
     * @return the enum constant array {@code $VALUES} should be built from, with the mod's constants appended.
     */
    public static Upgrade[] valuesWithModUpgrades() {
        // At this point all seven vanilla constants are already assigned to their static fields, so reading them is safe.
        List<Upgrade> all = new ArrayList<>(9);
        all.add(Upgrade.SPEED);
        all.add(Upgrade.ENERGY);
        all.add(Upgrade.FILTER);
        all.add(Upgrade.CHEMICAL);
        all.add(Upgrade.MUFFLING);
        all.add(Upgrade.ANCHOR);
        all.add(Upgrade.STONE_GENERATOR);
        // Ordinal order is load bearing (UpgradeUtils switches over ordinals and BY_ID indexes by ordinal), so the two
        // constants are appended in the exact order their ordinals declare. A failure to create one leaves a gap rather
        // than shifting the other out of place; the affected feature is then simply disabled.
        Upgrade combining = register(combiningSlots, COMBINING_NAME, COMBINING_ORDINAL, COMBINING_MAX, EnumColor.YELLOW);
        combiningSlots = combining;
        Upgrade stack = register(extraStack, STACK_NAME, STACK_ORDINAL, STACK_MAX, EnumColor.PURPLE);
        extraStack = stack;
        if (combining != null) {
            all.add(combining);
        }
        if (stack != null) {
            all.add(stack);
        }
        return all.toArray(new Upgrade[0]);
    }

    private static Upgrade register(Upgrade existing, String name, int ordinal, int maxStack, EnumColor color) {
        if (existing != null) {
            return existing;
        }
        try {
            Upgrade created = createConstant(name, ordinal, maxStack, color);
            MekAddon.LOGGER.info("Registered the '{}' upgrade constant at ordinal {}", name, created.ordinal());
            return created;
        } catch (Throwable t) {
            MekAddon.LOGGER.error("Could not register the '{}' upgrade; that feature will be disabled", name, t);
            return null;
        }
    }

    private static Upgrade createConstant(String name, int ordinal, int maxStack, EnumColor color) throws Exception {
        // Two independent routes, because both have environment-dependent failure modes: MethodHandles needs the
        // Mekanism module to be open for private lookup, while Unsafe needs sun.misc to be reachable (it lives in
        // jdk.unsupported, which opens sun.misc, so that one is expected to work).
        Upgrade created = viaMethodHandles(name, ordinal, maxStack, color);
        return created != null ? created : viaUnsafe(name, ordinal, maxStack, color);
    }

    private static Upgrade viaMethodHandles(String name, int ordinal, int maxStack, EnumColor color) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Upgrade.class, MethodHandles.lookup());
            MethodHandle ctor = lookup.findConstructor(Upgrade.class, MethodType.methodType(void.class,
                  String.class, int.class, String.class, ILangEntry.class, ILangEntry.class, int.class, EnumColor.class));
            return (Upgrade) ctor.invoke(enumName(name), ordinal, name, langKey(name), descLangKey(name), maxStack, color);
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
    private static Upgrade viaUnsafe(String name, int ordinal, int maxStack, EnumColor color) throws Exception {
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
              enumName(name));
        putInt.invoke(unsafe, constant, objectFieldOffset.invoke(unsafe, Enum.class.getDeclaredField("ordinal")), ordinal);

        set(unsafe, objectFieldOffset, putObject, constant, "name", name);
        set(unsafe, objectFieldOffset, putObject, constant, "langKey", langKey(name));
        set(unsafe, objectFieldOffset, putObject, constant, "descLangKey", descLangKey(name));
        set(unsafe, objectFieldOffset, putInt, constant, "maxStack", maxStack);
        set(unsafe, objectFieldOffset, putObject, constant, "color", color);

        return constant;
    }

    /** Java-level enum constant name, i.e. {@code combining_slots -> COMBINING_SLOTS}. */
    private static String enumName(String name) {
        return name.toUpperCase(java.util.Locale.ROOT);
    }

    private static ILangEntry langKey(String name) {
        return () -> "upgrade.mekaddon." + name;
    }

    private static ILangEntry descLangKey(String name) {
        return () -> "upgrade.mekaddon." + name + ".desc";
    }

    private static void set(Object unsafe, Method objectFieldOffset, Method setter, Object target, String field,
          Object value) throws Exception {
        long offset = (Long) objectFieldOffset.invoke(unsafe, Upgrade.class.getDeclaredField(field));
        setter.invoke(unsafe, target, offset, value);
    }
}
