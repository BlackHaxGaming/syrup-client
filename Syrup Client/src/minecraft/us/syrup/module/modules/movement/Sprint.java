package us.syrup.module.modules.movement;


import org.lwjgl.input.Keyboard;
import us.syrup.event.events.UpdateEvent;
import us.syrup.module.AntiCheat;
import us.syrup.module.Category;
import us.syrup.module.Module;

public class Sprint extends Module {

    public Sprint() {
            super("Sprint", "Sprints for the User", Keyboard.KEY_NONE, Category.MOVEMENT, AntiCheat.VANILLA, AntiCheat.VANILLA);
    }


    //Check if MC Player is there and if the Current screen is not NULL
    //Also added Boolean which Checks the Movement Input so the Player does not Try Sprinting while Standing so it Does not Flag anticheat
    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.thePlayer == null || mc.currentScreen != null) return;

        boolean forward = mc.thePlayer.movementInput.moveForward > 0.0F;
        if (mc.thePlayer.onGround && forward) {
            mc.thePlayer.setSprinting(true);
        }
    }

}
