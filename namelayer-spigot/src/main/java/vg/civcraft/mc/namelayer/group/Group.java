package vg.civcraft.mc.namelayer.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.google.common.base.Preconditions;

import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.log.GroupActionLog;
import vg.civcraft.mc.namelayer.permission.GroupRank;
import vg.civcraft.mc.namelayer.permission.GroupRankHandler;

public class Group implements Comparable<Group> {

	private String name;
	private final int id;

	private Map<UUID, GroupRank> players;
	private Map<UUID, GroupRank> invites;
	private Map<GroupRank, Set<UUID>> playersByRank;

	private List<GroupLink> incomingLinks;
	private List<GroupLink> outgoingLinks;

	private final GroupActionLog groupActionLog;

	private GroupRankHandler rankHandler;

	private Set<Integer> secondaryIds;

	/**
	 * Instanciates a new group with no members, no invites, no links, an empty
	 * action log and the given name and id
	 * 
	 * @param name Name the group will be identifier by. Name lookup is case
	 *             insensitive, so there may never be two groups whose equal when
	 *             ignoring case
	 * @param id   Database primary key and external id used to identify this group
	 */
	public Group(String name, int id) {
		Preconditions.checkNotNull(name, "Group name may not be null");
		Preconditions.checkArgument(id >= 0, "Group id must be initialized");
		this.name = name;
		this.id = id;
		this.players = new TreeMap<>();
		this.invites = new TreeMap<>();
		this.playersByRank = new HashMap<>();
		this.incomingLinks = new ArrayList<>();
		this.outgoingLinks = new ArrayList<>();
		this.groupActionLog = new GroupActionLog(this);
		this.secondaryIds = new HashSet<>();
	}

	/**
	 * Sets the groups name. Does not update any tracking or the database, use the
	 * method in GroupManager for that
	 * 
	 * @param newName New name of the group
	 */
	public void setName(String newName) {
		this.name = newName;
	}

	/**
	 * The GroupActionLog logs any changes made by players to the group
	 * 
	 * @return GroupActionLog containing all changes made to this group
	 */
	public GroupActionLog getActionLog() {
		return groupActionLog;
	}

	/**
	 * Adds a new link pointing away from this group to this groups tracking. Just a
	 * raw setter, does not do any logic
	 * 
	 * @param link Link to add
	 */
	public void addIncomingLink(GroupLink link) {
		Preconditions.checkArgument(link.getTargetGroup() == this);
		this.incomingLinks.add(link);
	}

	/**
	 * Adds a new link pointing towards this group to this groups tracking. Just a
	 * raw setter, does not do any logic
	 * 
	 * @param link Link to add
	 */
	public void addOutgoingLink(GroupLink link) {
		Preconditions.checkArgument(link.getOriginatingGroup() == this);
		this.outgoingLinks.add(link);
	}

	/**
	 * Attempts to remove the given link from this groups tracking, assuming its an
	 * outgoing link
	 * 
	 * @param link Link to remove
	 * @return True if the link was removed, false if it wasn't found
	 */
	public boolean removeIncomingLink(GroupLink link) {
		return this.incomingLinks.remove(link);
	}

	/**
	 * Attempts to remove the given link from this groups tracking, assuming its an
	 * outgoing link
	 * 
	 * @param link Link to remove
	 * @return True if the link was removed, false if it wasn't found
	 */
	public boolean removeOutgoingLink(GroupLink link) {
		return this.outgoingLinks.remove(link);
	}

	/**
	 * @return Read-only list of all links pointing towards this group
	 */
	public List<GroupLink> getIncomingLinks() {
		return Collections.unmodifiableList(this.incomingLinks);
	}

	/**
	 * @return Read-only list of all links pointing away from this group
	 */
	public List<GroupLink> getOutgoingLinks() {
		return Collections.unmodifiableList(this.outgoingLinks);
	}

	/**
	 * @return The groups name with the color prefix set in the NameLayerMetaData of
	 *         this group
	 */
	public String getColoredName() {
		return NameLayerPlugin.getInstance().getNameLayerMeta().getMetaData(this).getChatColor() + this.name;
	}

	/**
	 * Gets the uuids of all players who are tracked by this group. This doesn't
	 * only include members of the group, but also blacklisted players, anyone who
	 * has an explicit rank set for this group
	 * 
	 * Any player not in this set will have the default non member player type as
	 * specified in the group rank handler of this group
	 * 
	 * @return Read only set of all players tracked in this group
	 */
	public Set<UUID> getAllTracked() {
		return Collections.unmodifiableSet(players.keySet());
	}

	public List<UUID> getAllMembers() {
		List<UUID> members = new ArrayList<>();
		for (Entry<UUID, GroupRank> entry : players.entrySet()) {
			if (rankHandler.isBlacklistedRank(entry.getValue())) {
				continue;
			}
			members.add(entry.getKey());
		}
		return members;
	}

	/**
	 * Checks whether the given uuid is tracked by this group, either as member or
	 * blacklisted. Use getPlayerType(uuid) instead if you need the rank for
	 * anything as both this method and getPlayerType are a map lookup
	 * 
	 * @param uuid UUID to check for
	 * @return True if the uuid is tracked meaning an explicit rank has been set for
	 *         the player, false if not
	 */
	public boolean isTracked(UUID uuid) {
		return players.containsKey(uuid);
	}

	/**
	 * Gets the uuids of all players, who are tracked with the given GroupRank. The
	 * given GroupRank may not be the default non member type
	 * 
	 * @param rank GroupRank to retrieve tracked players for
	 * @return Read-only set of all players tracked in this group with the given
	 *         GroupRank. Never null if the given GroupRank is part of this group
	 */
	public Set<UUID> getAllTrackedByType(GroupRank rank) {
		return Collections.unmodifiableSet(playersByRank.getOrDefault(rank, Collections.emptySet()));
	}

	/**
	 * Adds the player to be allowed to join a group into a specific PlayerType.
	 * 
	 * @param uuid - The UUID of the player.
	 * @param type - The PlayerType they will be joining.
	 */
	public void addInvite(UUID uuid, GroupRank type) {
		invites.put(uuid, type);
	}

	/**
	 * Get's the PlayerType of an invited Player.
	 * 
	 * @param uuid - The UUID of the player.
	 * @return Returns the PlayerType or null.
	 */
	public GroupRank getInvite(UUID uuid) {
		return invites.get(uuid);
	}

	/**
	 * Removes the invite of a Player
	 * 
	 * @param uuid - The UUID of the player.
	 */
	public void removeInvite(UUID uuid) {
		invites.remove(uuid);
	}

	/**
	 * @return All invites pending
	 */
	public Map<UUID, GroupRank> getAllInvites() {
		return new HashMap<>(invites);
	}

	/**
	 * Checks if the player is a group member or not.
	 * 
	 * @param uuid - The UUID of the player.
	 * @return Returns true if the player is a member, false otherwise.
	 */
	public boolean isMember(UUID uuid) {
		GroupRank rank = players.get(uuid);
		if (rank != null) {
			// if the type is not a child node of the non member type, it is not
			// a blacklisted type, so the player is a member
			return !rankHandler.isRelated(rank, rankHandler.getDefaultNonMemberRank());
		}
		return false;
	}

	/**
	 * Checks if the player is in the Group's PlayerType or not.
	 * 
	 * @param uuid - The UUID of the player.
	 * @param type - The PlayerType wanted.
	 * @return Returns true if the player is a member of the specific playertype,
	 *         otherwise false.
	 */
	public boolean isTracked(UUID uuid, GroupRank type) {
		GroupRank pType = players.get(uuid);
		return pType != null && pType.equals(type);
	}

	/**
	 * Retrieves the explicitly specified rank of the given player in this group and
	 * returns it if one was found. If none was found the default non member rank is
	 * returned
	 * 
	 * @param uuid - The UUID of the player whose rank should be looked up
	 * @return Rank of the player with the given UUID in this group, never null
	 */
	public GroupRank getRank(UUID uuid) {
		GroupRank member = players.get(uuid);
		if (member != null) {
			return member;
		}
		// not tracked, so default
		return rankHandler.getDefaultNonMemberRank();
	}

	/**
	 * Updates a players rank in this group. If the player does not have a rank yet,
	 * he will be assigned the given one anyway
	 * 
	 * @param uuid Player whose rank should be updated
	 * @param type New rank
	 */
	public void updateTracking(UUID uuid, GroupRank rank) {
		GroupRank oldRank = players.get(uuid);
		if (oldRank != null) {
			playersByRank.get(oldRank).remove(uuid);
		}
		playersByRank.get(rank).add(uuid);
		players.put(uuid, rank);
	}

	/**
	 * Assigns a rank to a player, assuming he didn't already have one before
	 * 
	 * @param uuid     Player to give rank to
	 * @param rank     Rank to give, must be one of this groups GroupRankHandler
	 * @param savetodb Whether this change should be persisted to the database
	 *                 (always async)
	 */
	public void addToTracking(UUID uuid, GroupRank rank) {
		if (rank == rankHandler.getDefaultNonMemberRank()) {
			return;
		}
		players.put(uuid, rank);
		playersByRank.get(rank).add(uuid);
	}

	/**
	 * Removes a player from this group tracking, taking away whatever rank he has
	 * 
	 * @param uuid            Player to take rank away from
	 * @param savetodbWhether this change should be persisted to the database
	 *                        (always async)
	 */
	public void removeFromTracking(UUID uuid) {
		GroupRank rank = players.remove(uuid);
		playersByRank.get(rank).remove(uuid);
	}

	/**
	 * @return The group name which is used to uniquely identify the group. No
	 *         groups whose names equal ignoring case may ever exist
	 */
	public String getName() {
		return name;
	}

	/**
	 * Every group has a single static id, which can be used to identify the group
	 * and its properties permanently
	 * 
	 * @return The primary id identifying this group
	 */
	public int getPrimaryId() {
		return id;
	}

	/**
	 * Groups may also specify alternative ids, which remap to this group. This is
	 * for example used when merging groups to remap all entries pointing at a group
	 * removed at another new group. These ids live in the same name space as the
	 * primary group ids and are guaranteed to be unique
	 * 
	 * @return Read-only set of all secondary ids mapping to this group
	 */
	public Collection<Integer> getSecondaryIds() {
		return Collections.unmodifiableSet(secondaryIds);
	}

	/**
	 * Adds a new secondary id mapping to this group
	 * 
	 * @param id Alternative id for this group
	 */
	public void addSecondaryId(int id) {
		secondaryIds.add(id);
	}

	/**
	 * @return GroupRankHandler which holds all information about ranks, their
	 *         relations and their permissions
	 */
	public GroupRankHandler getGroupRankHandler() {
		return rankHandler;
	}

	/**
	 * Sets the groups rank handler, should only be used by NameLayer internally
	 * when creating groups or loading them from the database
	 * 
	 * @param handler GroupRankHandler to use from now on for this group
	 */
	public void setGroupRankHandler(GroupRankHandler handler) {
		this.rankHandler = handler;
		for(GroupRank rank : handler.getAllRanks()) {
			if (rank != rankHandler.getDefaultNonMemberRank()) {
				this.playersByRank.put(rank, new HashSet<>());
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Group)) {
			return false;
		}
		Group g = (Group) obj;
		return g.getPrimaryId() == getPrimaryId();
	}

	@Override
	public int compareTo(Group o) {
		return this.name.compareToIgnoreCase(o.name);
	}
}
