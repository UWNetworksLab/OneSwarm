package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar.ClientServicesSidebarWidget;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar.CommunityServersSidebarWidget;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar.FriendListSidebarWidget;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar.InviteFriendSidebarWidget;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar.SidebarWidgetList;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class NavigationSidePanel extends VerticalPanel implements Updateable {
    private static OSMessages msg = OneSwarmGWT.msg;

    public static final String HYPERLINK_LABEL_TRANSFERS = "hist-transfers";
    public static final String HYPERLINK_LABEL_FRIENDS = "friends-panel";

    RoundedPanel mSelectedRP = null;
    HorizontalPanel mSelectedHP = null;

    NavigationSidePanel this_shadow = this;

    final Label upRateLabel = new Label("");
    final Label downRateLabel = new Label("");
    final Label remoteRateLabel = new Label("");
    final HTML unreadChatHTML = new HTML("");

    private long mNextUpdate = 0;

    private final FriendListSidebarWidget onlineFriendWidget;
    private final CommunityServersSidebarWidget communityServersWidget;

    private final List<SidebarWidgetList<?>> clearList;

    public NavigationSidePanel() {
        clearList = new LinkedList<SidebarWidgetList<?>>();
        addStyleName(OneSwarmCss.SidebarBase.MAIN_VERTICAL_PANEL);

        unreadChatHTML.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                EntireUIRoot.getRoot(NavigationSidePanel.this).startChat(null);
            }
        });
        unreadChatHTML.getElement().setId("unreadChatNotification");
        unreadChatHTML.addStyleName(OneSwarmCss.SidebarBase.CHAT_NOTIFICATION);

        upRateLabel.addStyleName(OneSwarmCss.SidebarBase.STATS_TEXT);
        downRateLabel.addStyleName(OneSwarmCss.SidebarBase.STATS_TEXT);
        remoteRateLabel.addStyleName(OneSwarmCss.SidebarBase.STATS_TEXT);
        this.setHorizontalAlignment(ALIGN_RIGHT);

        for (FileTypeFilter filter : FileTypeFilter.values()) {
            Panel rp = createCategoryPanel(filter.getUiLabel(), filter.history_state_name,
                    filter.sidebar_icon_path);
            add(rp);
        }
        add(createCategoryPanel(msg.navigation_transfers(), HYPERLINK_LABEL_TRANSFERS,
                ImageConstants.ICON_TRANSFERS));
        add(createCategoryPanel(msg.navigation_friends(), HYPERLINK_LABEL_FRIENDS,
                ImageConstants.ICON_FRIENDS));
        add(upRateLabel);
        add(downRateLabel);
        add(remoteRateLabel);

        remoteRateLabel.setVisible(false);

        communityServersWidget = new CommunityServersSidebarWidget();
        onlineFriendWidget = new FriendListSidebarWidget();

        add(communityServersWidget);
        add(onlineFriendWidget);
        add(new InviteFriendSidebarWidget());
        add(new ClientServicesSidebarWidget());
    }

    @Override
    public void add(IsWidget w) {
        if (w instanceof SidebarWidgetList) {
            clearList.add((SidebarWidgetList<?>) w);
        }
        super.add(w);
        setCellHorizontalAlignment(w, ALIGN_LEFT);
    }

    public FriendListSidebarWidget getFriendPanel() {
        return onlineFriendWidget;
    }

    public CommunityServersSidebarWidget getCommunityServersPanel() {
        return communityServersWidget;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        OneSwarmGWT.removeFromUpdateTask(this);
    }

    @Override
    public void onAttach() {
        super.onAttach();
        OneSwarmGWT.addToUpdateTask(this);
    }

    public void clearSelection() {
        if (mSelectedRP != null) {
            mSelectedRP.removeStyleName(OneSwarmCss.SidebarBase.SELECTED_ITEM);
        }

        for (SidebarWidgetList<?> sp : clearList) {
            sp.clearSelected();
        }
    }

    private Panel createCategoryPanel(String ui_label, String history_state_name, String icon_path) {
        final Hyperlink link = new Hyperlink(ui_label, history_state_name);
        link.setStyleName(OneSwarmCss.SidebarBase.LINK);
        final Image movieIcon = new Image(icon_path);
        movieIcon.setVisible(true);
        movieIcon.addLoadHandler(new LoadHandler() {
            @Override
            public void onLoad(LoadEvent event) {
                movieIcon.setSize("32px", "32px");
                movieIcon.setVisible(true);
            }
        });

        final HorizontalPanel ico_label = new HorizontalPanel();
        ico_label.setWidth("100%");
        ico_label.add(movieIcon);
        ico_label.setCellWidth(movieIcon, 40 + "px");
        ico_label.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
        ico_label.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        ico_label.setSpacing(0);
        ico_label.add(link);

        final RoundedPanel rp = new RoundedPanel(ico_label, RoundedPanel.LEFT, 2);

        if (ui_label.equals(FileTypeFilter.All.getUiLabel())) {
            // ico_label.setStyleName(OneSwarmCss.SidebarBase.NAV_LINK_SELECTED);
            rp.setStyleName(OneSwarmCss.SidebarBase.SELECTED_ITEM);
            mSelectedRP = rp;
            mSelectedHP = ico_label;
        } else {
            ico_label.setStyleName(OneSwarmCss.SidebarBase.LINK);
        }

        /**
         * We deal with changing the file browsing pane from
         * EntireUIRoot::onHistoryChanged, here we simply update the side panel
         * UI to reflect the current selection
         */
        ClickHandler navClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                EntireUIRoot.getRoot(NavigationSidePanel.this).clearSidebarSelection();
                mSelectedHP.setStyleName(OneSwarmCss.SidebarBase.LINK);

                rp.addStyleName(OneSwarmCss.SidebarBase.SELECTED_ITEM);

                mSelectedRP = rp;
                mSelectedHP = ico_label;

                EntireUIRoot root = EntireUIRoot.getRoot(NavigationSidePanel.this);

                /*
                 * remove any friend selection if there is any
                 */
                // if( root.getSelectedFriend() != null )
                {
                    root.pageZero();
                }

                // TODO: we messed up adding history listeners early on, so we
                // need to invoke this manually. fix at some point
                // ((EntireUIRoot)
                // this_shadow.getParent()).onHistoryChanged(link.getTargetHistoryToken());

                History.newItem(link.getTargetHistoryToken());

            } // onClick()
        };
        link.addClickHandler(navClickHandler);
        movieIcon.addClickHandler(navClickHandler);

        return rp;

    }

    @Override
    public void update(int count) {
        if (mNextUpdate < System.currentTimeMillis()) {
            mNextUpdate = Long.MAX_VALUE;

            OneSwarmRPCClient.getService().getSidebarStats(OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<HashMap<String, String>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();

                            mNextUpdate = System.currentTimeMillis() + 5000;
                        }

                        @Override
                        public void onSuccess(HashMap<String, String> result) {
                            if (result != null) {
                                upRateLabel.setText(msg.sidebar_speed_upload()
                                        + " "
                                        + StringTools.formatRate(result
                                                .get(Strings.SIDEBAR_UL_RATE)) + "ps");
                                downRateLabel.setText(msg.sidebar_speed_download()
                                        + " "
                                        + StringTools.formatRate(result
                                                .get(Strings.SIDEBAR_DL_RATE)) + "ps");

                                if (result.get(Strings.SIDEBAR_REMOTE).equals("0")) {
                                    remoteRateLabel.setText("");
                                    remoteRateLabel.setVisible(false);
                                } else {
                                    remoteRateLabel.setText("Remote Access: "
                                            + StringTools.formatRate(result
                                                    .get(Strings.SIDEBAR_REMOTE)) + "ps");
                                    remoteRateLabel.setTitle("Connected from: "
                                            + result.get(Strings.SIDEBAR_REMOTE_IPS));
                                    remoteRateLabel.setVisible(true);
                                }
                            }
                            mNextUpdate = System.currentTimeMillis() + 1000;
                        }
                    });
        }
    }

    public void setUnreadChatCount(int total) {
        if (ChatDialog.showing()) {
            total = 0;
        }

        if (unreadChatHTML.isAttached() && total == 0) {
            remove(unreadChatHTML);
            return;
        } else if (total == 0) {
            return;
        }

        unreadChatHTML.setHTML("<a href=\"#\">" + total + " unread message"
                + (total > 1 ? "s" : "") + "</a>");

        if (unreadChatHTML.isAttached() == false && total > 0) {
            insert(unreadChatHTML, getWidgetIndex(onlineFriendWidget));
        }

    }
}
