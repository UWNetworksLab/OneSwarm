package edu.washington.cs.oneswarm.ui.gwt.client.newui.sharedservices;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;

public class AddSharedServiceWizard extends OneSwarmDialogBox {

    private static OSMessages msg = OneSwarmGWT.msg;
    public static final int WIDTH = 400;
    public static final int BWIDTH = 405;

    public AddSharedServiceWizard(final AddSharedServiceCallback callback) {
        super(false, true, true);
        super.setText(msg.shared_services_dialog_title());
        VerticalPanel p = new VerticalPanel();

        Label selectLabel = new Label(msg.shared_services_dialog_hint());
        selectLabel.addStyleName(OneSwarmCss.Dialog.HEADER);
        selectLabel.setWidth(WIDTH + "px");
        p.add(selectLabel);
        p.setCellVerticalAlignment(selectLabel, VerticalPanel.ALIGN_TOP);
        p.setWidth("100%");

        final TextArea serviceId = new TextArea();
        p.add(new InputPanel(WIDTH - 10, msg.shared_services_dialog_service_id(), serviceId));
        final TextBox serviceName = new TextBox();
        p.add(new InputPanel(WIDTH - 10, msg.shared_services_dialog_service_name(), serviceName));

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
                    if (serviceId.getStyleName().contains(OneSwarmCss.INVALID_FORM_ENTRY))
                        serviceId.removeStyleName(OneSwarmCss.INVALID_FORM_ENTRY);
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
                    callback.addSharedService(id, name);
                    hide();
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

    interface AddSharedServiceCallback {
        void addSharedService(long id, String name);
    }
}
