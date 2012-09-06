package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;

public interface OneSwarmUIServiceAsync {
    void startBackend(AsyncCallback callback);

    void getVersion(String session, AsyncCallback<String> callback);

    void createSwarmFromLocalFileSystemPath(String session, String basePath,
            ArrayList<String> path, boolean startSeeding, String announce,
            ArrayList<PermissionsGroup> inPermittedGroups, AsyncCallback<Boolean> callback);

    void reportError(ReportableException inError, AsyncCallback callback);

    void getTorrentsInfo(String session, int page, AsyncCallback callback);

    void getTransferringInfo(String session, AsyncCallback<TorrentList> inCallback);

    void getStopped(String session, AsyncCallback<Boolean> inCallback);

    void recentFriendChanges(String session, AsyncCallback<Boolean> inCallback);

    void setRecentChanges(String session, boolean value, AsyncCallback callback);

    void getSidebarStats(String session, AsyncCallback<HashMap<String, String>> inCallback);

    void getLimits(String session, AsyncCallback<HashMap<String, String>> inCallback);

    void getDataStats(String session, AsyncCallback<HashMap<String, String>> inCallback);

    void getCounts(String session, AsyncCallback<HashMap<String, String>> inCallback);

    void resetLimit(String session, String limittype, AsyncCallback callback);

    void checkIfWarning(String session, AsyncCallback<String[]> inCallback);

    void setLimits(String session, String day, String week, String month, String year,
            AsyncCallback callback);

    void ping(String session, String version, AsyncCallback<String> callback);

    void startTorrent(String session, String[] torrentID, AsyncCallback<Boolean> callback);

    void stopTorrent(String session, String[] torrenID, AsyncCallback<Boolean> callback);

    void downloadTorrent(String session, String path, AsyncCallback<Integer> callback);

    void downloadTorrent(String session, int friendConnection, int channelId, String torrentId,
            int lengthHint, AsyncCallback<Integer> callback);

    void addDownloadFromLocalTorrentDefaultSaveLocation(String session, String inPathToTorrent,
            ArrayList<PermissionsGroup> inPermissions, AsyncCallback<Void> callback);

    // void addDownloadFromLocalTorrent(String session, String path,
    // String savePath, boolean skipCheck, ArrayList<PermissionsGroup>
    // inPermissions,
    // AsyncCallback<Void> callback);

    void getTorrentDownloadProgress(String session, int torrentDownloadID,
            AsyncCallback<Integer> callback);

    void getTorrentFiles(String session, int torrentDownloadID,
            AsyncCallback<FileListLite[]> callback);

    void getTorrentName(String session, int inID, AsyncCallback<String> callback);

    void addTorrent(String session, int torrentDownloadID, FileListLite[] selectedFiles,
            ArrayList<PermissionsGroup> inPerms, String path, boolean noStream,
            AsyncCallback<Boolean> callback);

    void torrentExists(String session, String torrentID, AsyncCallback<Boolean> callback);

    void deleteData(String session, String[] torrentID, AsyncCallback<Boolean> callback);

    void deleteFromShareKeepData(String session, String[] torrentID,
            AsyncCallback<ReportableException> callback);

    void deleteCompletely(String session, String[] torrentID,
            AsyncCallback<ReportableException> callback);

    void addFriend(String session, FriendInfoLite friendInfoLite, boolean testOnly,
            AsyncCallback<Void> callback);

    void scanXMLForFriends(String session, String text, AsyncCallback<FriendInfoLite[]> callback);

    void applySwarmPermissionChanges(String session, ArrayList<TorrentInfo> inSwarms,
            AsyncCallback<Void> callback);

    void getFriends(String session, int prevListId, boolean includeDisconnected,
            boolean includeBlocked, AsyncCallback<FriendList> callback);

    void getMyPublicKey(String session, AsyncCallback<String> callback);

    void getFileList(String session, int connectionId, String filter, int startNum, int num,
            long maxCacheAge, AsyncCallback<FileListLite[]> callback);

    void sendSearch(String session, String searchString, AsyncCallback<Integer> callback);

    void revealSwarmInFinder(String session, TorrentInfo[] inSwarm,
            AsyncCallback<ReportableException> callback);

    // void revealPathInFinder(String session, String path, AsyncCallback
    // callback);

    void openFileDefaultApp(String session, TorrentInfo[] inSwarm,
            AsyncCallback<ReportableException> callback);

    void getSearchResult(String session, int searchId,
            AsyncCallback<TextSearchResultLite[]> callback);

    void getFiles(String session, String path, AsyncCallback callback);

    void getFriendTransferStats(String session,
            AsyncCallback<ArrayList<HashMap<String, String>>> callback);

    void setFriendsSettings(String session, FriendInfoLite[] updated, AsyncCallback<Void> callback);

    void getPendingCommunityFriendImports(String session, AsyncCallback<FriendList> callback);

    void getNewUsersFromXMPP(String session, String xmppNetworkName, String username,
            char[] password, String machineName, AsyncCallback<FriendInfoLite[]> callback);

    void pollCommunityServer(String session, CommunityRecord record, AsyncCallback<Integer> callback);

    void getTorrentsState(String session, AsyncCallback<HashMap<String, Integer>> callback);

    void getComputerName(String session, AsyncCallback<String> callback);

    void setComputerName(String session, String computerName, AsyncCallback<Void> callback);

    void pagedTorrentStateRefresh(String session, ArrayList<String> whichOnes,
            AsyncCallback<TorrentInfo[]> callback);

    void getIntegerParameterValue(String session, String inParamName,
            AsyncCallback<Integer> callback);

    void setIntegerParameterValue(String session, String inParamName, Integer inValue,
            AsyncCallback<Void> callback);

    void getBooleanParameterValue(String session, String inParamName,
            AsyncCallback<Boolean> callback);

    void setBooleanParameterValue(String session, String inParamName, Boolean inValue,
            AsyncCallback<Void> callback);

    void getStringParameterValue(String session, String inParamName, AsyncCallback<String> callback);

    void setStringParameterValue(String session, String inParamName, String inValue,
            AsyncCallback<Void> callback);

    void getStringListParameterValue(String session, String inParamName,
            AsyncCallback<ArrayList<String>> callback);

    void getCommunityServers(String session, AsyncCallback<List<CommunityRecord>> callback);

    void setStringListParameterValue(String session, String inParamName, ArrayList<String> value,
            AsyncCallback<Void> callback);

    void getDownloadManagersCount(String session, AsyncCallback<Integer> callback);

    void getPagedAndFilteredSwarms(int inPage, int swarmsPerPage, String filter, int sort,
            String type, boolean includeF2F, int inSelectedFriendID, String inTagPath,
            AsyncCallback<PagedTorrentInfo> callback);

    void getFilesForDownloadingTorrentHash(String session, String inOneSwarmHash,
            AsyncCallback<FileListLite[]> callback);

    void updateSkippedFiles(String session, FileListLite[] lites,
            AsyncCallback<ReportableException> callback);

    void getAllGroups(String session, AsyncCallback<ArrayList<PermissionsGroup>> callback);

    void getFriendsForGroup(String session, PermissionsGroup inGroup,
            AsyncCallback<ArrayList<FriendInfoLite>> callback);

    void getGroupsForSwarm(String session, TorrentInfo inSwarm,
            AsyncCallback<ArrayList<PermissionsGroup>> callback);

    void setGroupsForSwarm(String session, TorrentInfo inSwarm,
            ArrayList<PermissionsGroup> inGroups, AsyncCallback<ReportableException> callback);

    void updateGroupMembership(String session, PermissionsGroup inGroup,
            ArrayList<FriendInfoLite> inMembers, AsyncCallback<PermissionsGroup> callback);

    void removeGroup(String session, Long inGroupID, AsyncCallback<ReportableException> callback);

    void connectToFriends(String session, FriendInfoLite[] friendLite, AsyncCallback<Void> callback);

    void getUpdatedFriendInfo(String session, FriendInfoLite friendLite,
            AsyncCallback<FriendInfoLite> callback);

    void getBackendTasks(String session, AsyncCallback<BackendTask[]> callback);

    void getBackendTask(String session, int inID, AsyncCallback<BackendTask> callback);

    void cancelBackendTask(String session, int inID, AsyncCallback<Void> callback);

    void debug(String session, String which, AsyncCallback<String> callback);

    void getLanOneSwarmUsers(String session, AsyncCallback<FriendInfoLite[]> callback);

    void getRemoteAccessUserName(String session, AsyncCallback<String> callback);

    void saveRemoteAccessCredentials(String session, String username, String password,
            AsyncCallback<String> callback);

    void getListenAddresses(String session, AsyncCallback<String[]> asyncCallback);

    void getNewFriendsCountsFromAutoCheck(String session,
            AsyncCallback<HashMap<String, Integer>> callback);

    void getDeniedIncomingConnections(String session,
            AsyncCallback<HashMap<String, String>> callback);

    void getPlatform(String session, AsyncCallback<String> callback);

    void deleteFriends(String session, FriendInfoLite[] friend, AsyncCallback<Void> callback);

    void addToIgnoreRequestList(String session, FriendInfoLite friend, AsyncCallback<Void> callback);

    void getGtalkStatus(String session, AsyncCallback<String> callback);

    void getAllTags(String session, AsyncCallback<FileTree> callback);

    void getTags(String session, String inOneSwarmHash, AsyncCallback<FileTree> callback);

    void setTags(String session, String inOneSwarmHash, String[] path, AsyncCallback<Void> callback);

    void getSelf(String session, AsyncCallback<FriendInfoLite> callback);

    void getUsersWithMessages(String session, AsyncCallback<HashMap<String, String[]>> callback);

    void getUnreadMessageCounts(String session, AsyncCallback<HashMap<String, Integer>> callback);

    void getMessagesForUser(String session, String base64Key, boolean include_read, int limit,
            AsyncCallback<SerialChatMessage[]> callback);

    void sendChatMessage(String session, String base64Key, SerialChatMessage message,
            AsyncCallback<Boolean> callback);

    void clearChatLog(String session, String base64Key, AsyncCallback<Integer> callback);

    void updateRemoteAccessIpFilter(String session, String selectedFilterType, String filterString,
            AsyncCallback<Void> callback);

    void getBackendErrors(String session, AsyncCallback<ArrayList<BackendErrorReport>> callback);

    void getDebugMessageLog(String session, String friendKey, AsyncCallback<String> callback);

    void getBase64HashesForOneSwarmHashes(String session, String[] inOneSwarmHashes,
            AsyncCallback<String[]> callback);

    void getBase64HashesForBase32s(String session, String[] inBase32s,
            AsyncCallback<String[]> callback);

    void createInvitation(String session, String name, boolean canSeeFileList, long maxAge,
            SecurityLevel securityLevel, AsyncCallback<FriendInvitationLite> callback);

    void redeemInvitation(String session, FriendInvitationLite invitation, boolean testOnly,
            AsyncCallback<Void> callback);

    void getSentFriendInvitations(String session,
            AsyncCallback<ArrayList<FriendInvitationLite>> callback);

    void getRedeemedFriendInvitations(String session,
            AsyncCallback<ArrayList<FriendInvitationLite>> callback);

    void updateFriendInvitations(String sessionID, FriendInvitationLite invitation,
            AsyncCallback<Void> asyncCallback);

    void deleteFriendInvitations(String sessionID, ArrayList<FriendInvitationLite> invitations,
            AsyncCallback<Void> asyncCallback);

    void copyTorrentInfoToMagnetLink(String sessionID, String[] torrentIDs,
            AsyncCallback<String> asyncCallback);

    void refreshFileAssociations(String session, AsyncCallback<Void> callback);

    void getLocales(String session, AsyncCallback<LocaleLite[]> callback);

    void getFileInfo(String session, FileListLite file, boolean getMediaInfo,
            AsyncCallback<HashMap<String, String>> callback);

    void performSpeedCheck(String session, double setWithFraction,
            AsyncCallback<BackendTask> callback);

    void applyDefaultSettings(String session, AsyncCallback<Void> callback);

    void getNumberFriendsCount(String session, AsyncCallback<Integer> callback);

    void getNumberOnlineFriends(String session, AsyncCallback<Integer> callback);

    void publishSwarms(String session, TorrentInfo[] infos, String[] previewPaths,
            String[] comments, String[] categories, CommunityRecord toServer,
            AsyncCallback<BackendTask> callback);

    void getCategoriesForCommunityServer(String sessionID, CommunityRecord selected,
            AsyncCallback<ArrayList<String>> asyncCallback);

    void triggerNatCheck(String sessionID, AsyncCallback<Void> callback);

    void getNatCheckResult(String sessionID, AsyncCallback<HashMap<String, String>> callback);

    void fixPermissions(String sessionID, TorrentInfo torrent, boolean inFixAll,
            AsyncCallback<Void> asyncCallback);

    void isStreamingDownload(String session, String infohash, AsyncCallback<Boolean> callback);

    void setStreamingDownload(String session, String infohash, boolean streaming,
            AsyncCallback<Void> callback);

    void getMultiTorrentSourceTemp(String session, AsyncCallback<String> callback);

    void listFiles(String session, String string, AsyncCallback<FileInfo[]> callback);

    void getClientServices(String session, AsyncCallback<ClientServiceInfo[]> callback);

    void addClientService(String sessionID, long id, String name, AsyncCallback<Void> callback);

    void removeClientService(String session, long id, AsyncCallback<Void> callback);

    void activateClientService(String session, String name, long id, AsyncCallback<String> callback);

    void getNewServiceKey(AsyncCallback<String> asyncCallback);

    void setExitPolicy(String exitNodes, String localPort, int mode, AsyncCallback<String> callback);

    void getExitPolicy(AsyncCallback<List<String>> callback);

    void getNickname(AsyncCallback<String[]> callback);

    void setNickname(String nickname, Boolean published, Boolean enabled, AsyncCallback<Void> callback);
}