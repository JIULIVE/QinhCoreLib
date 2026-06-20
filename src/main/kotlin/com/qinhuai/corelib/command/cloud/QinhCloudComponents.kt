package com.qinhuai.corelib.command.cloud

import org.bukkit.command.CommandSender
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import java.util.function.Supplier

object QinhCloudComponents {

    private val stringDescriptor: ParserDescriptor<CommandSender, String> =
        StringParser.stringParser()

    fun requiredString(
        name: String,
        suggestions: Supplier<Iterable<String>>? = null,
    ): CommandComponent.Builder<CommandSender, String> {
        val builder = CommandComponent.builder(name, stringDescriptor)
        if (suggestions != null) {
            builder.suggestionProvider(
                BlockingSuggestionProvider.Strings<CommandSender> { _, _ -> suggestions.get() },
            )
        }
        return builder
    }

    fun optionalString(
        name: String,
        suggestions: Supplier<Iterable<String>>? = null,
    ): CommandComponent.Builder<CommandSender, String> {
        val builder = CommandComponent.builder(name, stringDescriptor).optional()
        if (suggestions != null) {
            builder.suggestionProvider(
                BlockingSuggestionProvider.Strings<CommandSender> { _, _ -> suggestions.get() },
            )
        }
        return builder
    }
}
