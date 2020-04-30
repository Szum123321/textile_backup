package net.szum123321.textile_backup.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.CommandSource;
import net.minecraft.text.TranslatableText;
import net.szum123321.textile_backup.TextileBackup;
import net.szum123321.textile_backup.core.RestoreHelper;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class FileArgumentType implements ArgumentType<String> {
	private Collection<String> availableFiles;
	public static final DynamicCommandExceptionType NONEXISTANT_FILE_EXCEPTION = new DynamicCommandExceptionType((object) -> new TranslatableText("argument.file.notexists", object));

	public static FileArgumentType file() {
		return new FileArgumentType();
	}

	private FileArgumentType() {
	}

	public static String getFile(final CommandContext<?> context, final String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if(context.getSource() instanceof CommandSource) {
			availableFiles = RestoreHelper.getAvailableBackups(TextileBackup.worldPath.getName());

			for (String file : availableFiles) {
				if(file.startsWith(builder.getRemaining()))
					builder.suggest(file);
			}

			return builder.buildFuture();
		}

		return Suggestions.empty();
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		String f = reader.readUnquotedString();

		if(!availableFiles.contains(f))
			throw NONEXISTANT_FILE_EXCEPTION.create(f);

		return f;
	}

	@Override
	public Collection<String> getExamples() {
		return availableFiles;
	}
}
