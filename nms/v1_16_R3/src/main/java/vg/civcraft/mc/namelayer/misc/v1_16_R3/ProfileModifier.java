package vg.civcraft.mc.namelayer.misc.v1_16_R3;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import vg.civcraft.mc.namelayer.misc.ProfileInterface;

/**
 * @deprecated version specific NMS is bad, stop using it, stop updating this class, it's time to stop, just stop.
 *
 * After converting NMS direct access to reflection I found
 * <a href="https://github.com/caucow/NameLayer/commit/a7feb2cb">this commit</a>
 * while searching for whichever heathen decided on tabs in .editorconfig.
 * TL;DR that commit's changes were lost in
 * <a href="https://github.com/caucow/NameLayer/commit/5b57c54a">this commit</a>
 * and updating this NMS for every version is dumb so don't do that anymore, I'm resurrecting that old commit.
 * - S
 */
@Deprecated
public class ProfileModifier implements ProfileInterface {

	private static final Logger log = Logger.getLogger(ProfileModifier.class.getSimpleName());

	private boolean init;
	private Method m_CraftServer_getServer;
	private Method m_MinecraftServer_getUserCache;
	private Method m_UserCache_a;
	private Method m_CraftHumanEntity_getHandle;
	private Method m_EntityHuman_getProfile;
	private Field f_GameProfile_name;

	private void checkReflectionHandles() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
		// yeah ik this is ugly but don't feel like putting more effort into
		// sync and dunno how async the calls to setPlayerProfile are
		if (!init) {
			synchronized (this) {
				if (!init) {
					Server craftServer = Bukkit.getServer();
					String nmsPkg; // nms versioned package name
					String cbPkg; // craft bukkit versioned package name

					// CraftServer /////////////////////////////////////////////
					Class<?> c_CraftServer = craftServer.getClass();
					Method m_CraftServer_getServer = c_CraftServer.getDeclaredMethod("getServer");

					// MinecraftServer /////////////////////////////////////////
					Object minecraftServer = m_CraftServer_getServer.invoke(craftServer);
					Class<?> c_MinecraftServer = minecraftServer.getClass();
					Method m_MinecraftServer_getUserCache = c_MinecraftServer.getDeclaredMethod("getUserCache");

					// get NMS and CB package name
					nmsPkg = c_MinecraftServer.getPackage().getName();
					cbPkg = c_CraftServer.getPackage().getName();

					// GameProfile /////////////////////////////////////////////
                    /*
                    Alternatively, could add the latest compiled Paper dependency
                    from Paper's repo, but Paper doesn't maintain old versions of
                    the compiled server in its repo so woo more reflection so the
                    dependency doesn't have to be updated constantly.
                    <dependency>
                      <groupId>com.destroystokyo.paper</groupId>
                      <artifactId>paper</artifactId>
                      <version>1.16.5-R0.1-SNAPSHOT</version> <!-- Paper  -->
                      <scope>provided</scope>
                    </dependency>
                    */
					Class<?> c_GameProfile = Class.forName("com.mojang.authlib.GameProfile");
					Field f_GameProfile_name = c_GameProfile.getDeclaredField("name");
					f_GameProfile_name.setAccessible(true);
					// START - remove final modifier from name field
                    /*
                    I don't know why this is being done, unless it's something
                    to do with Java 11's new access warnings.
                    setAccessible(true) allows you to write to final fields so..
                    --commented out--
                    */
//                    Field modifiersField = Field.class.getDeclaredField("modifiers");
//                    modifiersField.setAccessible(true);
//                    modifiersField.setInt(f_GameProfile_name, f_GameProfile_name.getModifiers() & ~Modifier.FINAL);
					// END - remove final modifier from name field

					// UserCache ///////////////////////////////////////////////
					Object userCache = m_MinecraftServer_getUserCache.invoke(minecraftServer);
					Class<?> c_UserCache = userCache.getClass();
					Method m_UserCache_a = c_UserCache.getDeclaredMethod("a", c_GameProfile);

					// CraftHumanEntity ////////////////////////////////////////
					Class<?> c_CraftHumanEntity = Class.forName(cbPkg + ".CraftHumanEntity");
					Method m_CraftHumanEntity_getHandle = c_CraftHumanEntity.getDeclaredMethod("getHandle");

					// EntityHuman /////////////////////////////////////////////
					Class<?> c_EntityHuman = Class.forName(nmsPkg + ".EntityHuman");
					Method m_EntityHuman_getProfile = c_EntityHuman.getDeclaredMethod("entityHuman");

					this.m_CraftServer_getServer = m_CraftServer_getServer;
					this.m_MinecraftServer_getUserCache = m_MinecraftServer_getUserCache;
					this.m_UserCache_a = m_UserCache_a;
					this.m_CraftHumanEntity_getHandle = m_CraftHumanEntity_getHandle;
					this.m_EntityHuman_getProfile = m_EntityHuman_getProfile;
					this.f_GameProfile_name = f_GameProfile_name;
					this.init = true;
				}
			}
		}
	}

	@Override
	public void setPlayerProfle(Player player, String name) {
		String oldName = player.getName();
		if (name.length() > 16) {
			log.info(String.format("The player %s (%s) was kicked from the server due to his "
							+ "name already existing but now becoming over 16 characters.",
					name, player.getUniqueId().toString()));
		}
		try {
			checkReflectionHandles();
			// get nms player
			Object entityHuman = m_CraftHumanEntity_getHandle.invoke(player);
			// get nms player's profile
			Object gameProfile = m_EntityHuman_getProfile.invoke(entityHuman);
			// set profile's name
			f_GameProfile_name.set(gameProfile, name);

			// get nms server
			Object minecraftServer = m_CraftServer_getServer.invoke(Bukkit.getServer());
			// get nms user cache
			Object userCache = m_MinecraftServer_getUserCache.invoke(minecraftServer);
			// call UserCache.a(GameProfile)
			m_UserCache_a.invoke(userCache, gameProfile);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| ClassNotFoundException | NoSuchFieldException ex) {
			ex.printStackTrace();
		}
		player.setDisplayName(name);
		player.setPlayerListName(name);
		player.setCustomName(name);
		log.info(String.format("The player %s has had his name changed to %s.", oldName, name));
	}
}
