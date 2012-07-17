package edu.washington.cs.oneswarm.ui.gwt.client.newui.sharedservices;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.Command;
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
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.ImageConstants;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.SidebarWidget;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.sharedservices.AddSharedServiceWizard.AddSharedServiceCallback;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ClientServiceInfo;

public class SharedServicesSidePanel extends VerticalPanel implements SidebarWidget {

    private static OSMessages msg = OneSwarmGWT.msg;

    private final VerticalPanel serviceList = new VerticalPanel();
    private final DisclosurePanel disclosurePanel = new DisclosurePanel(
            msg.shared_services_widget_title());

    SharedServicePanel selectedService;

    public SharedServicesSidePanel() {
        VerticalPanel contentPanel = new VerticalPanel();

        serviceList.setWidth("100%");

        disclosurePanel.setOpen(true);
        disclosurePanel.addStyleName(OneSwarmCss.SidebarWidget.MAIN_PANEL);

        MenuBar footer = new MenuBar();
        footer.addStyleName(OneSwarmCss.SidebarWidget.FOOTER_MENU_BAR);
        footer.setWidth("100%");
        MenuItem addServiceLink = new MenuItem(msg.shared_services_add_link(), new Command() {
            public void execute() {
                final OneSwarmDialogBox dlg = new AddSharedServiceWizard(
                        new AddSharedServiceCallback() {
                            @Override
                            public void addSharedService(long id, String name) {
                                OneSwarmRPCClient.getService().addClientService(
                                        OneSwarmRPCClient.getSessionID(), id, name,
                                        new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                            }

                                            @Override
                                            public void onSuccess(Void result) {
                                                update(0);
                                            }
                                        });
                            }
                        });
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
            }
        });

        addServiceLink.setStylePrimaryName(OneSwarmCss.SidebarWidget.FOOTER_MENU_ITEM);
        footer.addItem(addServiceLink);

        contentPanel.add(serviceList);
        contentPanel.add(footer);
        contentPanel.setCellHorizontalAlignment(footer, ALIGN_CENTER);

        disclosurePanel.add(contentPanel);

        this.add(disclosurePanel);

        OneSwarmGWT.addToUpdateTask(this);
    }

    long nextUpdateRPC = 0;

    public void update(int thisValueIgnored) {
        if (System.currentTimeMillis() > nextUpdateRPC) {

            OneSwarmRPCClient.getService().getClientServices(OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<ClientServiceInfo[]>() {
                        public void onSuccess(ClientServiceInfo[] results) {
                            nextUpdateRPC = System.currentTimeMillis() + 5 * 1000;

                            while (serviceList.getWidgetCount() > results.length)
                                serviceList.remove(serviceList.getWidgetCount() - 1);

                            // TODO (nick) bug if the selected item is replaced
                            // in all sidebar widgets
                            for (int i = 0; i < results.length; i++)
                                if (i >= serviceList.getWidgetCount())
                                    serviceList.add(new SharedServicePanel(results[i]));
                                else if (((SharedServicePanel) serviceList.getWidget(i)).service.serviceID != results[i].serviceID) {
                                    serviceList.insert(new SharedServicePanel(results[i]), i);
                                    serviceList.remove(i + 1);
                                }
                        }

                        public void onFailure(Throwable caught) {
                            new ReportableErrorDialogBox(new OneSwarmException(caught),
                                    false).show();
                        }
                    });

            nextUpdateRPC = System.currentTimeMillis() + 10 * 1000;
        }
    }

    @Override
    public void clearSelection() {
        if (selectedService != null)
            selectedService.clearSelected();
    }

    class SharedServicePanel extends FocusPanel {
        private static final int MAX_LABEL_NAME_LENGTH = 19;
        private final static int TOTAL_WIDTH = 170;
        private final static int NAME_LABEL_WIDTH = TOTAL_WIDTH - 14;
        private final static int HEIGHT = 18;

        ClientServiceInfo service;

        public SharedServicePanel(final ClientServiceInfo service) {
            this.service = service;

            addStyleName(OneSwarmCss.CLICKABLE);

            HorizontalPanel mainPanel = new HorizontalPanel();
            mainPanel.setWidth(TOTAL_WIDTH + "px");

            AbsolutePanel labelPanel = new AbsolutePanel();
            labelPanel.setHeight(HEIGHT + "px");
            labelPanel.setWidth(NAME_LABEL_WIDTH + "px");

            Label nameLabel = new Label("");
            nameLabel.setWidth(NAME_LABEL_WIDTH + "px");
            nameLabel.setHeight(14 + "px");
            nameLabel
                    .setText(service.serviceName.length() > MAX_LABEL_NAME_LENGTH ? service.serviceName
                            .substring(0, MAX_LABEL_NAME_LENGTH - 3) + "..."
                            : service.serviceName);
            nameLabel.setTitle(service.serviceName);

            nameLabel.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    EntireUIRoot.getRoot(SharedServicesSidePanel.this).clearSidebarSelection();
                    setSelected();
                }
            });

            nameLabel.addDoubleClickHandler(new DoubleClickHandler() {
                public void onDoubleClick(DoubleClickEvent event) {
                    OneSwarmRPCClient.getService().activateClientService(
                            OneSwarmRPCClient.getSessionID(), service.serviceName,
                            service.serviceID, new AsyncCallback<String>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    new ReportableErrorDialogBox(new OneSwarmException(caught),
                                            false).show();
                                }

                                @Override
                                public void onSuccess(String url) {
                                    Window.open(url, "_null", "");
                                }
                            });
                }
            });

            Image closeButton = new Image(GWT.getModuleBaseURL() + ImageConstants.ICON_CLOSE_BUTTON);
            closeButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    OneSwarmRPCClient.getService().removeClientService(
                            OneSwarmRPCClient.getSessionID(), service.serviceID,
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    new ReportableErrorDialogBox(new OneSwarmException(caught),
                                            false).show();
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    update(0);
                                }
                            });
                }
            });

            labelPanel.add(nameLabel, 2, 0);
            mainPanel.setVerticalAlignment(ALIGN_MIDDLE);
            mainPanel.add(labelPanel);
            mainPanel.add(closeButton);
            super.add(mainPanel);
        }

        public void setSelected() {
            if (selectedService != this) {
                selectedService = this;
                SharedServicePanel.this.addStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
            }
        }

        public void clearSelected() {
            if (selectedService == this) {
                selectedService = null;
                SharedServicePanel.this.removeStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
            }
        }
    }
}
