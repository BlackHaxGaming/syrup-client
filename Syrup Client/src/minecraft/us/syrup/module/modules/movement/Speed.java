package us.syrup.module.modules.movement;

import org.lwjgl.input.Keyboard;
import us.syrup.Syrup;
import us.syrup.event.events.UpdateEvent;
import us.syrup.module.AntiCheat;
import us.syrup.module.Category;
import us.syrup.module.Module;
import us.syrup.module.ModuleManager;

public class Speed extends Module {
    public Speed() {
        super("Speed", "Out run the Players", Keyboard.KEY_NONE, Category.MOVEMENT, AntiCheat.MINEBERRY);
    }

    @Override
    public void setup() {
        moduleSettings.addDefault("Mineberry-Speed", 1.0F, 0.5F, 4.0F, 0.1F);

        super.setup();
    }

    @Override
    public void onEnable() {
        if (antiCheat == AntiCheat.MINEBERRY) {


            Module sprint = Syrup.instance.getModuleManager().getModule("Sprint");
            if (sprint != null && sprint.isToggled()) {
                sprint.setToggled(false);
            }

            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }

            mc.thePlayer.setSprinting(true);
        }

        super.onEnable();
    }


    @Override
    public void onUpdate(UpdateEvent event) {
        if(antiCheat == AntiCheat.MINEBERRY) {

            mc.timer.timerSpeed = moduleSettings.getFloat("Mineberry-Speed");

        }

        super.onUpdate(event);
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0F;
        super.onDisable();
    }
}
