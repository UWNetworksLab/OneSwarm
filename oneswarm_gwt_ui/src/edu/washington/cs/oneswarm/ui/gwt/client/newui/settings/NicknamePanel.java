package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;

class NicknamePanel  extends SettingsPanel implements ClickHandler {
    CheckBox enabled_box = new CheckBox(msg.settings_exitpolicy_enabled_label());
    TextBox nickname_box = new TextBox();
    CheckBox published_box = new CheckBox(msg.settings_exitpolicy_published_label());
    
    ExitPolicyPanel policy = new ExitPolicyPanel();
    Label service_key = new Label();
    Button reset_button = new Button(msg.settings_exitpolicy_key_reset());
    
    public NicknamePanel() {
        super();

        VerticalPanel container = new VerticalPanel();
        container.add(enabled_box);
        enabled_box.addClickHandler(this);
        container.add(new com.google.gwt.user.client.ui.HTML("<hr />"));

        HorizontalPanel nickname = new HorizontalPanel();
        nickname.setVerticalAlignment(ALIGN_MIDDLE);
        nickname.add(new Label(msg.settings_exitpolicy_nickname_label()));
        nickname_box.setWidth("100px");
        nickname.add(nickname_box);
        nickname.add(published_box);
        
        container.add(nickname);
        container.add(policy);
        
        HorizontalPanel keyPanel = new HorizontalPanel();
        keyPanel.add(new Label(msg.settings_exitpolicy_key_label()));
        keyPanel.add(service_key);
        reset_button.addClickHandler(this);
        keyPanel.add(reset_button);
        container.add(keyPanel);

        super.add(container);
        
        OneSwarmRPCClient.getService().getNickname( 
                new AsyncCallback<String[]>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                        loadNotify();
                    }

                    public void onSuccess(String[] result) {
                        nickname_box.setText(result[0]);
                        published_box.setValue(result[1].equals("true"));
                        enabled_box.setValue(result[2].equals("true"));
                        service_key.setText(result[3]);
                        NicknamePanel.this.onClick(null);
                        loadNotify();
                    }
                });
    }

    @Override
    public void sync() {
        OneSwarmRPCClient.getService().setNickname(nickname_box.getText(), published_box.getValue(), enabled_box.getValue(), new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
            }
            @Override
            public void onSuccess(Void result) {
                System.out.println("new nickname set succesfully");   
            }
        });  
        this.policy.sync();
    }

    @Override
    public
    String validData() {
        return null;
    }

    @Override
    public void onClick(ClickEvent event) {
        if (event != null && event.getSource().equals(reset_button)) {
            OneSwarmRPCClient.getService().getNewServiceKey(new AsyncCallback<String>() {
                
                @Override
                public void onSuccess(String result) {
                    service_key.setText(result);
                }
                
                @Override
                public void onFailure(Throwable caught) {
                }
            });
        } else {
            boolean enabled = this.enabled_box.getValue();
            this.nickname_box.setEnabled(enabled);
            this.published_box.setEnabled(enabled);
            this.policy.setEnabled(enabled);
            this.reset_button.setEnabled(enabled);
        }
    }
}
