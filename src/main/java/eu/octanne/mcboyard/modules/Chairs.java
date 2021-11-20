package eu.octanne.mcboyard.modules;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import eu.octanne.mcboyard.McBoyard;
import eu.octanne.mcboyard.entity.ChairEntity;
import eu.octanne.mcboyard.entity.EntityCustom;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_12_R1.PacketPlayInSteerVehicle;
import net.minecraft.server.v1_12_R1.PacketPlayOutMount;

public class Chairs extends Module implements Listener {
	
	public Chairs(JavaPlugin instance) {
		super(instance);
	}

	public ArrayList<Chair> playerOnChairs;
	
	public void onEnable() {
		
		playerOnChairs = new ArrayList<Chair>();
		
		for(Player player : Bukkit.getOnlinePlayers()) {
			injectPlayer(player);
		}
		Bukkit.getPluginManager().registerEvents(this, McBoyard.instance);
	}
	
	public void onDisable() {
		for(Chair chair : playerOnChairs) {
			chair.destroy();
		}
		for(Player player : Bukkit.getOnlinePlayers()) {
			removePlayer(player);
		}
		HandlerList.unregisterAll(this);
	}
	
	private void removePlayer(Player player) {
		Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
		channel.eventLoop().submit(()->{
			channel.pipeline().remove(player.getName());
			return null;
		});
	}
	
	private void injectPlayer(Player player) {
		ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
			
			@Override
			public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception {
				if(packet.toString().contains("PacketPlayInSteerVehicle")) {
					super.channelRead(channelHandlerContext, packet);
					PacketPlayInSteerVehicle packetReadable = (PacketPlayInSteerVehicle) packet;
					//Dismount
					Field field4 = packetReadable.getClass().getDeclaredField("d");
					field4.setAccessible(true);// allows us to access the field
					boolean dismount = field4.getBoolean(packetReadable);

					Chair chairD = null;
					for(Chair chair : playerOnChairs) {
						if(chair.playerOnChair.getEntityId() == player.getEntityId() && dismount) {
							chair.destroy();
							chairD = chair;
						}
					}
					if(chairD != null)playerOnChairs.remove(chairD);
				}else {
					super.channelRead(channelHandlerContext, packet);
				}
				//Bukkit.getServer().getConsoleSender().sendMessage("§ePacket READ : §c" + packet.toString());
			}
			
			@Override
			public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise channelPromise) throws Exception {
				//Bukkit.getServer().getConsoleSender().sendMessage("§bPacket READ : §c" + packet.toString());
				super.write(channelHandlerContext, packet, channelPromise);
			}
			
		};
		
		ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.pipeline();
		pipeline.addBefore("packet_handler", player.getName(), channelDuplexHandler);
	}
	
	@EventHandler
	public void onPlayerClickOnChair(PlayerInteractEvent e) {
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if(e.getClickedBlock().getType().equals(Material.JUNGLE_WOOD_STAIRS)) {
				boolean isExist = false;
				for(Chair chair : playerOnChairs) {
					if(chair.x == e.getClickedBlock().getX() && chair.y == e.getClickedBlock().getY() && 
							chair.z == e.getClickedBlock().getZ()) {
						isExist = true;
						break;
					}
				}
				if(!isExist) {
					Chair chair = new Chair(e.getPlayer(), e.getClickedBlock().getLocation().clone());
					playerOnChairs.add(chair);
				}
			}
		}else return;
	}
	
	@EventHandler
	public void onArrowDeath(EntityDeathEvent e) {
		if(e.getEntityType().equals(EntityType.ARMOR_STAND)) {
			for(Chair chair : playerOnChairs) {
				if(chair.armorstand.getBukkitEntity().getEntityId() == e.getEntity().getEntityId()) {
					chair.destroy();
					playerOnChairs.remove(chair);
					return;
				}
			}
		}
	}
	
	@EventHandler 
	public void onChairDestroy(BlockBreakEvent e) {
		if(e.getBlock().getType().equals(Material.JUNGLE_WOOD_STAIRS)) {
			Chair chairD = null;
			for(Chair chair : playerOnChairs) {
				if(chair.x == e.getBlock().getX() && chair.y == e.getBlock().getY() && 
						chair.z == e.getBlock().getZ()) {
					chair.destroy();
					chairD = chair;
				}
			}
			if(chairD != null)playerOnChairs.remove(chairD);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		//PACKET SYSTEM
		removePlayer(e.getPlayer());
		
		Chair chairD = null;
		for(Chair chair : playerOnChairs) {
			if(chair.playerOnChair.equals(e.getPlayer())){
				chair.destroy();
				chairD = chair;
			}
		}
		if(chairD != null) playerOnChairs.remove(chairD);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		//PACKET SYSTEM
		injectPlayer(e.getPlayer());
		
		// DELETE COOLDOWN
		e.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(50);
	}
	
	public class Chair {
		
		ChairEntity armorstand;
		Player playerOnChair;
		// Coordonate
		int x,y,z;
		Location locEnter;
		
		public Chair(Player p, Location loc) {
			locEnter = p.getLocation().clone();
			x = loc.getBlockX(); y = loc.getBlockY(); z = loc.getBlockZ();
			playerOnChair = p;
			loc.setZ(loc.getZ()+0.5);
			loc.setX(loc.getX()+0.5);
			loc.setY(loc.getY()-0.4);
			//loc.setYaw(0.0f);
			//loc.setPitch(-90.0f);
			
			armorstand = new ChairEntity(loc.getWorld());
			EntityCustom.spawnEntity(armorstand, loc);
			//ArmorStand armorstand = (ArmorStand) armorstandBase;
			
			/*
			armorstand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
			armorstand.setVisible(false);
			armorstand.setSmall(true);
			armorstand.setInvulnerable(true);
			armorstand.setBasePlate(false);
			armorstand.setAI(false);
			armorstand.setHealth(2.0);
			armorstand.setMaxHealth(2.0);
			armorstand.setCollidable(false);
			armorstand.setGravity(false);
			*/
			
			//PACKET
			for(Player pS : Bukkit.getOnlinePlayers()) {
				CraftPlayer pC = (CraftPlayer) pS;
				PacketPlayOutMount npc = new PacketPlayOutMount(pC.getHandle());
				 
				//the a field used to be public, we'll need to use reflection to access:
				try {
				    Field field = npc.getClass().getDeclaredField("a");
				    field.setAccessible(true);// allows us to access the field
				 
				    field.setInt(npc, armorstand.getBukkitEntity().getEntityId());// sets the field to an integer
				    
				    Field field2 = npc.getClass().getDeclaredField("b");
				    field2.setAccessible(true);// allows us to access the field
					 
				    int[] passengerList = {playerOnChair.getEntityId()};
				    field2.set(npc, passengerList);// sets the field to an integer
				    
				} catch(Exception x) {
				    x.printStackTrace();
				}
				
				//now comes the sending
				pC.getHandle().playerConnection.sendPacket(npc);
			}
			//armorstand.getBukkitEntity().addPassenger((CraftPlayer)p);
		}
		
		public void destroy() {
			//playerOnChair.leaveVehicle();
			//playerOnChair.teleport(locEnter);
			armorstand.getBukkitEntity().remove();
		}
	}
}