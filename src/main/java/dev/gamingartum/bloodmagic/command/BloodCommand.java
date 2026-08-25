package dev.gamingartum.bloodmagic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.gamingartum.bloodmagic.buff.BuffManager;
import dev.gamingartum.bloodmagic.data.ActiveBuffEntry;
import dev.gamingartum.bloodmagic.data.BloodBuff;
import dev.gamingartum.bloodmagic.data.BloodData;
import dev.gamingartum.bloodmagic.menu.BloodMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The mod's only entry point for players.
 *
 * <p>Rooted at {@code /blood} rather than {@code /bloodmagic}, which the sibling item-import
 * mod already claims.
 */
public final class BloodCommand {

    private BloodCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("blood")
            .executes(ctx -> openMenu(ctx.getSource()))

            .then(Commands.literal("menu")
                .executes(ctx -> openMenu(ctx.getSource())))

            .then(Commands.literal("status")
                .executes(ctx -> status(ctx.getSource())))

            .then(Commands.literal("use")
                .then(Commands.argument("buff", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(BloodBuff.values()).map(BloodBuff::id), builder))
                    .executes(BloodCommand::use)))

            .then(Commands.literal("clear")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> clear(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> clear(ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "target"))))));
    }

    private static int openMenu(CommandSourceStack source) throws CommandSyntaxException {
        BloodMenu.open(source.getPlayerOrException());
        return 1;
    }

    private static int use(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "buff");

        Optional<BloodBuff> parsed = BloodBuff.byId(id);
        if (parsed.isEmpty()) {
            source.sendFailure(Component.literal("No such blood buff: " + id));
            return 0;
        }
        BloodBuff buff = parsed.get();

        BuffManager.Result result = BuffManager.activate(player, buff);
        switch (result) {
            case OK -> source.sendSuccess(() -> Component.literal(
                "You sacrifice " + buff.heartCost + " hearts for " + buff.displayName + ".")
                .withStyle(ChatFormatting.DARK_RED), false);
            case ALREADY_ACTIVE -> source.sendFailure(Component.literal(
                buff.displayName + " is already coursing through you."));
            case COOLING_DOWN -> source.sendFailure(Component.literal(
                "Your blood has not recovered from " + buff.displayName + " yet."));
            case NOT_ENOUGH_BLOOD -> source.sendFailure(Component.literal(
                "You do not have " + buff.heartCost + " hearts to give."));
            case BODY_TOO_WEAK -> source.sendFailure(Component.literal(
                "Your body cannot survive another sacrifice."));
        }
        return result == BuffManager.Result.OK ? 1 : 0;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long now = BuffManager.gameTime(player);
        List<ActiveBuffEntry> entries = BloodData.get(player);

        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Your blood is quiet.")
                .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Blood Magic")
            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);

        for (ActiveBuffEntry entry : entries) {
            boolean active = entry.isEffectActive(now);
            String line = "  " + entry.buff().displayName
                + (active ? " - active, " : " - regenerating, ")
                + clock(entry.remaining(now));
            source.sendSuccess(() -> Component.literal(line)
                .withStyle(active ? ChatFormatting.RED : ChatFormatting.BLUE), false);
        }

        String hearts = String.format("  %.1f / %.1f hearts",
            player.getHealth() / 2.0f, player.getMaxHealth() / 2.0f);
        source.sendSuccess(() -> Component.literal(hearts).withStyle(ChatFormatting.GRAY), false);
        return entries.size();
    }

    private static int clear(CommandSourceStack source, ServerPlayer target) {
        BuffManager.clear(target);
        source.sendSuccess(() -> Component.literal(
            "Cleared all blood buffs from " + target.getName().getString() + "."), true);
        return 1;
    }

    private static String clock(long ticks) {
        long seconds = ticks / 20L;
        return (seconds / 60L) + ":" + String.format("%02d", seconds % 60L);
    }
}
