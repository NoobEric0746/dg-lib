package org.nooberic.dglib.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.nooberic.dglib.dglib;

public final class DgKeyBindings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(dglib.MODID, "ui"));

    public static final KeyMapping OPEN_DG_UI = new KeyMapping(
            "key.dglib.open_ui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY
    );

    public static final KeyMapping EMERGENCY_STOP = new KeyMapping(
            "key.dglib.emergency_stop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            CATEGORY
    );

    private DgKeyBindings() {
    }
}