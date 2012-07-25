package edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.ImageConstants;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;

public class CommunityServersSidebarWidget extends SidebarWidgetList<CommunityRecord> {
    private static OSMessages msg = OneSwarmGWT.msg;

    public CommunityServersSidebarWidget() {
        super(msg.community_servers_sidebar_header());

        addFooterMenuItem(true, new MenuItem(msg.community_servers_sidebar_add(), new Command() {
            @Override
            public void execute() {
                OneSwarmDialogBox dlg = new FriendsImportWizard(
                        FriendsImportWizard.FRIEND_SRC_COMMUNITY);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
            }
        }));
    }

    @Override
    protected void update() {
        OneSwarmRPCClient.getService().getCommunityServers(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<List<CommunityRecord>>() {
                    @Override
                    public void onSuccess(List<CommunityRecord> results) {
                        List<SidebarItem> contents = new LinkedList<SidebarItem>();
                        for (CommunityRecord result : results) {
                            contents.add(new CommunityServerPanel(result));
                        }
                        setContent(contents);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        OneSwarmGWT.log(caught.toString());
                    }
                });
    }

    class CommunityServerPanel extends SidebarItem {
        private final static int TOTAL_WIDTH = 170;
        private final static int STATUS_IMAGE_WIDTH = 12;
        private final static int NAME_LABEL_WIDTH = TOTAL_WIDTH - STATUS_IMAGE_WIDTH - 4;

        public CommunityServerPanel(CommunityRecord item) {
            super(item);
        }

        @Override
        public FocusPanel asFocusPanel() {
            FocusPanel fp = new FocusPanel();

            HorizontalPanel mainPanel = new HorizontalPanel();
            mainPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            mainPanel.setSpacing(2);

            Image statusImage = new Image(
                    item.getSplash_path() != null ? ImageConstants.ICON_FRIEND_ONLINE
                            : ImageConstants.ICON_FRIEND_LIMITED);

            String name = item.getServer_name() == null ? item.getUrl() : item.getServer_name();
            Label nameLabel = new Label(name);
            nameLabel.setTitle(item.getServer_name());
            nameLabel.setWidth(NAME_LABEL_WIDTH + "px");

            mainPanel.add(statusImage);
            mainPanel.add(nameLabel);
            fp.add(mainPanel);

            fp.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if (item.getSplash_path() == null) {
                        Window.alert(msg.community_servers_sidebar_no_files());
                        return;
                    }
                    History.newItem("cserver-" + item.getBaseURL().hashCode());
                }
            });

            fp.addDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    Window.open(item.getBaseURL(), "_blank", null);
                }
            });

            return fp;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof CommunityServerPanel) {
                return item.getUrl().equals(((CommunityServerPanel) other).item.getUrl());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.item.getUrl().hashCode();
        }

        @Override
        public boolean hasUpdates(SidebarItem oldItem) {
            return !this.item.getServer_name().equals(oldItem.item.getServer_name());
        }
    }
}
