package edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar;

import java.util.Collection;
import java.util.LinkedList;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.ImageConstants;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ClientServiceInfo;

public class ClientServicesSidebarWidget extends SidebarWidgetList<ClientServiceInfo> {
    private static OSMessages msg = OneSwarmGWT.msg;

    public ClientServicesSidebarWidget() {
        super(msg.client_services_widget_title());

        addFooterMenuItem(true, new MenuItem(msg.client_services_add_link(), new Command() {
            @Override
            public void execute() {
                final OneSwarmDialogBox dlg = new AddClientServiceWizard();
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
            }
        }));
    }

    @Override
    protected void update() {
        OneSwarmRPCClient.getService().getClientServices(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<ClientServiceInfo[]>() {
                    @Override
                    public void onSuccess(ClientServiceInfo[] results) {
                        Collection<SidebarItem> contents = new LinkedList<SidebarItem>();
                        for (ClientServiceInfo result : results) {
                            ClientServicePanel pan = new ClientServicePanel(result);
                            contents.add(pan);
                        }
                        setContent(contents);
                        setClosedHeaderTextSuffix("(" + results.length + ")");
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        OneSwarmGWT.log(caught.toString());
                    }
                });
    }

    class ClientServicePanel extends SidebarItem {
        private final static int NAME_LABEL_WIDTH = 150;

        public ClientServicePanel(ClientServiceInfo item) {
            super(item);
        }

        @Override
        public FocusPanel asFocusPanel() {
            FocusPanel fp = new FocusPanel();
            HorizontalPanel mainPanel = new HorizontalPanel();
            mainPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            mainPanel.setSpacing(2);

            // Label with short version of Service Name.
            Label nameLabel = new Label(item.serviceName);
            nameLabel.setTitle(item.serviceName);
            nameLabel.setWidth(NAME_LABEL_WIDTH + "px");

            Image closeButton = new Image(ImageConstants.ICON_CLOSE_BUTTON);
            closeButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    OneSwarmRPCClient.getService().removeClientService(
                            OneSwarmRPCClient.getSessionID(), item.serviceID,
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    OneSwarmGWT.log(caught.toString());
                                }

                                @Override
                                public void onSuccess(Void result) {
                                }
                            });
                }
            });

            mainPanel.add(nameLabel);
            mainPanel.add(closeButton);
            fp.add(mainPanel);

            fp.addDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    OneSwarmRPCClient.getService().activateClientService(
                            OneSwarmRPCClient.getSessionID(), item.serviceName, item.serviceID,
                            new AsyncCallback<String>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    OneSwarmGWT.log(caught.toString());
                                }

                                @Override
                                public void onSuccess(String url) {
                                    Window.open(url, "_null", "");
                                }
                            });
                }
            });

            return fp;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ClientServicePanel) {
                return item.serviceID == ((ClientServicePanel) other).item.serviceID;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (int) item.serviceID;
        }

        @Override
        public boolean hasUpdates(SidebarItem oldItem) {
            return !this.item.serviceName.equals(oldItem.item.serviceName);
        }
    }

    static class AddClientServiceWizard extends OneSwarmDialogBox {
        private static final int WIDTH = 400;

        public AddClientServiceWizard() {
            super(false, true, true);
            super.setText(msg.client_services_dialog_title());
            VerticalPanel p = new VerticalPanel();

            Label selectLabel = new Label(msg.client_services_dialog_hint());
            selectLabel.addStyleName(OneSwarmCss.Dialog.HEADER);
            selectLabel.setWidth(WIDTH + "px");
            p.add(selectLabel);
            p.setCellVerticalAlignment(selectLabel, VerticalPanel.ALIGN_TOP);
            p.setWidth("100%");

            final TextArea serviceId = new TextArea();
            p.add(new InputPanel(WIDTH - 10, msg.client_services_dialog_service_id(), serviceId));
            final TextBox serviceName = new TextBox();
            p.add(new InputPanel(WIDTH - 10, msg.client_services_dialog_service_name(), serviceName));

            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setSpacing(5);

            Button cancelButton = new Button(msg.button_cancel());
            buttonPanel.add(cancelButton);
            cancelButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    hide();
                }
            });

            Button addButton = new Button(msg.button_add());
            addButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    boolean valid = true;

                    long id = 0l;
                    try {
                        id = Long.parseLong(serviceId.getText());
                        if (serviceId.getStyleName().contains(OneSwarmCss.INVALID_FORM_ENTRY)) {
                            serviceId.removeStyleName(OneSwarmCss.INVALID_FORM_ENTRY);
                        }
                    } catch (Exception e) {
                        serviceId.addStyleName(OneSwarmCss.INVALID_FORM_ENTRY);
                        valid = false;
                    }

                    String name = serviceName.getText();
                    if (name == null || name == "") {
                        serviceName.addStyleName(OneSwarmCss.INVALID_FORM_ENTRY);
                        valid = false;
                    } else if (serviceName.getStyleName().contains(OneSwarmCss.INVALID_FORM_ENTRY)) {
                        serviceName.removeStyleName(OneSwarmCss.INVALID_FORM_ENTRY);
                    }

                    if (valid) {
                        OneSwarmRPCClient.getService().addClientService(
                                OneSwarmRPCClient.getSessionID(), id, name,
                                new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        OneSwarmGWT.log(caught.toString());
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        hide();
                                    }
                                });
                    }
                }
            });
            buttonPanel.add(addButton);
            p.add(buttonPanel);
            p.setCellHorizontalAlignment(buttonPanel, HorizontalPanel.ALIGN_RIGHT);
            super.setWidget(p);
        }

        private class InputPanel extends HorizontalPanel {
            private final int LABEL_WIDTH = 80;

            InputPanel(int width, String labelText, Widget input) {
                super();
                Label label = new Label(labelText);
                label.setWidth(LABEL_WIDTH + "px");
                this.add(label);
                input.setWidth(width - LABEL_WIDTH + "px");
                this.add(input);
                this.setWidth(width + "px");
            }
        }
    }
}
