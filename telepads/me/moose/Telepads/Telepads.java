package me.moose.Telepads;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;
import org.bukkit.plugin.PluginManager;
import org.bukkit.Material;



public class Telepads extends JavaPlugin {
	
    public int MAX_DISTANCE = 0;
    public boolean USE_PERMISSIONS = false;
    public boolean OP_ONLY = false;
    public boolean DISABLE_TELEPORT_WAIT = false;
    public boolean DISABLE_TELEPORT_MESSAGE = false;
    public Material TELEPAD_CENTER_ID = Material.getMaterial("LAPIS_BLOCK");
    public Material TELEPAD_SURROUNDING_ID = Material.getMaterial("OBSIDIAN");
	
	Logger log;
    
	public void onEnable() {
        getConfig().options().copyDefaults(true);

        MAX_DISTANCE = getConfig().getInt("max_telepad_distance");
        USE_PERMISSIONS = getConfig().getBoolean("use_permissions");
        OP_ONLY = getConfig().getBoolean("op_only",OP_ONLY);
        DISABLE_TELEPORT_WAIT = getConfig().getBoolean("disable_teleport_wait");
        DISABLE_TELEPORT_MESSAGE = getConfig().getBoolean("disable_teleport_message");
        TELEPAD_CENTER_ID = Material.getMaterial( getConfig().getString("telepad_center"));
        TELEPAD_SURROUNDING_ID = Material.getMaterial( getConfig().getString("telepad_surrounding"));
    	
    	
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new TelepadsListener(this), this);
       
        this.saveDefaultConfig();
    	log = this.getLogger();
    	log.info("Telepads Enabled!");
    	
	}
	public void onDisable(){ 
		log.info("Telepads Disabled!");
	}
}
