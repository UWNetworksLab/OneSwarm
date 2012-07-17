package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendListPanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;

public class CommunityServersSidePanel extends VerticalPanel implements Updateable, SidebarWidget {

    private static OSMessages msg = OneSwarmGWT.msg;

    ServerPanel mSelectedServer = null;

    private final VerticalPanel serverListVP = new VerticalPanel();
    private final DisclosurePanel disclosurePanel = new DisclosurePanel(
            msg.community_servers_sidebar_header());

    public CommunityServersSidePanel() {

        VerticalPanel contentPanel = new VerticalPanel();
        // add the panel that will contain the friends
        serverListVP.setWidth("100%");

        disclosurePanel.setOpen(true);
        disclosurePanel.addStyleName(OneSwarmCss.SidebarWidget.MAIN_PANEL);

        MenuBar footerMenu = new MenuBar();
        footerMenu.addStyleName(OneSwarmCss.SidebarWidget.FOOTER_MENU_BAR);
        footerMenu.setWidth("100%");
        MenuItem addFriendItem = new MenuItem(msg.community_servers_sidebar_add(), new Command() {
            public void execute() {
                OneSwarmDialogBox dlg = new FriendsImportWizard(
                        FriendsImportWizard.FRIEND_SRC_COMMUNITY);
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
                dlg.setVisible(true);
            }
        });

        addFriendItem.setStylePrimaryName(OneSwarmCss.SidebarWidget.FOOTER_MENU_ITEM);
        footerMenu.addItem(addFriendItem);
        addFriendItem.getElement().setId("addFriendItemLink");

        contentPanel.add(serverListVP);
        contentPanel.add(footerMenu);
        contentPanel.setCellHorizontalAlignment(footerMenu, HorizontalPanel.ALIGN_CENTER);

        disclosurePanel.add(contentPanel);

        this.add(disclosurePanel);

        OneSwarmGWT.addToUpdateTask(this);
    }

    long nextUpdateRPC = 0;

    public void update(int count) {
        if (System.currentTimeMillis() > nextUpdateRPC) {

            OneSwarmRPCClient.getService().getStringListParameterValue(
                    OneSwarmRPCClient.getSessionID(), "oneswarm.community.servers",
                    new AsyncCallback<ArrayList<String>>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(ArrayList<String> result) {
                            nextUpdateRPC = System.currentTimeMillis() + 5 * 1000;

                            if (result.size() / 5 == serverListVP.getWidgetCount()) {
                                // might need to update status
                                for (int i = 0; i < result.size() / 5; i++) {
                                    CommunityRecord rec = new CommunityRecord(result, i * 5);
                                    ((ServerPanel) serverListVP.getWidget(i)).update(rec);
                                }
                            } else {
                                serverListVP.clear();
                                for (int i = 0; i < result.size() / 5; i++) {
                                    CommunityRecord rec = new CommunityRecord(result, i * 5);
                                    serverListVP.add(new ServerPanel(rec));
                                }
                            }
                        }
                    });
            nextUpdateRPC = System.currentTimeMillis() + 10 * 1000;
        }
    }

    public void clearSelectedServer() {
        if (mSelectedServer != null) {
            mSelectedServer.clearSelected();
            mSelectedServer = null;
        }
    }

    public CommunityRecord getSelectedServer() {
        if (mSelectedServer == null) {
            return null;
        }
        return mSelectedServer.getRecord();
    }

    @Override
    public void clearSelection() {
        this.clearSelectedServer();
    }

    class ServerPanel extends FocusPanel {
        private static final int MAX_LABEL_NAME_LENGTH = 20;

        private boolean isSelected = false;

        private final ClickHandler clickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (!isSelected) {
                    if (server.getSplash_path() == null) {
                        Window.alert(msg.community_servers_sidebar_no_files());
                        return;
                    }
                    EntireUIRoot.getRoot(CommunityServersSidePanel.this).clearSidebarSelection();
                    setSelected();
                    History.newItem("cserver-" + server.getBaseURL().hashCode());
                }
            }
        };

        public final DoubleClickHandler doubleClickHandler = new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                Window.open(server.getBaseURL(), "_blank", null);
            }
        };

        private final HorizontalPanel mainPanel = new HorizontalPanel();
        private final Image statusImage = new Image(ImageConstants.ICON_FRIEND_LIMITED);
        private final HorizontalPanel imagePanel = new HorizontalPanel();
        private final AbsolutePanel labelPanel = new AbsolutePanel();
        private final Label nameLabel = new Label("");

        private CommunityRecord server;

        private final static int TOTAL_WIDTH = 170;
        private final static int STATUS_IMAGE = 12;
        private final static int NAME_LABEL_WIDTH = TOTAL_WIDTH - STATUS_IMAGE - 4;
        private final static int HEIGHT = 18;

        public ServerPanel(CommunityRecord server) {

            this.server = server;

            addStyleName(OneSwarmCss.CLICKABLE);

            mainPanel.setWidth(TOTAL_WIDTH + "px");

            statusImage.setHeight(STATUS_IMAGE + "px");
            statusImage.setWidth(STATUS_IMAGE + "px");
            imagePanel.add(statusImage);
            imagePanel.setHorizontalAlignment(ALIGN_CENTER);
            imagePanel.setWidth(STATUS_IMAGE + 2 + "px");

            mainPanel.add(imagePanel);
            mainPanel.setCellVerticalAlignment(imagePanel, HorizontalPanel.ALIGN_TOP);
            mainPanel.setCellHorizontalAlignment(imagePanel, HorizontalPanel.ALIGN_CENTER);

            labelPanel.setHeight(HEIGHT + "px");

            labelPanel.setWidth(NAME_LABEL_WIDTH + "px");

            nameLabel.setWidth(NAME_LABEL_WIDTH + "px");
            nameLabel.setHeight(14 + "px");
            nameLabel.setText("test");
            labelPanel.add(nameLabel, 2, 0);

            this.addClickHandler(clickHandler);
            this.addDoubleClickHandler(doubleClickHandler);

            mainPanel.add(labelPanel);

            super.add(mainPanel);

            refreshUI();
        }

        private void refreshUI() {
            String name = server.getServer_name() == null ? server.getUrl() : server
                    .getServer_name();
            String labelName;
            if (name.length() > MAX_LABEL_NAME_LENGTH) {
                labelName = name.substring(0, MAX_LABEL_NAME_LENGTH - 2) + "...";
            } else {
                labelName = name;
            }
            nameLabel.setText(labelName);
            nameLabel.setTitle(name);

            // System.out.println(statusImage.getUrl() + " / " +
            // server.getSplash_path());
            if (statusImage.getUrl().endsWith(ImageConstants.ICON_FRIEND_LIMITED)
                    && server.getSplash_path() != null) {
                statusImage.setUrl(ImageConstants.ICON_FRIEND_ONLINE);
            }
        }

        public void setSelected() {
            if (this != mSelectedServer) {
                ServerPanel.this.addStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
                mSelectedServer = this;
            }
        }

        public void clearSelected() {
            if (this == mSelectedServer) {
                ServerPanel.this.removeStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
                mSelectedServer = null;
            }
        }

        public CommunityRecord getRecord() {
            return server;
        }

        public void update(CommunityRecord po) {
            this.server = po;
            refreshUI();
        }
    }
}
