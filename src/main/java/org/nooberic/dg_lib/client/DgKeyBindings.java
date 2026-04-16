package org.nooberic.dg_lib.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class DgKeyBindings {
    public static final String CATEGORY = "key.categories.dg_lib";

    public static final KeyMapping OPEN_DG_UI = new KeyMapping(
            "key.dg_lib.open_ui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY
    );

        public static final KeyMapping EMERGENCY_STOP = new KeyMapping(
            "key.dg_lib.emergency_stop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            CATEGORY
        );

    private DgKeyBindings() {
    }
}
