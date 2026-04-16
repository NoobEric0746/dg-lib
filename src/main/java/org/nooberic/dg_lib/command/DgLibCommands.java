package org.nooberic.dg_lib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.nooberic.dg_lib.api.DgLibApi;
import org.nooberic.dg_lib.client.QrCodeScreen;
import org.nooberic.dg_lib.client.StrengthControlScreen;
import org.nooberic.dg_lib.service.DeviceStatus;

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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dg")
                .then(Commands.literal("connect")
                        .executes(ctx -> {
                            DgLibApi.get().connect();
                            autoOpenQrUiAfterConnect();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§2[DG Lib]§r Connecting to DG-LAB..."),
                                    false
                            );
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§7QR UI will open automatically. You can also use /dg qr."),
                                    false
                            );
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
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal(String.format(
                                                "§2[DG Lib]§r Paired! Target: %s",
                                                status.getTargetId()
                                        )),
                                        false
                                );
                            } else {
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("§c[DG Lib] Not paired. Scan /dg qr with your phone."),
                                        false
                                );
                            }
                            return paired ? 1 : 0;
                        }))
                .then(Commands.literal("qr")
                        .executes(ctx -> {
                        DeviceStatus status = DgLibApi.get().getStatus();
                        if (hasReadyQrUrl(status)) {
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("§2[DG Lib]§r Opening QR Code UI..."),
                                        false
                                );
                        openQrUiWithWsUrl(status.getWsUrl());
                                return 1;
                            } else {
                                ctx.getSource().sendSuccess(
                            () -> Component.literal("§7[DG Lib] Waiting for session assignment... (state=" + DgLibApi.get().getConnectionState() + ")"),
                                        false
                                );
                                return 0;
                            }
                        }))
                .then(Commands.literal("ui")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§2[DG Lib]§r Opening Strength Control UI..."),
                                    false
                            );
                            Minecraft minecraft = Minecraft.getInstance();
                            minecraft.execute(() -> {
                                try {
                                    minecraft.setScreen(new StrengthControlScreen());
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    if (minecraft.player != null) {
                                        minecraft.player.displayClientMessage(
                                                Component.literal("§c[DG Lib] Failed to open Strength Control UI: " + e.getClass().getSimpleName()),
                                                false
                                        );
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
        );
    }
}
