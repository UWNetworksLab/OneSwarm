package edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.TextBox;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportCallback;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.InvitationCreatePanel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

public class InviteFriendSidebarWidget extends SidebarWidget {
    private static OSMessages msg = OneSwarmGWT.msg;
    private final TextBox emailBox;

    public InviteFriendSidebarWidget() {
        super(msg.friends_sidebar_invitation_header());

        addFooterMenuItem(true, new MenuItem(msg.friends_sidebar_invitation_button(),
                new Command() {
                    @Override
                    public void execute() {
                        sendIt();
                    }
                }));

        emailBox = new TextBox();
        emailBox.setWidth("158px");
        emailBox.setText(msg.friends_sidebar_invitation_msg());
        emailBox.addFocusHandler(new FocusHandler() {
            @Override
            public void onFocus(FocusEvent event) {
                if (emailBox.getText().equals(msg.friends_sidebar_invitation_msg())) {
                    DOM.setStyleAttribute(emailBox.getElement(), "color", "grey");
                    emailBox.setText("");
                }
            }
        });
        emailBox.addBlurHandler(new BlurHandler() {
            @Override
            public void onBlur(BlurEvent event) {
                if (emailBox.getText().equals("")) {
                    DOM.setStyleAttribute(emailBox.getElement(), "color", "grey");
                    emailBox.setText(msg.friends_sidebar_invitation_msg());
                }
            }
        });
        emailBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    sendIt();
                }
            }
        });

        widgetBody.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        setContent(emailBox);

        // Dont update since this is a static widget.
        super.setUpdateInterval(0);
    }

    @Override
    protected void update() {
    }

    private void sendIt() {
        if (emailBox.getText().length() > 0) {
            final OneSwarmDialogBox dlg = new OneSwarmDialogBox();
            dlg.setWidth(FriendsImportWizard.WIDTH + "px");
            dlg.setWidget(new InvitationCreatePanel(emailBox.getText(),
                    new FriendsImportCallback() {
                        @Override
                        public void back() {
                            dlg.hide();
                        }

                        @Override
                        public void cancel() {
                            dlg.hide();
                        }

                        @Override
                        public void connectSuccesful(FriendInfoLite[] changes, boolean showSkip) {
                            dlg.hide();
                        }
                    }));
            dlg.setText(msg.friends_sidebar_invitation_header());
            dlg.center();
            emailBox.setText("");
        }
    }
}
