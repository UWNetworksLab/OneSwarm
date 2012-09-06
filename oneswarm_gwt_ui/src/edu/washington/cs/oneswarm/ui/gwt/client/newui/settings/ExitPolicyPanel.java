package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;

public class ExitPolicyPanel extends SettingsPanel implements ClickHandler, ChangeHandler {
    public static final String EVERYTHING = "accept *:*";
    public static final String SAFE = "accept facebook.com:*\n" +
            "accept *.facebook.com:*\n" +
            "accept google.com:*\n" +
            "accept *.google.com:*\n" +
            "accept youtube.com:*\n" + 
            "accept yahoo.com:*\n" +
            "accept baidu.com:*\n" + 
            "accept wikipedia.org:*\n" +
            "accept *.wikipedia.org:*\n" +
            "accept live.com:*\n" + 
            "accept qq.com:*\n" +
            "accept twitter.com:*\n" +
            "accept amazon.com:*\n" +
            "accept *.blogspot.com:*\n" + 
            "accept taobao.com:*\n" +
            "accept linkedin.com:*\n" +
            "accept yahoo.co.jp:*\n" + 
            "accept sina.com.cn:*\n" +
            "accept msn.com:*\n" +
            "accept yandex.ru:*\n" +
            "accept babylon.com:*\n" + 
            "accept bing.com:*\n" +
            "accept *.wordpress.com:*\n" +
            "accept ebay.com:*\n" + // top 25 alexa.
            "accept t.co:*\n" +
            "accept bbc.co.uk:*\n" +
            "accept mail.ru:*\n" +
            "accept blogger.com:*\n" +
            "accept gmail.com:*\n" +
            "accept googleusercontent.com:*";
    
    RadioButton everythingButton = new RadioButton("group",
            msg.settings_exitpolicy_policy_everything());
    RadioButton localButton = new RadioButton("group", msg.settings_exitpolicy_policy_local());
    RadioButton safeButton = new RadioButton("group", msg.settings_exitpolicy_policy_safe());
    RadioButton customButton = new RadioButton("group", msg.settings_exitpolicy_policy_custom());
    TextArea policy = new TextArea();
    TextBox localPort = new TextBox();

    public ExitPolicyPanel() {

        HorizontalPanel splitView = new HorizontalPanel();

        VerticalPanel modes = new VerticalPanel();

        everythingButton.addClickHandler(this);
        localButton.addClickHandler(this);
        safeButton.addClickHandler(this);
        customButton.addClickHandler(this);

        modes.add(everythingButton);
        modes.add(safeButton);
        modes.add(customButton);
        modes.add(localButton);
        HorizontalPanel portLine = new HorizontalPanel();
        portLine.add(localPort);
        localPort.setWidth("54px");
        portLine.add(new Label(msg.settings_exitpolicy_local_port()));
        modes.add(portLine);

        splitView.add(modes);
        
        policy.addChangeHandler(this);
        policy.setSize("300px", "300px");
        splitView.add(policy);

        OneSwarmRPCClient.getService().getExitPolicy(new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
            }

            @Override
            public void onSuccess(List<String> result) {
                localButton.setValue(false);
                everythingButton.setValue(false);
                safeButton.setValue(false);
                customButton.setValue(false);
                
                if (result.get(0).equals("local")) {
                    localButton.setValue(true);
                } else {
                    if (result.get(1).equals(EVERYTHING)) {
                        everythingButton.setValue(true);
                    } else if (result.get(1).equals(SAFE)) {
                        safeButton.setValue(true);
                    } else {
                        customButton.setValue(true);
                    }
                }
                policy.setText(result.get(1));
                localPort.setText(result.get(2));

                loadNotify();
            }
        });

        super.add(new Label(msg.settings_exitpolicy_header()));
        super.add(splitView);
    }

    @Override
    public void sync() {
        this.ready_save = false;
        OneSwarmRPCClient.getService().setExitPolicy(policy.getText(), localPort.getText(), this.getMode(),
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                        loadNotify();
                    }

                    @Override
                    public void onSuccess(String result) {
                        if (!result.equals("")) {
                            Window.alert(result);
                        }
                        loadNotify();
                    }
                });
    }

    @Override
    public String validData() {
        return null;
    }
    
    public int getMode() {
        if (this.everythingButton.getValue()) {
            return 1;
        } else if (this.safeButton.getValue()) {
            return 2;
        } else if (this.localButton.getValue()) {
            return 3;
        } else {
            return 4;
        }
    }
    
    @Override
    public void onClick(ClickEvent event) {
        Object sender = event.getSource();
        if (sender.equals(localButton)) {
            policy.setText("");
            policy.setEnabled(false);
            localPort.setEnabled(true);
        } else {
            if (sender.equals(safeButton)) {
                policy.setText(SAFE);
            } else if (sender.equals(everythingButton)) {
                policy.setText(EVERYTHING);
            }
            policy.setEnabled(true);
            localPort.setEnabled(false);
        }
    }

    @Override
    public void onChange(ChangeEvent event) {
        if (safeButton.getValue() && !policy.getValue().equals(SAFE)) {
            this.safeButton.setValue(false);
            this.customButton.setValue(true);
        } else if (everythingButton.getValue() && !policy.getValue().equals(EVERYTHING)) {
            this.everythingButton.setValue(false);
            this.customButton.setValue(true);
        }
    }

    public void setEnabled(boolean enabled) {
        this.customButton.setEnabled(enabled);
        this.everythingButton.setEnabled(enabled);
        this.safeButton.setEnabled(enabled);
        this.localButton.setEnabled(enabled);
        this.policy.setEnabled(enabled);
        this.localPort.setEnabled(enabled);
        if (enabled && this.localButton.getValue()) {
            this.policy.setEnabled(false);
        }
        if (enabled && !this.localButton.getValue()) {
            this.localPort.setEnabled(false);
        }
    }
}
