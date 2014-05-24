package me.moose.Telepads;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;
import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.World;

public class TelepadsListener implements Listener{

    private final Telepads plugin;
    private static Map<String, Location> mLapisLinks  = new HashMap<String, Location>();
    private static Map<String, Long> mTimeouts = new HashMap<String, Long>();
    
    //Ticks to wait between standing on a telepad, and sending the player
    private int SEND_WAIT_TIMER = 60;
    
    public TelepadsListener(Telepads instance){
        this.plugin = instance;
    }

    public static void msgPlayer(Player player,String msg){
        player.sendMessage(ChatColor.DARK_AQUA+"[TelePad] "+ChatColor.AQUA+msg);
    }


    //returns true if telepad structure is correct
    public boolean isTelePadLapis(Block center){
    	Boolean in = ( center.getType() == plugin.TELEPAD_CENTER_ID );
    	Boolean up = ( center.getRelative(BlockFace.NORTH).getType() == plugin.TELEPAD_SURROUNDING_ID );
    	Boolean dn = ( center.getRelative(BlockFace.SOUTH).getType() == plugin.TELEPAD_SURROUNDING_ID );
    	Boolean ri = ( center.getRelative(BlockFace.EAST).getType() == plugin.TELEPAD_SURROUNDING_ID );
    	Boolean le = ( center.getRelative(BlockFace.WEST).getType() == plugin.TELEPAD_SURROUNDING_ID );
    	Boolean sn = ( 
    			(center.getRelative(BlockFace.DOWN).getType() == Material.SIGN_POST) |
    			(center.getRelative(BlockFace.DOWN).getType() == Material.WALL_SIGN	));

    	if ( in && up && dn && ri && le && sn ){  
    		return true; 
    	}
    	
    	return false;
    			
    }		
    			


    private String toHex(int number){
        return Integer.toHexString(number + 32000);
    }

    private int toInt(String hex){
        return Integer.parseInt(hex, 16) - 32000;
    }

    private Block getTelepadLapisReceiver(Block bSenderLapis){
        Block bSenderSign = bSenderLapis.getRelative(BlockFace.DOWN);

        if(bSenderSign.getType() == Material.WALL_SIGN || bSenderSign.getType() == Material.SIGN_POST){
            Sign sbSenderSign = (Sign) bSenderSign.getState();

            String sHexLocation = sbSenderSign.getLine(2);

            String sWorld = sbSenderSign.getLine(1);
            String[] sXYZ = sHexLocation.split(":");

            World world = plugin.getServer().getWorld(sWorld);

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
        if(plugin.MAX_DISTANCE == 0){
            return true;
        }
        if(getDistance(block1.getLocation(),block2.getLocation()) < plugin.MAX_DISTANCE){
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
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
                    msgPlayer(event.getPlayer(),ChatColor.RED+"Error: Telepads are too far apart! (Distance:"+getDistance(bSenderLapis.getLocation(),bReceiverLapis.getLocation())+",MaxAllowed:"+plugin.MAX_DISTANCE+")");

                    return;
                }

                Sign sbReceiverSign = (Sign) bReceiverLapis.getRelative(BlockFace.DOWN).getState();
                
                if(!plugin.DISABLE_TELEPORT_MESSAGE){
                    String sMessage;

                    if(!plugin.DISABLE_TELEPORT_WAIT){
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
                
                if(plugin.DISABLE_TELEPORT_WAIT){
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadTeleport(event.getPlayer(),event.getPlayer().getLocation(),bSenderLapis,bReceiverLapis,plugin.DISABLE_TELEPORT_WAIT));
                }else{
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadTeleport(event.getPlayer(),event.getPlayer().getLocation(),bSenderLapis,bReceiverLapis,plugin.DISABLE_TELEPORT_WAIT),SEND_WAIT_TIMER);
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
            if(!mLapisLinks.containsKey(event.getPlayer().getName())){
                //Initial telepad click
                mLapisLinks.put(event.getPlayer().getName(),event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation());

                msgPlayer(event.getPlayer(),"Telepad location stored!");

                return;
            }else{
                //They have a stored location, and right clicked  a telepad lapis, so remove the temp location
                if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
                    mLapisLinks.remove(event.getPlayer().getName());

                    msgPlayer(event.getPlayer(),"Telepad location ditched! (right clicked)");

                    return;
                }else{
                    //Setting up the second link
                    Block bFirstLapis = mLapisLinks.get(event.getPlayer().getName()).getBlock();

                    if(isTelePadLapis(bFirstLapis)){
                        Block bSecondLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);

                        if(!TelePadsWithinDistance(bFirstLapis,bSecondLapis)){
                            msgPlayer(event.getPlayer(),ChatColor.RED+"Error: Telepads are too far apart! (Distance:"+getDistance(bFirstLapis.getLocation(),event.getClickedBlock().getLocation())+",MaxAllowed:"+plugin.MAX_DISTANCE+")");

                            return;
                        }

                        //The same telepad?
                        if(bFirstLapis == bSecondLapis){
                            msgPlayer(event.getPlayer(),ChatColor.RED+"Error: You cannot connect a telepad to itself.");

                            msgPlayer(event.getPlayer(),"Well you could, but why would you want to? Maybe   you want a door or something?");

                            mLapisLinks.remove(event.getPlayer().getName());

                            return;
                        }

                        mLapisLinks.remove(event.getPlayer().getName());

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
            && (plugin.TELEPAD_SURROUNDING_ID == Material.AIR
                || (bResetLapis.getRelative(BlockFace.EAST).getType() == plugin.TELEPAD_SURROUNDING_ID
                    && bResetLapis.getRelative(BlockFace.WEST).getType() == plugin.TELEPAD_SURROUNDING_ID
                    && bResetLapis.getRelative(BlockFace.NORTH).getType() == plugin.TELEPAD_SURROUNDING_ID
                    && bResetLapis.getRelative(BlockFace.SOUTH).getType() == plugin.TELEPAD_SURROUNDING_ID))
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

    private static class BluePadTeleport implements Runnable{
        private final Player player;
        private final Location player_location;
        private final Block receiver;
        private final boolean disable_teleport_wait;

        BluePadTeleport(Player player,Location player_location,Block senderLapis,Block receiverLapis,boolean disable_teleport_wait){
            this.player = player;
            this.player_location = player_location;
            this.receiver = receiverLapis;
            this.disable_teleport_wait = disable_teleport_wait;
        }

        public void run(){
            if(getDistance(player_location,player.getLocation()) > 1){
                msgPlayer(player,"You moved, cancelling teleport!");
                return;
            }

            if(!this.disable_teleport_wait){
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
    }
}