package dev.rinchan.keepmysword.item;

/** Transient decode context, never serialized or used as item identity. */
public interface TemplateContext {
    void kms$context(com.mojang.serialization.DynamicOps<?> ops);
}
