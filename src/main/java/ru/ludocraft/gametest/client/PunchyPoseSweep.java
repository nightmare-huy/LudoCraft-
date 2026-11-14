package ru.ludocraft.gametest.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Development-only resource-pack sweep; tests Punchy's actual JSON loader, not an emulated renderer. */
public final class PunchyPoseSweep {
    private static final int[] ANGLES = {-90, -45, 0, 45, 90};
    private static int phase, ticks;
    private static boolean loading, ready, capture;
    private PunchyPoseSweep() {}
    public static boolean enabled() { return Boolean.getBoolean("ludocraft.punchySweep"); }
    public static void tick(Minecraft mc) {
        if (phase >= ANGLES.length) return;
        mc.player.setYRot(0); mc.player.yRotO = 0;
        mc.player.setXRot(0); mc.player.xRotO = 0;
        mc.player.yHeadRot = 0; mc.player.yHeadRotO = 0;
        if (!loading) {
            loading = true;
            var repository = mc.getResourcePackRepository();
            repository.reload();
            var ids = new ArrayList<>(repository.getSelectedIds());
            ids.removeIf(id -> id.startsWith("file/ludka-punchy-candidate-"));
            String selected = "file/ludka-punchy-candidate-" + ANGLES[phase];
            if (repository.getPack(selected) == null) throw new AssertionError("Missing candidate pack " + selected);
            ids.add(selected);
            repository.setSelected(ids);
            mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> ready = true));
        }
        if (ready && ++ticks == 60) capture = true;
    }
    public static void frame(Minecraft mc) throws Exception {
        if (!capture || mc.getOverlay() != null) return;
        capture = false;
        Path path = Path.of("screenshots", "punchy-candidate-z" + ANGLES[phase] + ".png");
        Files.createDirectories(path.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
        phase++; ticks = 0; loading = false; ready = false;
        if (phase == ANGLES.length) {
            System.out.println("PUNCHY_SWEEP_PASS: actual resource-pack pose candidates captured");
            mc.stop();
        }
    }
}
