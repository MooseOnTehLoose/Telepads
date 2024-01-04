package net.moosecraft.Telepads;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Telepads extends JavaPlugin implements Listener{
	
    private int MAX_DISTANCE;
    private boolean OP_ONLY;
    private boolean DISABLE_TELEPORT_WAIT;
    private boolean ENABLE_HYPER_BLOCKS;
    private int SEND_WAIT_TIMER;
    private boolean DISABLE_TELEPORT_MESSAGE;
    private boolean ENABLE_SURROUNDING_BLOCKS;
    private Material TELEPAD_CENTER_ID;
    private Material TELEPAD_SURROUNDING_ID;
    private Material TELEPAD_HYPER_ID;
    private static double xRange;
    private static double yRange;
    private static double zRange;
    
    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
    private static Map<String, Location> mLinks  = new HashMap<String, Location>();
    private static Map<String, Long> mTimeouts = new HashMap<String, Long>();
    

	public void onEnable() {
        getConfig().options().copyDefaults(true);

        MAX_DISTANCE = getConfig().getInt("max_telepad_distance");
        OP_ONLY = getConfig().getBoolean("op_only",OP_ONLY);
        DISABLE_TELEPORT_WAIT = getConfig().getBoolean("disable_teleport_wait");
        SEND_WAIT_TIMER = getConfig().getInt("send_wait_timer") * 20;
        DISABLE_TELEPORT_MESSAGE = getConfig().getBoolean("disable_teleport_message");
        TELEPAD_CENTER_ID = Material.getMaterial( getConfig().getString("telepad_center"));
        TELEPAD_SURROUNDING_ID = Material.getMaterial( getConfig().getString("telepad_surrounding"));
        ENABLE_SURROUNDING_BLOCKS = getConfig().getBoolean("enable_surrounding_blocks");
		ENABLE_HYPER_BLOCKS = getConfig().getBoolean("enable_hyper_blocks");
		xRange = getConfig().getDouble("xRange");
		yRange = getConfig().getDouble("yRange");
		zRange = getConfig().getDouble("zRange");
		getLogger().info("Telepads has been enabled");
		getServer().getPluginManager().registerEvents(this, this);
		this.saveDefaultConfig();
    	
	}//onEnable
	
	public void onDisable(){ 
		getLogger().info("Telepads has been Disabled!");
	}//onDisable
	
    private static int getDistance(Location loc1,Location loc2){
        return (int) loc1.distance(loc2);
    }
    
    public static void msgPlayer(Player player,String msg){
    	player.sendMessage(ChatColor.DARK_AQUA+"[TelePad] "+ChatColor.AQUA+msg);
    }
	
    private String toHex(int number){
        return Integer.toHexString(number + 32000);
    }

    private int toInt(String hex){
        return Integer.parseInt(hex, 16) - 32000;
    }
    
    @EventHandler // EventPriority.NORMAL by default
    public void onInteract(PlayerInteractEvent event) throws InterruptedException{
    	boolean HYPER_ON = false;
    	
        //Using a telepad, note we verify the timeout here after checking if it's a telepad
        if(event.getAction() == Action.PHYSICAL
        && event.getClickedBlock() != null
        && isTelePad(event.getClickedBlock().getRelative(BlockFace.DOWN))
        && (!mTimeouts.containsKey(event.getPlayer().getName()) 
        	|| mTimeouts.get(event.getPlayer().getName()) < System.currentTimeMillis())){
            
        	//Should resolve to TELEPAD_CENTER_ID
        	Block bSender = event.getClickedBlock().getRelative(BlockFace.DOWN);
            Block bReceiver = getTelepadReceiver(bSender);

            //Verify receiver is a working telepad
            if(bReceiver != null){

                //Verify distance
                if(!TelePadsWithinDistance(bSender,bReceiver)){
                    msgPlayer(
                    		event.getPlayer(),ChatColor.RED+"Error: Telepads are too far apart! (Distance:"
                    	    + getDistance(bSender.getLocation(),bReceiver.getLocation())
                    	    + ",MaxAllowed:"+MAX_DISTANCE+")"
                    	    );
                    return;
                }

                Sign sbReceiverSign = (Sign) bReceiver.getRelative(BlockFace.DOWN).getState();
                
            	if (isHyperPad(event.getClickedBlock().getRelative(BlockFace.DOWN)) && isHyperPad(bReceiver )){
            		HYPER_ON = true;
            	}
                
                if(!DISABLE_TELEPORT_MESSAGE){
                    String sMessage;

                    if(!DISABLE_TELEPORT_WAIT){
                    	//if(sbReceiverSign.getLine(3).equals("")){
                        if(sbReceiverSign.getSide(Side.FRONT).getLine(3).equals("")){
                            sMessage = "Preparing to send you, stand still!";
                        }else{
                            sMessage = "Preparing to send you to "
                                +ChatColor.YELLOW+sbReceiverSign.getSide(Side.FRONT).getLine(3)
                                +ChatColor.AQUA+", stand still!";
                        }//if destination labeled
                    }else{
                        if(sbReceiverSign.getSide(Side.FRONT).getLine(3).equals("")){
                            sMessage = "You have been teleported!";
                        }else{
                            sMessage = "You have been teleported to "
                                +ChatColor.YELLOW+sbReceiverSign.getSide(Side.FRONT).getLine(3);
                        }//if destination labeled
                    }//if teleport message not disabled
                    msgPlayer(event.getPlayer(),sMessage);
                }//if teleport message disabled
                
                //Don't pass wait time if disabled in config.yml. 
                if(DISABLE_TELEPORT_WAIT){
                   getServer().getScheduler().scheduleSyncDelayedTask(
                		   this,new Teleport(
                				   event.getPlayer(),event.getPlayer().getLocation(),bReceiver,HYPER_ON)
                		   );
                }else{
                    getServer().getScheduler().scheduleSyncDelayedTask(
                    		this,new Teleport(
                    				event.getPlayer(),event.getPlayer().getLocation(),bReceiver,HYPER_ON),
                    				SEND_WAIT_TIMER);
               }//else send wait timer
            }//if verify working telepad
        }//if oninteract with telepad
        
        //Creating a telepad link, check for redstone, correct blocks
        else if(event.getItem() != null
        && event.getItem().getType() == Material.REDSTONE
        && event.getClickedBlock() != null
        && isTelePad(event.getClickedBlock().getRelative(BlockFace.DOWN))){
           
        	if(getTelepadReceiver(event.getClickedBlock().getRelative(BlockFace.DOWN)) != null){
                msgPlayer(event.getPlayer(),"Error: This telepad seems to be linked already!");
                msgPlayer(
                		event.getPlayer(),ChatColor.YELLOW
                		+ "You can reset it by breaking the pressure pad on top of it, then clicking the  with redstone."
                		);

                return;
            }

            //Determine the action
            if(!mLinks.containsKey(event.getPlayer().getName())){
                //Initial telepad click
                mLinks.put(event.getPlayer().getName(),event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation());
                msgPlayer(event.getPlayer(),"Telepad location stored!");

                return;
            }else{
                //They have a stored location, and right clicked  a telepad , so remove the temp location
                if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
                    mLinks.remove(event.getPlayer().getName());
                    msgPlayer(event.getPlayer(),"Telepad location ditched! (right clicked)");

                    return;
                }else{
                    //Setting up the second link
                    Block bFirst = mLinks.get(event.getPlayer().getName()).getBlock();

                    if(isTelePad(bFirst)){
                        Block bSecond = event.getClickedBlock().getRelative(BlockFace.DOWN);

                        if(!TelePadsWithinDistance(bFirst,bSecond)){
                            msgPlayer(
                            		    event.getPlayer(),ChatColor.RED
                                      +"Error: Telepads are too far apart! (Distance:"
                            		  +getDistance(bFirst.getLocation(),event.getClickedBlock().getLocation())
                            		  +",MaxAllowed:"+MAX_DISTANCE+")"
                            		  );

                            return;
                        }//if valid range

                        //Compare locations of first and second block with location.equals()
                        if(bFirst.getLocation().equals(bSecond.getLocation())){
                            msgPlayer(event.getPlayer(),ChatColor.RED+"Error: You cannot connect a telepad to itself.");
                            mLinks.remove(event.getPlayer().getName());

                            return;
                        }//if left click on first telepad, don't link telepads

                        mLinks.remove(event.getPlayer().getName());
                        linkTelepadReceivers(bFirst,event.getClickedBlock().getRelative(BlockFace.DOWN));
                        msgPlayer(event.getPlayer(),"Telepad location transferred!");

                        return;
                    }//if first telepad exists
                }//if not right click on first telepad
            }//if first telepad already set, start creating second telepad 
        }//else if creating a new telepad
        
        //Resetting telepad
        else if(event.getItem() != null
        && event.getItem().getType() == Material.REDSTONE
        && event.getClickedBlock() != null
        && event.getClickedBlock().getType() == TELEPAD_CENTER_ID){
        	
            Block bReset = event.getClickedBlock();

            if (bReset.getType() == TELEPAD_CENTER_ID && (
            	    (  bReset.getRelative(BlockFace.EAST).getType() == TELEPAD_SURROUNDING_ID
                    && bReset.getRelative(BlockFace.WEST).getType() == TELEPAD_SURROUNDING_ID
                    && bReset.getRelative(BlockFace.NORTH).getType() == TELEPAD_SURROUNDING_ID
                    && bReset.getRelative(BlockFace.SOUTH).getType() == TELEPAD_SURROUNDING_ID
            		) || !ENABLE_SURROUNDING_BLOCKS	)
            		&& bReset.getRelative(BlockFace.DOWN).getBlockData() instanceof org.bukkit.block.Sign
            		&& bReset.getRelative(BlockFace.UP).getType() == Material.AIR
              
              )//enclosed if statement
            
            {//end if check
            	
                Sign sbResetSign = (Sign) bReset.getRelative(BlockFace.DOWN).getState();
                sbResetSign.getSide(Side.FRONT).setLine(1,"");
                sbResetSign.getSide(Side.FRONT).setLine(2,"");
                sbResetSign.update();
                msgPlayer(event.getPlayer(),"Telepad Reset!");

                return;
                
            }//if check trigger for Telepad
        }//if reset Telepad
    }//onInteract
    
    private boolean isHyperPad(Block center) {
		if (isTelePad( center) && ENABLE_SURROUNDING_BLOCKS && ENABLE_HYPER_BLOCKS){
			Block up = center.getRelative(BlockFace.NORTH);
			
			Boolean upL = (up.getRelative(BlockFace.EAST).getType() == TELEPAD_HYPER_ID);
			Boolean upR = (up.getRelative(BlockFace.WEST).getType() == TELEPAD_HYPER_ID);
			
			Block dn =  center.getRelative(BlockFace.NORTH);
			Boolean dnL = (dn.getRelative(BlockFace.EAST).getType() == TELEPAD_HYPER_ID);
			Boolean dnR = (dn.getRelative(BlockFace.WEST).getType() == TELEPAD_HYPER_ID);
			
			if ( upL && upR && dnL && dnR ){
				
				return true;
			
			}//if a hyperpad	
		
		}//if a telepad
		
		return false;
	
    }//isHyperPad
    
    //returns true if telepad structure is correct
    public boolean isTelePad(Block center){
    	Boolean in = ( center.getType() == TELEPAD_CENTER_ID );
    	Boolean hi = ( center.getRelative(BlockFace.UP).getType() == Material.STONE_PRESSURE_PLATE);
    	BlockData sign = center.getRelative(BlockFace.DOWN).getBlockData();   	
    	Boolean lo = ( sign instanceof org.bukkit.block.data.type.Sign || sign instanceof org.bukkit.block.data.type.WallSign );

    	if (ENABLE_SURROUNDING_BLOCKS){
        	Boolean up = ( center.getRelative(BlockFace.NORTH).getType() == TELEPAD_SURROUNDING_ID );
        	Boolean dn = ( center.getRelative(BlockFace.SOUTH).getType() == TELEPAD_SURROUNDING_ID );
        	Boolean ri = ( center.getRelative(BlockFace.EAST).getType() == TELEPAD_SURROUNDING_ID );
        	Boolean le = ( center.getRelative(BlockFace.WEST).getType() == TELEPAD_SURROUNDING_ID );
    		
    		if ( in && up && dn && ri && le && hi && lo ){  
        		return true; 
        	}
    	}
    	else if (!ENABLE_SURROUNDING_BLOCKS) {
        	if ( in && hi && lo ){  
        		return true; 
        	}	
    	}
    	
    	return false;
    			
    }//isTelePad
    
    //get the Telepad linked to the sending Block
    private Block getTelepadReceiver(Block bSender){
        Block bSenderSign = bSender.getRelative(BlockFace.DOWN);
        //check if Block below Telepad is a Sign
        BlockData sign = bSender.getRelative(BlockFace.DOWN).getBlockData();
        if( sign instanceof org.bukkit.block.data.type.Sign || sign instanceof org.bukkit.block.data.type.WallSign ){
            Sign sbSenderSign = (Sign) bSenderSign.getState();
            String sHexLocation = sbSenderSign.getSide(Side.FRONT).getLine(2);
            String sWorld = sbSenderSign.getSide(Side.FRONT).getLine(1);
            String[] sXYZ = sHexLocation.split(":");
            World world = getServer().getWorld(sWorld);

            if(world == null){
                return null;
            }
            
            Block bReceiver = world.getBlockAt(toInt(sXYZ[0]),toInt(sXYZ[1]),toInt(sXYZ[2]));
            
            //return block if its a telepad
            if(isTelePad(bReceiver)){
                return bReceiver;
            }
        }//if Block is Sign
        return null;
    }//getTelepadReceiver
    
    //currently assumes you checked both blocks with isTelePad
    private void linkTelepadReceivers(Block b1,Block b2){
        Sign sb1 = (Sign) b1.getRelative(BlockFace.DOWN).getState();
        Sign sb2 = (Sign) b2.getRelative(BlockFace.DOWN).getState();

        sb1.getSide(Side.FRONT).setLine(1,sb2.getWorld().getName());
        sb2.getSide(Side.FRONT).setLine(1,sb1.getWorld().getName());

        Location l1 = b1.getLocation();
        Location l2 = b2.getLocation();

        sb1.getSide(Side.FRONT).setLine(2,toHex(l2.getBlockX())+":"+toHex(l2.getBlockY())+":"+toHex(l2.getBlockZ()));
        sb2.getSide(Side.FRONT).setLine(2,toHex(l1.getBlockX())+":"+toHex(l1.getBlockY())+":"+toHex(l1.getBlockZ()));

        sb1.update(true);
        sb2.update(true);
    }
    
    private boolean TelePadsWithinDistance(Block block1,Block block2){
        if(MAX_DISTANCE == 0){
            return true;
        }
        if(getDistance(block1.getLocation(),block2.getLocation()) < MAX_DISTANCE){
            return true;
        }
        return false;
    }//TelePadsWithinDistance
    
    //Handles the actual transfer from pad to pad, takes a delay in server tic format
    private static class Teleport implements Runnable{
        
    	private final Player player;
        private final Location player_location;
        private final Block receiver;
        private final boolean hyper_active;
    	
    	Teleport(Player player,Location player_location,Block receiver,boolean hyper_active){
            this.player = player;
            this.player_location = player_location;
            this.receiver = receiver;
            this.hyper_active = hyper_active;
    	}

        @Override
        public void run() {
        	
        	if (hyper_active) {
        		
        		if(getDistance(player_location, player.getLocation()) > 1){	
        			return;
        		}
        		
        		
        		List<Entity> entities =  player.getNearbyEntities(xRange,yRange,zRange);
        		
        		for (Entity e : entities){
        			Location lSendTo = receiver.getRelative(BlockFace.UP,2).getLocation();
        			lSendTo.setX(lSendTo.getX()+0.5);
            		lSendTo.setZ(lSendTo.getZ()+0.5);
            		lSendTo.setPitch(e.getLocation().getPitch());
            		lSendTo.setYaw(player.getLocation().getYaw());
            		e.teleport(lSendTo);
        		}
        		
        		Location lSendTo = receiver.getRelative(BlockFace.UP,2).getLocation();
        		lSendTo.setX(lSendTo.getX()+0.5);
        		lSendTo.setZ(lSendTo.getZ()+0.5);
        		lSendTo.setPitch(player.getLocation().getPitch());
        		lSendTo.setYaw(player.getLocation().getYaw());
        		player.teleport(lSendTo);
        		
        		mTimeouts.put(player.getName(),System.currentTimeMillis()+5000);
        		
        		
        	}
        	
        	else{
        		//cancel teleport if the player moves
        		if(getDistance(player_location, player.getLocation()) > 1){
        			return;
        		}

        		Location lSendTo = receiver.getRelative(BlockFace.UP,2).getLocation();
        		lSendTo.setX(lSendTo.getX()+0.5);
        		lSendTo.setZ(lSendTo.getZ()+0.5);
        		lSendTo.setPitch(player.getLocation().getPitch());
        		lSendTo.setYaw(player.getLocation().getYaw());
        		player.teleport(lSendTo);
        		mTimeouts.put(player.getName(),System.currentTimeMillis()+5000);
            
        	}//if regular
        }//run
    }//class Teleport
}//class Telepads	
