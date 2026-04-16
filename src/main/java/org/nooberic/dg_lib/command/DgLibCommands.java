package org.nooberic.dg_lib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.nooberic.dg_lib.api.DgLibApi;
import org.nooberic.dg_lib.client.QrCodeScreen;
import org.nooberic.dg_lib.client.StrengthControlScreen;
import org.nooberic.dg_lib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dg_lib.multiplayer.ServerCoyote;
import org.nooberic.dg_lib.service.DeviceStatus;

import java.util.concurrent.CompletableFuture;

public final class DgLibCommands {
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
                                    "§2[DG Lib]§r State: %s | Paired: %s | Client: %s | Target: %s | Strength A: %d/%d | Strength B: %d/%d",
                                    state, paired, status.getClientId(), status.getTargetId(),
                                    status.getChannelAStrength(), status.getChannelALimit(),
                                    status.getChannelBStrength(), status.getChannelBLimit()
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
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 2))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 200))
                                        .executes(ctx -> {
                                            int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            boolean ok = DgLibApi.get().setStrength(channel, value);
                                            String msg = ok
                                                    ? String.format("§2[DG Lib]§r Set Ch%d to %d sent.", channel, value)
                                                    : "§c[DG Lib] Not paired or unavailable.";
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                            return ok ? 1 : 0;
                                        }))))
                .then(Commands.literal("wave")
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 2))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> {
                                            int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                            boolean ok = DgLibApi.get().playBasicWave(channel, seconds);
                                            String msg = ok
                                                    ? String.format("§2[DG Lib]§r Basic wave sent to Ch%d for %ds.", channel, seconds)
                                                    : "§c[DG Lib] Not paired or unavailable.";
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                            return ok ? 1 : 0;
                                        }))))
        );

        dispatcher.register(Commands.literal("dg_server")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("channel", IntegerArgumentType.integer(1, 2))
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
                                                                    ? String.format("§2[DG Server]§r Set %s Ch%d to %d sent.", target.getGameProfile().getName(), channel, value)
                                                                    : String.format("§c[DG Server] %s operation rejected (not paired or unavailable).", target.getGameProfile().getName())
                                                    );
                                                    return 1;
                                                })))))
                .then(Commands.literal("wave")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("channel", IntegerArgumentType.integer(1, 2))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 10))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");

                                                    ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(target);
                                                    handleServerFuture(
                                                            ctx.getSource(),
                                                            coyote.playBasicWave(channel, seconds),
                                                            ok -> ok
                                                                    ? String.format("§2[DG Server]§r Basic wave sent to %s Ch%d for %ds.", target.getGameProfile().getName(), channel, seconds)
                                                                    : String.format("§c[DG Server] %s wave rejected (not paired or unavailable).", target.getGameProfile().getName())
                                                    );
                                                    return 1;
                                                })))))
        );
    }
}
