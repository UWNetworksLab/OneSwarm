package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar.FriendListSidebarWidget;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

public class FriendPropertiesDialog extends OneSwarmDialogBox {
    private final FriendPropertiesPanel mainPanel;
    private final FriendListSidebarWidget parent;
    private final String initialGroup;

    public FriendPropertiesDialog(final FriendInfoLite friend, boolean useDebug) {
        this(null, friend, useDebug);
    }

    public FriendPropertiesDialog(FriendListSidebarWidget parent, final FriendInfoLite friend,
            boolean useDebug) {
        super();
        this.parent = parent;

        this.initialGroup = friend.getGroup();

        setText("Edit Friend: " + friend.getName());
        mainPanel = new FriendPropertiesPanel(friend, this, useDebug);
        setWidget(mainPanel);
    }

    public void saveFriend() {
        if (mainPanel != null) {
            mainPanel.saveChanges(null, true, false);
        }
    }

    @Override
    public void hide() {
        mainPanel.stopUpdates();
        super.hide();
        if (parent != null) {
            boolean force = !initialGroup.equals(mainPanel.getGroup());
            parent.update(force ? 0 : 1);
        }
    }
}
