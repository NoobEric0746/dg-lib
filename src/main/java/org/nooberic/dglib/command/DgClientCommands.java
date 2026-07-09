package org.nooberic.dglib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.client.screen.QrCodeScreen;
import org.nooberic.dglib.client.screen.StrengthControlScreen;
import org.nooberic.dglib.pulse.Pulse;
import org.nooberic.dglib.service.DeviceStatus;

public final class DgClientCommands {
    private DgClientCommands() {
    }

    private static void sendFeedback(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static void sendError(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal(text));
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
                            sendFeedback(ctx.getSource(), "§2[DG Lib]§r Connecting to DG-LAB...");
                            sendFeedback(ctx.getSource(), "§7QR UI will open automatically. You can also use /dg qr.");
                            return 1;
                        }))
                .then(Commands.literal("disconnect")
                        .executes(ctx -> {
                            DgLibApi.get().disconnect();
                            sendFeedback(ctx.getSource(), "§2[DG Lib]§r Disconnected.");
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
                            sendFeedback(ctx.getSource(), statusLine);
                            if (!status.getLastErrorCode().isEmpty()) {
                                sendFeedback(ctx.getSource(), "§cLast Error: " + status.getLastErrorCode());
                            }
                            return 1;
                        }))
                .then(Commands.literal("pair")
                        .executes(ctx -> {
                            boolean paired = DgLibApi.get().isPaired();
                            if (paired) {
                                DeviceStatus status = DgLibApi.get().getStatus();
                                sendFeedback(ctx.getSource(), String.format("§2[DG Lib]§r Paired! Target: %s", status.getTargetId()));
                            } else {
                                sendFeedback(ctx.getSource(), "§c[DG Lib] Not paired. Scan /dg qr with your phone.");
                            }
                            return paired ? 1 : 0;
                        }))
                .then(Commands.literal("qr")
                        .executes(ctx -> {
                            DeviceStatus status = DgLibApi.get().getStatus();
                            if (hasReadyQrUrl(status)) {
                                sendFeedback(ctx.getSource(), "§2[DG Lib]§r Opening QR Code UI...");
                                openQrUiWithWsUrl(status.getWsUrl());
                                return 1;
                            }
                            sendFeedback(ctx.getSource(), "§7[DG Lib] Waiting for session assignment... (state=" + DgLibApi.get().getConnectionState() + ")");
                            return 0;
                        }))
                .then(Commands.literal("ui")
                        .executes(ctx -> {
                            sendFeedback(ctx.getSource(), "§2[DG Lib]§r Opening Strength Control UI...");
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
                                                sendFeedback(ctx.getSource(), msg);
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
                                                sendFeedback(ctx.getSource(), msg);
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
                                                sendFeedback(ctx.getSource(), msg);
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
                                                sendFeedback(ctx.getSource(), msg);
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
                                        sendFeedback(ctx.getSource(), msg);
                                    return ok ? 1 : 0;
                                })))
                .then(Commands.literal("control")
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                .then(Commands.argument("strength", IntegerArgumentType.integer(0, 200))
                                        .then(Commands.argument("pulse_id", StringArgumentType.word())
                                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(DgLibApi.get().getAllPulses().keySet(), builder))
                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                                        .executes(ctx -> {
                                                            int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                            int strength = IntegerArgumentType.getInteger(ctx, "strength");
                                                            String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                                            try {
                                                                boolean ok = DgLibApi.get().control(channel, strength, pulseId, seconds);
                                                                if (!ok && DgLibApi.get().getPulse(pulseId) == null) {
                                                                    sendError(ctx.getSource(), "§c[DG Lib] Unknown pulse id: " + pulseId);
                                                                    sendFeedback(ctx.getSource(), "§7Registered: " + String.join(", ", DgLibApi.get().getAllPulses().keySet()));
                                                                    return 0;
                                                                }
                                                                String msg = ok
                                                                        ? String.format("§2[DG Lib]§r Control sent. Ch%s strength=%d, pulse='%s', %ds.", channel == 3 ? "1+2" : String.valueOf(channel), strength, pulseId, seconds)
                                                                        : "§c[DG Lib] Not paired or unavailable.";
                                                                sendFeedback(ctx.getSource(), msg);
                                                                return ok ? 1 : 0;
                                                            } catch (Throwable ex) {
                                                                sendError(ctx.getSource(), "§c[DG Lib] Control failed: " + ex.getClass().getSimpleName() + " - " + String.valueOf(ex.getMessage()));
                                                                return 0;
                                                            }
                                                        }))))))
                .then(Commands.literal("controlsoft")
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                .then(Commands.argument("strength", IntegerArgumentType.integer(0, 100))
                                        .then(Commands.argument("pulse_id", StringArgumentType.word())
                                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(DgLibApi.get().getAllPulses().keySet(), builder))
                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                                        .executes(ctx -> {
                                                            int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                            int strength = IntegerArgumentType.getInteger(ctx, "strength");
                                                            String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                                            try {
                                                                boolean ok = DgLibApi.get().controlSoft(channel, strength, pulseId, seconds);
                                                                if (!ok && DgLibApi.get().getPulse(pulseId) == null) {
                                                                    sendError(ctx.getSource(), "§c[DG Lib] Unknown pulse id: " + pulseId);
                                                                    sendFeedback(ctx.getSource(), "§7Registered: " + String.join(", ", DgLibApi.get().getAllPulses().keySet()));
                                                                    return 0;
                                                                }
                                                                String msg = ok
                                                                        ? String.format("§2[DG Lib]§r Soft control sent. Ch%s strength=%d, pulse='%s', %ds.", channel == 3 ? "1+2" : String.valueOf(channel), strength, pulseId, seconds)
                                                                        : "§c[DG Lib] Not paired or unavailable.";
                                                                sendFeedback(ctx.getSource(), msg);
                                                                return ok ? 1 : 0;
                                                            } catch (Throwable ex) {
                                                                sendError(ctx.getSource(), "§c[DG Lib] Soft control failed: " + ex.getClass().getSimpleName() + " - " + String.valueOf(ex.getMessage()));
                                                                return 0;
                                                            }
                                                        }))))))
                .then(Commands.literal("wave")
                        .then(Commands.argument("channel", IntegerArgumentType.integer(1, 3))
                                .then(Commands.argument("pulse_id", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DgLibApi.get().getAllPulses().keySet(), builder))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                                .executes(ctx -> {
                                                    int channel = IntegerArgumentType.getInteger(ctx, "channel");
                                                    String pulseId = StringArgumentType.getString(ctx, "pulse_id");
                                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                                    Pulse pulse = DgLibApi.get().getPulse(pulseId);
                                                    if (pulse == null) {
                                                        sendError(ctx.getSource(), "§c[DG Lib] Unknown pulse id: " + pulseId);
                                                        sendFeedback(ctx.getSource(), "§7Registered: " + String.join(", ", DgLibApi.get().getAllPulses().keySet()));
                                                        return 0;
                                                    }
                                                    boolean ok = DgLibApi.get().playPulse(channel, pulse, seconds);
                                                    String msg = ok
                                                            ? String.format("§2[DG Lib]§r Pulse '%s' triggered on Ch%s for %ds.", pulseId, channel == 3 ? "1+2" : String.valueOf(channel), seconds)
                                                            : "§c[DG Lib] Not paired or unavailable.";
                                                    sendFeedback(ctx.getSource(), msg);
                                                    return ok ? 1 : 0;
                                                })))))
        );
    }
}