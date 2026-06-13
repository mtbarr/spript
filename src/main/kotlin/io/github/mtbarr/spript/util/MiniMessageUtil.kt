package io.github.mtbarr.spript.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags

object MiniMessageUtil {

    private val TAG_RESOLVER: TagResolver = TagResolver.builder()
        .resolver(StandardTags.color())
        .resolver(StandardTags.decorations())
        .resolver(StandardTags.gradient())
        .build()

    val DEFAULT_MINI_MESSAGE: MiniMessage = MiniMessage.builder()
        .tags(TAG_RESOLVER)
        .postProcessor { component -> component.decoration(TextDecoration.ITALIC, false) }
        .build()

    fun miniMessage(message: String): Component {
        return DEFAULT_MINI_MESSAGE.deserialize(message)
    }

    fun serialize(component: Component): String {
        return DEFAULT_MINI_MESSAGE.serialize(component)
    }

    fun miniMessage(message: String, resolver: TagResolver): Component {
        return DEFAULT_MINI_MESSAGE.deserialize(message, resolver)
    }

    fun serializeMultilined(vararg lines: String): Component {
        return lines.map { miniMessage(it) }
            .reduceOrNull { cur, next -> cur.append(Component.newline()).append(next) }
            ?: Component.empty()
    }

    fun serializeMultilined(lines: List<String>): Component {
        return lines.map { miniMessage(it) }
            .reduceOrNull { cur, next -> cur.append(Component.newline()).append(next) }
            ?: Component.empty()
    }
}
