package org.nooberic.dglib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.client.screen.QrCodeScreen;
import org.nooberic.dglib.client.screen.StrengthControlScreen;
import org.nooberic.dglib.multiplayer.CoyoteStatusSnapshot;
import org.nooberic.dglib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dglib.multiplayer.ServerCoyote;
import org.nooberic.dglib.pulse.Pulse;
import org.nooberic.dglib.service.DeviceStatus;

import java.util.concurrent.CompletableFuture;

public final class DgLibCommands {
    private static final SuggestionProvider<CommandSourceStack> PULSE_ID_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(DgLibApi.get().getAllPulses().keySet(), builder);

    private DgLibCommands() {
    }

    private static boolean hasValidWsUrl(String wsUrl) {
        return wsUrl != null && !wsUrl.isEmpty() && (wsUrl.startsWith("ws://") || wsUrl.startsWith("wss://"));
    }

    private static boolean hasReadyQrUrl(DeviceStatus status) {
        return status != null
                && hasValidWsUrl(status.getWsUrl())
                && status.getClientId() != null
                && !status.getClientId().isEmpty();
    }

    private static void openQrUiWithWsUrl(String wsUrl) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            try {
                minecraft.setScreen(new QrCodeScreen(wsUrl));
            } catch (Throwable e) {
                e.printStackTrace();
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§c[DG Lib] Failed to open QR UI: " + e.getClass().getSimpleName()),
                            false
                    );
                }
            }
        });
    }

    private static void autoOpenQrUiAfterConnect() {
        Thread worker = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                DeviceStatus status = DgLibApi.get().getStatus();
                if (hasReadyQrUrl(status)) {
                    openQrUiWithWsUrl(status.getWsUrl());
                    return;
                }
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "dg-lib-qr-ui-auto-open");
        worker.setDaemon(true);
        worker.start();
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
        dispatcher.register(Commands.literal("dg")
                .then(Commands.literal("connect")
                        .executes(ctx -> {
                            DgLibApi.get().connect();
                            autoOpenQrUiAfterConnect();
                            ctx.getSource().sendSuccess(() -> Component.literal("§2[DG Lib]§r Connecting to DG-LAB..."), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§7QR UI will open automatically. You can also use /dg qr."), false);
                            return 1;
                        }))
                .then(Commands.literal("disconnect")
                        .executes(ctx -> {
                            DgLibApi.get().disconnect();
                            ctx.getSource().sendSuccess(() -> Component.literal("§2[DG Lib]§r Disconnected."), false);
                            return 1;
                        }))
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            DeviceStatus status = DgLibApi.get().getStatus();
                            String state = DgLibApi.get().getConnectionState().toString();
                            String paired = DgLibApi.get().isPaired() ? "§aYES" : "§cNO";
                            String statusLine = String.format(
                                    "§2[DG Lib]§r State: %s | Paired: %s | Client: %s | Target: %s | Strength A: %d/%d | Strength B: %d/%d | Pain A/B: %d/%d | Floor A/B: %d/%d",
                                    state, paired, status.getClientId(), status.getTargetId(),
                                    status.getChannelAStrength(), status.getChannelALimit(),
                                    status.getChannelBStrength(), status.getChannelBLimit(),
                                    status.getChannelAPainStrength(), status.getChannelBPainStrength(),
                                    status.getChannelASensationLowerLimit(), status.getChannelBSensationLowerLimit()
                            );
                            ctx.getSource().sendSuccess(() -> Component.literal(statusLine), false);
                            if (!status.getLastErrorCode().isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§cLast Error: " + status.getLastErrorCode()), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("pair")
                        .executes(ctx -> {
                            boolean paired = DgLibApi.get().isPaired();
                            if (paired) {
                                DeviceStatus status = DgLibApi.get().getStatus();
                                ctx.getSource().sendSuccess(() -> Component.literal(String.format("§2[DG Lib]§r Paired! Target: %s", status.getTargetId())), false);
                            } else {
                                ctx.getSource().sendSuccess(() -> Component.literal("§c[DG Lib] Not paired. Scan /dg qr with your phone."), false);
                            }
                            return paired ? 1 : 0;
                        }))
                .then(Commands.literal("qr")
                        .executes(ctx -> {
                            DeviceStatus status = DgLibApi.get().getStatus();
                            if (hasReadyQrUrl(status)) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§2[DG Lib]§r Opening QR Code UI..."), false);
                                openQrUiWithWsUrl(status.getWsUrl());
                                return 1;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§7[DG Lib] Waiting for session assignment... (state=" + DgLibApi.get().getConnectionState() + ")"), false);
                            return 0;
                        }))
                .then(Commands.literal("ui")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§2[DG Lib]§r Opening Strength Control UI..."), false);
                            Minecraft minecraft = Minecraft.getInstance();
                            minecraft.execute(() -> {
                                try {
                                    minecraft.setScreen(new StrengthControlScreen());
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    if (minecraft.player != null) {
                                        minecraft.player.displayClientMessage(Component.literal("§c[DG Lib] Failed to open Strength Control UI: " + e.getClass().getSimpleName()), false);
                                    }
                                }
                            });
                            return 1;
                        }))
                .then(Commands.literal("set")
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 200))
                                        .executes(ctx -> {
                                            int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            boolean ok = DgLibApi.get().setStrength(channel, value);
                                            String msg = ok
                                        ? String.format("§2[DG Lib]§r Set Ch%s to %d sent.", channel == 3 ? "1+2" : String.valueOf(channel), value)
                                                    : "§c[DG Lib] Not paired or unavailable.";
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                            return ok ? 1 : 0;
                                        }))))
                .then(Commands.literal("setsoft")
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            boolean ok = DgLibApi.get().setSoftStrength(channel, value);
                                            String msg = ok
                                                    ? String.format("§2[DG Lib]§r Set soft Ch%s to %d sent.", channel == 3 ? "1+2" : String.valueOf(channel), value)
                                                    : "§c[DG Lib] Not paired or unavailable.";
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                            return ok ? 1 : 0;
                                        }))))
                                    .then(Commands.literal("increase")
                                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                            .then(Commands.argument("delta", IntegerArgumentType.integer(1, 200))
                                                .executes(ctx -> {
                                                    int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                    int delta = IntegerArgumentType.getInteger(ctx, "delta");
                                                    boolean ok = DgLibApi.get().increaseStrength(channel, delta);
                                                    String msg = ok
                                                        ? String.format("§2[DG Lib]§r Increased Ch%s by %d.", channel == 3 ? "1+2" : String.valueOf(channel), delta)
                                                        : "§c[DG Lib] Not paired or unavailable.";
                                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                                    return ok ? 1 : 0;
                                                }))))
                                    .then(Commands.literal("decrease")
                                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                            .then(Commands.argument("delta", IntegerArgumentType.integer(1, 200))
                                                .executes(ctx -> {
                                                    int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                    int delta = IntegerArgumentType.getInteger(ctx, "delta");
                                                    boolean ok = DgLibApi.get().decreaseStrength(channel, delta);
                                                    String msg = ok
                                                        ? String.format("§2[DG Lib]§r Decreased Ch%s by %d.", channel == 3 ? "1+2" : String.valueOf(channel), delta)
                                                        : "§c[DG Lib] Not paired or unavailable.";
                                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                                    return ok ? 1 : 0;
                                                }))))
                                    .then(Commands.literal("clear")
                                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                            .executes(ctx -> {
                                                int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                boolean ok = DgLibApi.get().clear(channel);
                                                String msg = ok
                                                    ? String.format("§2[DG Lib]§r Cleared Ch%s wave and reset strength.", channel == 3 ? "1+2" : String.valueOf(channel))
                                                    : "§c[DG Lib] Not paired or unavailable.";
                                                ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                                return ok ? 1 : 0;
                                        })))
                .then(Commands.literal("control")
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                .then(Commands.argument("strength", IntegerArgumentType.integer(0, 200))
                                        .then(Commands.argument("pulse_id", StringArgumentType.word())
                                                .suggests(PULSE_ID_SUGGESTIONS)
                                                .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.1D, 60D))
                                                        .executes(ctx -> {
                                                            int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                            int strength = IntegerArgumentType.getInteger(ctx, "strength");
                                                            String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                            double seconds = DoubleArgumentType.getDouble(ctx, "seconds");
                                                            try {
                                                                boolean ok = DgLibApi.get().control(channel, strength, pulseId, seconds);
                                                                if (!ok && DgLibApi.get().getPulse(pulseId) == null) {
                                                                    ctx.getSource().sendFailure(Component.literal("§c[DG Lib] Unknown pulse id: " + pulseId));
                                                                    ctx.getSource().sendSuccess(
                                                                            () -> Component.literal("§7Registered: " + String.join(", ", DgLibApi.get().getAllPulses().keySet())),
                                                                            false
                                                                    );
                                                                    return 0;
                                                                }

                                                                String msg = ok
                    ? String.format("§2[DG Lib]§r Control sent. Ch%s strength=%d, pulse='%s', %.1fs.", channel == 3 ? "1+2" : String.valueOf(channel), strength, pulseId, seconds)
                                                                        : "§c[DG Lib] Not paired or unavailable.";
                                                                ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                                                return ok ? 1 : 0;
                                                            } catch (Throwable ex) {
                                                                ctx.getSource().sendFailure(Component.literal("§c[DG Lib] Control failed: " + ex.getClass().getSimpleName() + " - " + String.valueOf(ex.getMessage())));
                                                                return 0;
                                                            }
                                                        }))))))
                                        .then(Commands.literal("controlsoft")
                                            .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                                .then(Commands.argument("strength", IntegerArgumentType.integer(0, 100))
                                                    .then(Commands.argument("pulse_id", StringArgumentType.word())
                                                        .suggests(PULSE_ID_SUGGESTIONS)
                                                        .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.1D, 60D))
                                                            .executes(ctx -> {
                                                                int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                                int strength = IntegerArgumentType.getInteger(ctx, "strength");
                                                                String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                                double seconds = DoubleArgumentType.getDouble(ctx, "seconds");
                                                                try {
                                                                boolean ok = DgLibApi.get().controlSoft(channel, strength, pulseId, seconds);
                                                                if (!ok && DgLibApi.get().getPulse(pulseId) == null) {
                                                                    ctx.getSource().sendFailure(Component.literal("§c[DG Lib] Unknown pulse id: " + pulseId));
                                                                    ctx.getSource().sendSuccess(
                                                                        () -> Component.literal("§7Registered: " + String.join(", ", DgLibApi.get().getAllPulses().keySet())),
                                                                        false
                                                                    );
                                                                    return 0;
                                                                }

                                                                String msg = ok
                                                                    ? String.format("§2[DG Lib]§r Soft control sent. Ch%s strength=%d, pulse='%s', %.1fs.", channel == 3 ? "1+2" : String.valueOf(channel), strength, pulseId, seconds)
                                                                    : "§c[DG Lib] Not paired or unavailable.";
                                                                ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                                                return ok ? 1 : 0;
                                                                } catch (Throwable ex) {
                                                                ctx.getSource().sendFailure(Component.literal("§c[DG Lib] Soft control failed: " + ex.getClass().getSimpleName() + " - " + String.valueOf(ex.getMessage())));
                                                                return 0;
                                                                }
                                                            }))))))
                .then(Commands.literal("wave")
                        .then(Commands.argument("pulse_id", StringArgumentType.word())
                                .suggests(PULSE_ID_SUGGESTIONS)
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                .executes(ctx -> {
                                    String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    Pulse pulse = DgLibApi.get().getPulse(pulseId);
                                    if (pulse == null) {
                                        ctx.getSource().sendFailure(Component.literal("§c[DG Lib] Unknown pulse id: " + pulseId));
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("§7Registered: " + String.join(", ", DgLibApi.get().getAllPulses().keySet())),
                                                false
                                        );
                                        return 0;
                                    }

                                        Pulse runtimePulse = new Pulse(
                                            pulse.getName(),
                                            pulse.getChannel(),
                                            seconds,
                                            pulse.getFrames()
                                        );
                                        boolean ok = DgLibApi.get().playPulse(runtimePulse);
                                    String msg = ok
                                            ? String.format("§2[DG Lib]§r Pulse '%s' triggered for %ds.", pulseId, seconds)
                                            : "§c[DG Lib] Not paired or unavailable.";
                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                    return ok ? 1 : 0;
                                    }))))
        );

        dispatcher.register(Commands.literal("dg_server")
                .requires(source -> source.hasPermission(2))
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
                                target.getGameProfile().getName(), paired,
                                snapshot.getChannelAStrength(), snapshot.getChannelALimit(),
                                snapshot.getChannelBStrength(), snapshot.getChannelBLimit(),
                                snapshot.getChannelAPainStrength(), snapshot.getChannelBPainStrength(),
                                snapshot.getChannelASensationLowerLimit(), snapshot.getChannelBSensationLowerLimit()
                            );
                            }
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
                                                                    ? String.format("§2[DG Server]§r Set %s Ch%s to %d sent.", target.getGameProfile().getName(), channel == 3 ? "1+2" : String.valueOf(channel), value)
                                                                    : String.format("§c[DG Server] %s operation rejected (not paired or unavailable).", target.getGameProfile().getName())
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
                                                                    ? String.format("§2[DG Server]§r Set soft %s Ch%s to %d sent.", target.getGameProfile().getName(), channel == 3 ? "1+2" : String.valueOf(channel), value)
                                                                    : String.format("§c[DG Server] %s operation rejected (not paired or unavailable).", target.getGameProfile().getName())
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
                                                                ? String.format("§2[DG Server]§r Increased %s Ch%s by %d.", target.getGameProfile().getName(), channel == 3 ? "1+2" : String.valueOf(channel), delta)
                                                                : String.format("§c[DG Server] %s increase rejected (not paired or unavailable).", target.getGameProfile().getName())
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
                                                                ? String.format("§2[DG Server]§r Decreased %s Ch%s by %d.", target.getGameProfile().getName(), channel == 3 ? "1+2" : String.valueOf(channel), delta)
                                                                : String.format("§c[DG Server] %s decrease rejected (not paired or unavailable).", target.getGameProfile().getName())
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
                                                            ? String.format("§2[DG Server]§r Cleared %s Ch%s wave and strength.", target.getGameProfile().getName(), channel == 3 ? "1+2" : String.valueOf(channel))
                                                            : String.format("§c[DG Server] %s clear rejected (not paired or unavailable).", target.getGameProfile().getName())
                                                    );
                                                    return 1;
                                        }))))
                                    .then(Commands.literal("control")
                                        .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                                .then(Commands.argument("strength", IntegerArgumentType.integer(0, 200))
                                                    .then(Commands.argument("pulse_id", StringArgumentType.word())
                                                        .suggests(PULSE_ID_SUGGESTIONS)
                                                        .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.1D, 60D))
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                                int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                                int strength = IntegerArgumentType.getInteger(ctx, "strength");
                                                                String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                                double seconds = DoubleArgumentType.getDouble(ctx, "seconds");

                                                                ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                                handleServerFuture(
                                                                    ctx.getSource(),
                                                                    coyote.control(channel, strength, pulseId, seconds),
                                                                    ok -> ok
                                                                        ? String.format("§2[DG Server]§r Control sent to %s: Ch%s=%d, pulse='%s', %.1fs.", target.getGameProfile().getName(), channel == 3 ? "1+2" : String.valueOf(channel), strength, pulseId, seconds)
                                                                        : String.format("§c[DG Server] %s control rejected (not paired / id not found / unavailable).", target.getGameProfile().getName())
                                                                );
                                                                return 1;
                                                            })))))))
                                    .then(Commands.literal("controlsoft")
                                        .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                                .then(Commands.argument("strength", IntegerArgumentType.integer(0, 100))
                                                    .then(Commands.argument("pulse_id", StringArgumentType.word())
                                                        .suggests(PULSE_ID_SUGGESTIONS)
                                                        .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.1D, 60D))
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                                int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                                int strength = IntegerArgumentType.getInteger(ctx, "strength");
                                                                String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                                double seconds = DoubleArgumentType.getDouble(ctx, "seconds");

                                                                ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                                handleServerFuture(
                                                                    ctx.getSource(),
                                                                    coyote.controlSoft(channel, strength, pulseId, seconds),
                                                                    ok -> ok
                                                                        ? String.format("§2[DG Server]§r Soft control sent to %s: Ch%s=%d, pulse='%s', %.1fs.", target.getGameProfile().getName(), channel == 3 ? "1+2" : String.valueOf(channel), strength, pulseId, seconds)
                                                                        : String.format("§c[DG Server] %s soft control rejected (not paired / id not found / unavailable).", target.getGameProfile().getName())
                                                                );
                                                                return 1;
                                                            })))))))
                .then(Commands.literal("wave")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("pulse_id", StringArgumentType.word())
                            .suggests(PULSE_ID_SUGGESTIONS)
                            .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                int seconds = IntegerArgumentType.getInteger(ctx, "seconds");

                                            ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                            handleServerFuture(
                                                    ctx.getSource(),
                                    coyote.playPulseById(pulseId, seconds),
                                                    ok -> ok
                                        ? String.format("§2[DG Server]§r Pulse '%s' sent to %s for %ds.", pulseId, target.getGameProfile().getName(), seconds)
                                                            : String.format("§c[DG Server] %s pulse '%s' rejected (not paired / id not found / unavailable).", target.getGameProfile().getName(), pulseId)
                                            );
                                            return 1;
                            })))))
        );
    }
}
