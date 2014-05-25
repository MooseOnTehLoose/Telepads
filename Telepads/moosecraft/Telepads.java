package moosecraft;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Telepads extends JavaPlugin implements Listener {
	
    public int MAX_DISTANCE = 0;
    public boolean USE_PERMISSIONS = false;
    public boolean OP_ONLY = false;
    public boolean DISABLE_TELEPORT_WAIT = false;
    public int SEND_WAIT_TIMER = 3;
    public boolean DISABLE_TELEPORT_MESSAGE = false;
    public Material TELEPAD_CENTER_ID = Material.getMaterial("LAPIS_BLOCK");
    public Material TELEPAD_SURROUNDING_ID = Material.getMaterial("OBSIDIAN");
    
    private static Map<String, Location> mLinks  = new HashMap<String, Location>();
    private static Map<String, Long> mTimeouts = new HashMap<String, Long>();
    

	public void onEnable() {
        getConfig().options().copyDefaults(true);

        MAX_DISTANCE = getConfig().getInt("max_telepad_distance");
        USE_PERMISSIONS = getConfig().getBoolean("use_permissions");
        OP_ONLY = getConfig().getBoolean("op_only",OP_ONLY);
        DISABLE_TELEPORT_WAIT = getConfig().getBoolean("disable_teleport_wait");
        SEND_WAIT_TIMER = getConfig().getInt("send_wait_timer");
        DISABLE_TELEPORT_MESSAGE = getConfig().getBoolean("disable_teleport_message");
        TELEPAD_CENTER_ID = Material.getMaterial( getConfig().getString("telepad_center"));
        TELEPAD_SURROUNDING_ID = Material.getMaterial( getConfig().getString("telepad_surrounding"));


        
        
        
		getLogger().info("Telepads has been enabled");
		getServer().getPluginManager().registerEvents(this, this);
		this.saveDefaultConfig();
    	
	}
	public void onDisable(){ 
		getLogger().info("Telepads has been Disabled!");
	}
	
    
    @EventHandler // EventPriority.NORMAL by default
    public void onInteract(PlayerInteractEvent event) throws InterruptedException{
        //Using a telepad, note we verify the timeout here after checking if it's a telepad
        if(event.getAction() == Action.PHYSICAL
        && event.getClickedBlock() != null
        && isTelePadLapis(event.getClickedBlock().getRelative(BlockFace.DOWN))
        && (!mTimeouts.containsKey(event.getPlayer().getName()) || mTimeouts.get(event.getPlayer().getName()) < System.currentTimeMillis())){
            Block bSenderLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);
            Block bReceiverLapis = getTelepadLapisReceiver(bSenderLapis);

            //Verify receiver is a working telepad
            if(bReceiverLapis != null){


                //Verify distance
                if(!TelePadsWithinDistance(bSenderLapis,bReceiverLapis)){
                    msgPlayer(event.getPlayer(),ChatColor.RED+"Error: Telepads are too far apart! (Distance:"+getDistance(bSenderLapis.getLocation(),bReceiverLapis.getLocation())+",MaxAllowed:"+MAX_DISTANCE+")");

                    return;
                }

                Sign sbReceiverSign = (Sign) bReceiverLapis.getRelative(BlockFace.DOWN).getState();
                
                if(!DISABLE_TELEPORT_MESSAGE){
                    String sMessage;

                    if(!DISABLE_TELEPORT_WAIT){
                        if(sbReceiverSign.getLine(3).equals("")){
                            sMessage = "Preparing to send you, stand still!";
                        }else{
                            sMessage = "Preparing to send you to "
                                +ChatColor.YELLOW+sbReceiverSign.getLine(3)
                                +ChatColor.AQUA+", stand still!";
                        }
                    }else{
                        if(sbReceiverSign.getLine(3).equals("")){
                            sMessage = "You have been teleported!";
                        }else{
                            sMessage = "You have been teleported to "
                                +ChatColor.YELLOW+sbReceiverSign.getLine(3);
                        }
                    }


                    msgPlayer(event.getPlayer(),sMessage);
                }
                
                if(DISABLE_TELEPORT_WAIT){
                     Teleport(event.getPlayer(),event.getPlayer().getLocation(),bSenderLapis,bReceiverLapis,DISABLE_TELEPORT_WAIT);
                }else{
                     Teleport(event.getPlayer(),event.getPlayer().getLocation(),bSenderLapis,bReceiverLapis,DISABLE_TELEPORT_WAIT);
               }


            }
        }
        //Creating a telepad link
        else if(event.getItem() != null
        && event.getItem().getType() == Material.REDSTONE
        && event.getClickedBlock() != null
        && isTelePadLapis(event.getClickedBlock().getRelative(BlockFace.DOWN))){


            if(getTelepadLapisReceiver(event.getClickedBlock().getRelative(BlockFace.DOWN)) != null){
                msgPlayer(event.getPlayer(),"Error: This telepad seems to be linked already!");
                msgPlayer(event.getPlayer(),ChatColor.YELLOW+"You can reset it by breaking the pressure pad on top of it, then clicking the lapis with redstone.");

                return;
            }

            //Determine the action
            if(!mLinks.containsKey(event.getPlayer().getName())){
                //Initial telepad click
                mLinks.put(event.getPlayer().getName(),event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation());

                msgPlayer(event.getPlayer(),"Telepad location stored!");

                return;
            }else{
                //They have a stored location, and right clicked  a telepad lapis, so remove the temp location
                if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
                    mLinks.remove(event.getPlayer().getName());

                    msgPlayer(event.getPlayer(),"Telepad location ditched! (right clicked)");

                    return;
                }else{
                    //Setting up the second link
                    Block bFirstLapis = mLinks.get(event.getPlayer().getName()).getBlock();

                    if(isTelePadLapis(bFirstLapis)){
                        Block bSecondLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);

                        if(!TelePadsWithinDistance(bFirstLapis,bSecondLapis)){
                            msgPlayer(event.getPlayer(),ChatColor.RED+"Error: Telepads are too far apart! (Distance:"+getDistance(bFirstLapis.getLocation(),event.getClickedBlock().getLocation())+",MaxAllowed:"+MAX_DISTANCE+")");

                            return;
                        }

                        //The same telepad?
                        if(bFirstLapis == bSecondLapis){
                            msgPlayer(event.getPlayer(),ChatColor.RED+"Error: You cannot connect a telepad to itself.");

                            msgPlayer(event.getPlayer(),"Well you could, but why would you want to? Maybe   you want a door or something?");

                            mLinks.remove(event.getPlayer().getName());

                            return;
                        }

                        mLinks.remove(event.getPlayer().getName());

                        linkTelepadLapisReceivers(bFirstLapis,event.getClickedBlock().getRelative(BlockFace.DOWN));

                        msgPlayer(event.getPlayer(),"Telepad location transferred!");

                        return;
                    }
                }
            }
        }
        
        //Resetting telepad
        else if(event.getItem() != null
        && event.getItem().getType() == Material.REDSTONE
        && event.getClickedBlock() != null
        && event.getClickedBlock().getType() == Material.LAPIS_BLOCK){
            Block bResetLapis = event.getClickedBlock();
            if(bResetLapis.getType() == Material.AIR
            && (TELEPAD_SURROUNDING_ID == Material.AIR
                || (bResetLapis.getRelative(BlockFace.EAST).getType() == TELEPAD_SURROUNDING_ID
                    && bResetLapis.getRelative(BlockFace.WEST).getType() == TELEPAD_SURROUNDING_ID
                    && bResetLapis.getRelative(BlockFace.NORTH).getType() == TELEPAD_SURROUNDING_ID
                    && bResetLapis.getRelative(BlockFace.SOUTH).getType() == TELEPAD_SURROUNDING_ID))
            && (bResetLapis.getRelative(BlockFace.DOWN).getType() == Material.SIGN_POST
                || bResetLapis.getRelative(BlockFace.DOWN).getType() == Material.WALL_SIGN)
            && bResetLapis.getRelative(BlockFace.UP).getType() == Material.STONE_PLATE){//*phew*
                //We checked that it's a sign above
                Sign sbResetSign = (Sign) bResetLapis.getRelative(BlockFace.DOWN).getState();

                sbResetSign.setLine(1,"");
                sbResetSign.setLine(2,"");
                sbResetSign.update();

                msgPlayer(event.getPlayer(),"Telepad Reset!");

                return;
            }
        }
    }

    
    private String toHex(int number){
        return Integer.toHexString(number + 32000);
    }

    private int toInt(String hex){
        return Integer.parseInt(hex, 16) - 32000;
    }
    
    //returns true if telepad structure is correct
    public boolean isTelePadLapis(Block center){
    	Boolean in = ( center.getType() == TELEPAD_CENTER_ID );
    	Boolean up = ( center.getRelative(BlockFace.NORTH).getType() == TELEPAD_SURROUNDING_ID );
    	Boolean dn = ( center.getRelative(BlockFace.SOUTH).getType() == TELEPAD_SURROUNDING_ID );
    	Boolean ri = ( center.getRelative(BlockFace.EAST).getType() == TELEPAD_SURROUNDING_ID );
    	Boolean le = ( center.getRelative(BlockFace.WEST).getType() == TELEPAD_SURROUNDING_ID );
    	Boolean sn = ( 
    			(center.getRelative(BlockFace.DOWN).getType() == Material.SIGN_POST) |
    			(center.getRelative(BlockFace.DOWN).getType() == Material.WALL_SIGN	));

    	if ( in && up && dn && ri && le && sn ){  
    		return true; 
    	}
    	
    	return false;
    			
    }
    
    private Block getTelepadLapisReceiver(Block bSenderLapis){
        Block bSenderSign = bSenderLapis.getRelative(BlockFace.DOWN);

        if(bSenderSign.getType() == Material.WALL_SIGN || bSenderSign.getType() == Material.SIGN_POST){
            Sign sbSenderSign = (Sign) bSenderSign.getState();

            String sHexLocation = sbSenderSign.getLine(2);

            String sWorld = sbSenderSign.getLine(1);
            String[] sXYZ = sHexLocation.split(":");

            World world = getServer().getWorld(sWorld);

            if(world == null){
                return null;
            }
            
            Block bReceiverLapis = world.getBlockAt(toInt(sXYZ[0]),toInt(sXYZ[1]),toInt(sXYZ[2]));

            if(isTelePadLapis(bReceiverLapis)){
                return bReceiverLapis;
            }
        }
        return null;
    }
    
    //currently assumes you checked both blocks with isTelePadLapis
    private void linkTelepadLapisReceivers(Block bLapis1,Block bLapis2){
        Sign sbLapis1 = (Sign) bLapis1.getRelative(BlockFace.DOWN).getState();
        Sign sbLapis2 = (Sign) bLapis2.getRelative(BlockFace.DOWN).getState();

        sbLapis1.setLine(1,sbLapis2.getWorld().getName());
        sbLapis2.setLine(1,sbLapis1.getWorld().getName());

        Location lLapis1 = bLapis1.getLocation();
        Location lLapis2 = bLapis2.getLocation();

        sbLapis1.setLine(2,toHex(lLapis2.getBlockX())+":"+toHex(lLapis2.getBlockY())+":"+toHex(lLapis2.getBlockZ()));
        sbLapis2.setLine(2,toHex(lLapis1.getBlockX())+":"+toHex(lLapis1.getBlockY())+":"+toHex(lLapis1.getBlockZ()));

        sbLapis1.update(true);
        sbLapis2.update(true);
    }
    
    private static int getDistance(Location loc1,Location loc2){
        return (int) Math.sqrt(Math.pow(loc2.getBlockX()-loc1.getBlockX(),2)+Math.pow(loc2.getBlockY()-loc1.getBlockY(),2)+Math.pow(loc2.getBlockZ()-loc1.getBlockZ(),2));
    }

    private boolean TelePadsWithinDistance(Block block1,Block block2){
        if(MAX_DISTANCE == 0){
            return true;
        }
        if(getDistance(block1.getLocation(),block2.getLocation()) < MAX_DISTANCE){
            return true;
        }
        return false;
    }
    
    private void Teleport(Player player,Location player_location,Block sender,Block receiver,boolean disable_teleport_wait) throws InterruptedException{
    	Thread.sleep(SEND_WAIT_TIMER * 1000);
    	
        if(getDistance(player_location,player.getLocation()) > 1){
            msgPlayer(player,"You moved, cancelling teleport!");
            return;
        }

       if(!disable_teleport_wait){
           msgPlayer(player,"Here goes nothing!");
       }

       Location lSendTo = receiver.getRelative(BlockFace.UP,2).getLocation();
       lSendTo.setX(lSendTo.getX()+0.5);
       lSendTo.setZ(lSendTo.getZ()+0.5);
       lSendTo.setPitch(player.getLocation().getPitch());
       lSendTo.setYaw(player.getLocation().getYaw());
       player.teleport(lSendTo);

       mTimeouts.put(player.getName(),System.currentTimeMillis()+5000);
        
    }


    public static void msgPlayer(Player player,String msg){
    	player.sendMessage(ChatColor.DARK_AQUA+"[TelePad] "+ChatColor.AQUA+msg);
    }

}	