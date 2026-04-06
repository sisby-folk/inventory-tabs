package folk.sisby.inventory_tabs.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

public record RegistryMatcher<T>(Either<Either<RegistryEntry<T>, TagKey<T>>, Pair<String, String>> value) {
    public int priority() {
        return value.left().isPresent() ? (value.left().orElseThrow().left().isPresent() ? 0 : 1) : 2;
    }

    public static @Nullable <T> RegistryMatcher<T> fromRegistryString(DynamicRegistryManager manager, RegistryKey<? extends Registry<T>> registry, String value) {
        if (value.contains("*")) { // Prefix/Suffix
            String[] split = value.split("\\*");
            if (split.length == 1) {
                if (value.startsWith("*")) { // Suffix
                    return new RegistryMatcher<>(Either.right(new Pair<>("", split[0])));
                } else if (value.endsWith("*")) { // Prefix
                    return new RegistryMatcher<>(Either.right(new Pair<>(split[0], "")));
                }
            } else if (split.length == 2) {
                return new RegistryMatcher<>(Either.right(new Pair<>(split[0], split[1])));
            }
            return null;
        } else if (value.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(value.substring(1));
            return tagId != null ? new RegistryMatcher<>(Either.left(Either.right(TagKey.of(registry, tagId)))) : null;
        } else {
            Identifier id = Identifier.tryParse(value);
            if (id == null) return null;
            return manager.getOptional(registry).orElseThrow().getOptional(RegistryKey.of(registry, id)).map(h -> new RegistryMatcher<>(Either.left(Either.left(h)))).orElse(null);
        }
    }

    public boolean is(RegistryEntry<T> value) {
        return this.value.map(e -> e.map((v) -> v.equals(value), value::isIn), pair -> value.getKey().orElseThrow().getValue().toString().startsWith(pair.getLeft()) && value.getKey().orElseThrow().getValue().toString().endsWith(pair.getRight()));
    }
}
