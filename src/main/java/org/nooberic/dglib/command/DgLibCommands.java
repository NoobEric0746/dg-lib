package org.nooberic.dglib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.multiplayer.CoyoteStatusSnapshot;
import org.nooberic.dglib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dglib.multiplayer.ServerCoyote;
import org.nooberic.dglib.util.ElectricityParticleUtil;

import java.util.concurrent.CompletableFuture;

public final class DgLibCommands {
    private static final SuggestionProvider<CommandSourceStack> PULSE_ID_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(DgLibApi.get().getAllPulses().keySet(), builder);

    private DgLibCommands() {
    }

    private static <T> void handleServerFuture(CommandSourceStack source, CompletableFuture<T> future, java.util.function.Function<T, String> successText) {
        future.whenComplete((result, throwable) -> {
            source.getServer().execute(() -> {
                if (throwable != null) {
                    String err = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
                    source.sendFailure(Component.literal("§c[DG Server] Operation failed: " + err));
                    return;
                }
                source.sendSuccess(() -> Component.literal(successText.apply(result)), false);
            });
        });
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dg_server")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("status")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                        handleServerFuture(
                            ctx.getSource(),
                            coyote.refreshStatus(),
                            snapshotObj -> {
                            CoyoteStatusSnapshot snapshot = (CoyoteStatusSnapshot) snapshotObj;
                            String paired = snapshot.isPaired() ? "§aYES" : "§cNO";
                            return String.format(
                                "§2[DG Server]§r %s | Paired: %s | Strength A: %d/%d | Strength B: %d/%d | Pain A/B: %d/%d | Floor A/B: %d/%d",
                                target.getGameProfile().name(), paired,
                                snapshot.getChannelAStrength(), snapshot.getChannelALimit(),
                                snapshot.getChannelBStrength(), snapshot.getChannelBLimit(),
                                snapshot.getChannelAPainStrength(), snapshot.getChannelBPainStrength(),
                                snapshot.getChannelASensationLowerLimit(), snapshot.getChannelBSensationLowerLimit()
                            );
                            }
                        );
                        return 1;
                    })))
                .then(Commands.literal("particle")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ElectricityParticleUtil.spawnAroundPlayer(target, 32);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal(String.format("§2[DG Server]§r Broadcast electricity particles on %s.", target.getGameProfile().name())),
                                    false
                            );
                            return 1;
                        })))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 200))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");

                                                    ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                    handleServerFuture(
                                                            ctx.getSource(),
                                                            coyote.setStrength(channel, value),
                                                            ok -> ok
                                                                    ? String.format("§2[DG Server]§r Set %s Ch%s to %d sent.", target.getGameProfile().name(), channel == 3 ? "1+2" : String.valueOf(channel), value)
                                                                    : String.format("§c[DG Server] %s operation rejected (not paired or unavailable).", target.getGameProfile().name())
                                                    );
                                                    return 1;
                                                })))))
                .then(Commands.literal("setsoft")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");

                                                    ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                    handleServerFuture(
                                                            ctx.getSource(),
                                                            coyote.setSoftStrength(channel, value),
                                                            ok -> ok
                                                                    ? String.format("§2[DG Server]§r Set soft %s Ch%s to %d sent.", target.getGameProfile().name(), channel == 3 ? "1+2" : String.valueOf(channel), value)
                                                                    : String.format("§c[DG Server] %s operation rejected (not paired or unavailable).", target.getGameProfile().name())
                                                    );
                                                    return 1;
                                                })))))
                                    .then(Commands.literal("increase")
                                        .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                                .then(Commands.argument("delta", IntegerArgumentType.integer(1, 200))
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                        int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                        int delta = IntegerArgumentType.getInteger(ctx, "delta");

                                                        ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                        handleServerFuture(
                                                            ctx.getSource(),
                                                            coyote.increaseStrength(channel, delta),
                                                            ok -> ok
                                                                ? String.format("§2[DG Server]§r Increased %s Ch%s by %d.", target.getGameProfile().name(), channel == 3 ? "1+2" : String.valueOf(channel), delta)
                                                                : String.format("§c[DG Server] %s increase rejected (not paired or unavailable).", target.getGameProfile().name())
                                                        );
                                                        return 1;
                                                    })))))
                                    .then(Commands.literal("decrease")
                                        .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                                .then(Commands.argument("delta", IntegerArgumentType.integer(1, 200))
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                        int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                        int delta = IntegerArgumentType.getInteger(ctx, "delta");

                                                        ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                        handleServerFuture(
                                                            ctx.getSource(),
                                                            coyote.decreaseStrength(channel, delta),
                                                            ok -> ok
                                                                ? String.format("§2[DG Server]§r Decreased %s Ch%s by %d.", target.getGameProfile().name(), channel == 3 ? "1+2" : String.valueOf(channel), delta)
                                                                : String.format("§c[DG Server] %s decrease rejected (not paired or unavailable).", target.getGameProfile().name())
                                                        );
                                                        return 1;
                                                    })))))
                                    .then(Commands.literal("clear")
                                        .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int channel = IntegerArgumentType.getInteger(ctx, "channel");

                                                    ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                    handleServerFuture(
                                                        ctx.getSource(),
                                                        coyote.clear(channel),
                                                        ok -> ok
                                                            ? String.format("§2[DG Server]§r Cleared %s Ch%s wave and strength.", target.getGameProfile().name(), channel == 3 ? "1+2" : String.valueOf(channel))
                                                            : String.format("§c[DG Server] %s clear rejected (not paired or unavailable).", target.getGameProfile().name())
                                                    );
                                                    return 1;
                                        }))))
                                    .then(Commands.literal("control")
                                        .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                                .then(Commands.argument("strength", IntegerArgumentType.integer(0, 200))
                                                    .then(Commands.argument("pulse_id", StringArgumentType.word())
                                                        .suggests(PULSE_ID_SUGGESTIONS)
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                                int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                                int strength = IntegerArgumentType.getInteger(ctx, "strength");
                                                                String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                                int seconds = IntegerArgumentType.getInteger(ctx, "seconds");

                                                                ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                                handleServerFuture(
                                                                    ctx.getSource(),
                                                                    coyote.control(channel, strength, pulseId, seconds),
                                                                    ok -> ok
                                                                        ? String.format("§2[DG Server]§r Control sent to %s: Ch%s=%d, pulse='%s', %ds.", target.getGameProfile().name(), channel == 3 ? "1+2" : String.valueOf(channel), strength, pulseId, seconds)
                                                                        : String.format("§c[DG Server] %s control rejected (not paired / id not found / unavailable).", target.getGameProfile().name())
                                                                );
                                                                return 1;
                                                            })))))))
                                    .then(Commands.literal("controlsoft")
                                        .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                                .then(Commands.argument("strength", IntegerArgumentType.integer(0, 100))
                                                    .then(Commands.argument("pulse_id", StringArgumentType.word())
                                                        .suggests(PULSE_ID_SUGGESTIONS)
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                                int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                                int strength = IntegerArgumentType.getInteger(ctx, "strength");
                                                                String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                                int seconds = IntegerArgumentType.getInteger(ctx, "seconds");

                                                                ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                                handleServerFuture(
                                                                    ctx.getSource(),
                                                                    coyote.controlSoft(channel, strength, pulseId, seconds),
                                                                    ok -> ok
                                                                        ? String.format("§2[DG Server]§r Soft control sent to %s: Ch%s=%d, pulse='%s', %ds.", target.getGameProfile().name(), channel == 3 ? "1+2" : String.valueOf(channel), strength, pulseId, seconds)
                                                                        : String.format("§c[DG Server] %s soft control rejected (not paired / id not found / unavailable).", target.getGameProfile().name())
                                                                );
                                                                return 1;
                                                            })))))))
                .then(Commands.literal("wave")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                        .then(Commands.argument("pulse_id", StringArgumentType.word())
                                    .suggests(PULSE_ID_SUGGESTIONS)
                                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                    String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");

                                                    ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                    handleServerFuture(
                                                            ctx.getSource(),
                                            coyote.playPulseById(channel, pulseId, seconds),
                                                            ok -> ok
                                                ? String.format("§2[DG Server]§r Pulse '%s' sent to %s on Ch%s for %ds.", pulseId, target.getGameProfile().name(), channel == 3 ? "1+2" : String.valueOf(channel), seconds)
                                                                    : String.format("§c[DG Server] %s pulse '%s' rejected (not paired / id not found / unavailable).", target.getGameProfile().name(), pulseId)
                                                    );
                                                    return 1;
                                    }))))))
        );
    }
}
