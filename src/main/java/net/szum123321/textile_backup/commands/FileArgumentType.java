package net.szum123321.textile_backup.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FileArgumentType implements ArgumentType<String> {
	private Collection<String> availableFiles;
	public static final DynamicCommandExceptionType NONEXISTANT_FILE_EXCEPTION = new DynamicCommandExceptionType((object) -> new TranslatableText("argument.file.notexist",(String)object));

	public FileArgumentType(Collection<String> availableFiles) {
		this.availableFiles = availableFiles;
	}

	public static String getString(final CommandContext<?> context, final String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(availableFiles, builder);
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		String s = reader.readString();

		if(!availableFiles.contains(s))
			throw NONEXISTANT_FILE_EXCEPTION.create(s);

		return s;
	}

	@Override
	public Collection<String> getExamples() {
		return availableFiles;
	}
}
