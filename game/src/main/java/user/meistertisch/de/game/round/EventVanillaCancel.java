package user.meistertisch.de.game.round;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class EventVanillaCancel implements Listener {
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event){
        event.setCancelled(true);
    }
}
