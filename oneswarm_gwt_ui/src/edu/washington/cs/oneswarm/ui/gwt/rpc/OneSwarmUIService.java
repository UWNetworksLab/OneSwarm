package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;

public interface OneSwarmUIService extends RemoteService {
    Boolean startBackend() throws OneSwarmException;

    String getVersion(String session);

    Boolean createSwarmFromLocalFileSystemPath(String session, String basePath,
            ArrayList<String> paths, boolean startSeeding, String announce,
            ArrayList<PermissionsGroup> inPermittedGroups);

    ReportableException reportError(ReportableException inError);

    String ping(String session, String latestVersion) throws Exception;

    TorrentList getTorrentsInfo(String session, int page);

    TorrentList getTransferringInfo(String session);

    HashMap<String, String> getSidebarStats(String session);

    HashMap<String, String> getDataStats(String session);

    HashMap<String, String> getLimits(String session);

    HashMap<String, String> getCounts(String session);

    boolean getStopped(String session);

    boolean recentFriendChanges(String session);

    void setRecentChanges(String session, boolean value);

    void setLimits(String session, String day, String week, String month, String year);

    String[] checkIfWarning(String session);

    void resetLimit(String session, String limittype);

    boolean startTorrent(String session, String[] torrentIDs);

    boolean stopTorrent(String session, String[] torrenIDs);

    int downloadTorrent(String session, String path);

    void addDownloadFromLocalTorrentDefaultSaveLocation(String session, String inPathToTorrent,
            ArrayList<PermissionsGroup> inPermissions) throws OneSwarmException;

    // void addDownloadFromLocalTorrent(String session, String path,
    // String savePath, boolean skipCheck, ArrayList<PermissionsGroup>
    // inPermissions)
    // throws OneSwarmException;

    int downloadTorrent(String session, int friendConnection, int channelId, String torrentId,
            int lengthHint);

    Integer getTorrentDownloadProgress(String session, int torrentDownloadID);

    FileListLite[] getTorrentFiles(String session, int torrentDownloadID);

    Boolean addTorrent(String session, int torrentDownloadID, FileListLite[] selectedFiles,
            ArrayList<PermissionsGroup> inPerms, String path, boolean noStream);

    Boolean torrentExists(String session, String torrentID);

    boolean deleteData(String session, String[] torrentID);

    ReportableException deleteFromShareKeepData(String session, String[] torrentID);

    ReportableException deleteCompletely(String session, String[] torrentID);

    void addFriend(String session, FriendInfoLite friendInfoLite, boolean testOnly)
            throws OneSwarmException;

    FriendInfoLite[] scanXMLForFriends(String session, String text) throws OneSwarmException;

    void applySwarmPermissionChanges(String session, ArrayList<TorrentInfo> inSwarms);

    FriendList getFriends(String session, int prevListId, boolean includeDisconnected,
            boolean includeBlocked);

    int getNumberFriendsCount(String session);

    String getMyPublicKey(String session);

    FileListLite[] getFileList(String session, int connectionId, String filter, int startNum,
            int num, long maxCacheAge);

    Integer sendSearch(String session, String searchString);

    TextSearchResultLite[] getSearchResult(String session, int searchId);

    ReportableException revealSwarmInFinder(String session, TorrentInfo[] inSwarm);

    // void revealPathInFinder(String session, String path);

    ReportableException openFileDefaultApp(String session, TorrentInfo[] inSwarm);

    FileTree getFiles(String session, String path);

    ArrayList<HashMap<String, String>> getFriendTransferStats(String session);

    void setFriendsSettings(String session, FriendInfoLite[] updated);

    FriendList getPendingCommunityFriendImports(String session) throws OneSwarmException;

    FriendInfoLite[] getNewUsersFromXMPP(String session, String xmppNetworkName, String username,
            char[] password, String machineName) throws OneSwarmException;

    int pollCommunityServer(String session, CommunityRecord record) throws OneSwarmException;

    HashMap<String, Integer> getTorrentsState(String session);

    String getComputerName(String session);

    void setComputerName(String session, String computerName);

    TorrentInfo[] pagedTorrentStateRefresh(String session, ArrayList<String> whichOnes);

    Integer getIntegerParameterValue(String session, String inParamName);

    void setIntegerParameterValue(String session, String inParamName, Integer inValue);

    Boolean getBooleanParameterValue(String session, String inParamName);

    void setBooleanParameterValue(String session, String inParamName, Boolean inValue);

    String getStringParameterValue(String session, String inParamName);

    void setStringParameterValue(String session, String inParamName, String inValue);

    ArrayList<String> getStringListParameterValue(String session, String inParamName);

    List<CommunityRecord> getCommunityServers(String session);

    void setStringListParameterValue(String session, String inParamName, ArrayList<String> value);

    int getDownloadManagersCount(String session);

    PagedTorrentInfo getPagedAndFilteredSwarms(int inPage, int swarmsPerPage, String filter,
            int sort, String type, boolean includeF2F, int selectedFriendID, String inTagPath);

    FileListLite[] getFilesForDownloadingTorrentHash(String session, String inOneSwarmHash);

    String getTorrentName(String session, int inID);

    ReportableException updateSkippedFiles(String session, FileListLite[] lites);

    ArrayList<PermissionsGroup> getAllGroups(String session);

    ArrayList<FriendInfoLite> getFriendsForGroup(String session, PermissionsGroup inGroup);

    ArrayList<PermissionsGroup> getGroupsForSwarm(String session, TorrentInfo inSwarm);

    ReportableException setGroupsForSwarm(String session, TorrentInfo inSwarm,
            ArrayList<PermissionsGroup> inGroups);

    PermissionsGroup updateGroupMembership(String session, PermissionsGroup inGroup,
            ArrayList<FriendInfoLite> inMembers) throws OneSwarmException;

    ReportableException removeGroup(String session, Long inGroupID);

    void connectToFriends(String session, FriendInfoLite[] friendLite);

    FriendInfoLite getUpdatedFriendInfo(String session, FriendInfoLite friendLite);

    BackendTask[] getBackendTasks(String session);

    BackendTask getBackendTask(String session, int inID);

    void cancelBackendTask(String session, int inID);

    /**
	 * 
	 */
    String debug(String session, String which);

    FriendInfoLite[] getLanOneSwarmUsers(String session);

    HashMap<String, String> getDeniedIncomingConnections(String session);

    String getRemoteAccessUserName(String session);

    String saveRemoteAccessCredentials(String session, String username, String password);

    String[] getListenAddresses(String session);

    HashMap<String, Integer> getNewFriendsCountsFromAutoCheck(String session);

    String getPlatform(String session);

    void deleteFriends(String session, FriendInfoLite[] friend);

    void addToIgnoreRequestList(String session, FriendInfoLite friend);

    String getGtalkStatus(String session);

    FileTree getAllTags(String session);

    FileTree getTags(String session, String inOneSwarmHash) throws OneSwarmException;

    void setTags(String session, String inOneSwarmHash, String[] path);

    FriendInfoLite getSelf(String session);

    HashMap<String, String[]> getUsersWithMessages(String session);

    HashMap<String, Integer> getUnreadMessageCounts(String session);

    SerialChatMessage[] getMessagesForUser(String session, String base64Key, boolean include_read,
            int limit);

    boolean sendChatMessage(String session, String base64Key, SerialChatMessage message)
            throws OneSwarmException;

    int clearChatLog(String session, String base64Key);

    void updateRemoteAccessIpFilter(String session, String selectedFilterType, String filterString)
            throws OneSwarmException;

    ArrayList<BackendErrorReport> getBackendErrors(String session);

    String getDebugMessageLog(String session, String friendKey);

    String[] getBase64HashesForOneSwarmHashes(String session, String[] inOneSwarmHashes);

    String[] getBase64HashesForBase32s(String session, String[] inBase32s) throws OneSwarmException;

    FriendInvitationLite createInvitation(String session, String name, boolean canSeeFileList,
            long maxAge, SecurityLevel securityLevel);

    void redeemInvitation(String session, FriendInvitationLite invitation, boolean testOnly)
            throws OneSwarmException;

    ArrayList<FriendInvitationLite> getSentFriendInvitations(String session);

    ArrayList<FriendInvitationLite> getRedeemedFriendInvitations(String session);

    void updateFriendInvitations(String sessionID, FriendInvitationLite invitation);

    void deleteFriendInvitations(String sessionID, ArrayList<FriendInvitationLite> invitations);

    String copyTorrentInfoToMagnetLink(String sessionID, String[] torrentIDs)
            throws OneSwarmException;

    void refreshFileAssociations(String session) throws OneSwarmException;

    LocaleLite[] getLocales(String session);

    HashMap<String, String> getFileInfo(String session, FileListLite file, boolean getFFmpegData)
            throws OneSwarmException;

    void applyDefaultSettings(String session);

    int getNumberOnlineFriends(String session);

    BackendTask performSpeedCheck(String session, double setWithFraction);

    BackendTask publishSwarms(String session, TorrentInfo[] infos, String[] previewPaths,
            String[] comments, String[] categories, CommunityRecord toServer);

    ArrayList<String> getCategoriesForCommunityServer(String sessionID, CommunityRecord selected);

    void triggerNatCheck(String sessionID);

    HashMap<String, String> getNatCheckResult(String sessionID);

    void fixPermissions(String session, TorrentInfo torrent, boolean inFixAll)
            throws OneSwarmException;

    Boolean isStreamingDownload(String session, String infohash);

    void setStreamingDownload(String session, String infohash, boolean streaming);

    String getMultiTorrentSourceTemp(String session);

    FileInfo[] listFiles(String session, String string);

    ClientServiceInfo[] getClientServices(String session);

    void removeClientService(String session, long id);

    void addClientService(String sessionID, long id, String name);

    String activateClientService(String session, String name, long id);

    String getNewServiceKey();

    String setExitPolicy(String exitNodes, String localPort, int mode);

    List<String> getExitPolicy();

    String[] getNickname();

    void setNickname(String nickname, Boolean published, Boolean enabled);
}
