package edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;

public abstract class SidebarWidget implements IsWidget, Updateable {
    protected final DisclosurePanel widget;
    protected final VerticalPanel widgetBody;
    protected final VerticalPanel contentPanel;
    protected final MenuBar footer;

    private long lastUpdate;
    private int UPDATE_INTERVAL = 5 * 1000;

    private final String widgetTitle;
    private String closedHeaderSuffix;

    /**
     * Main constructor to make a new SidebarWideget. Once instantiated, the
     * abstract method update() will be called every time
     * <code>UPDATE_INTERVAL</code> elapses. To add a HistoryTokenHandler to
     * enable history support use
     * <code>this.registerHistoryTokenHanlder()</code>.
     * 
     * @param widgetTitle
     *            The text to be displayed in the header of the Sidebar.
     * @param startOpen
     *            True if the widget should default to an open state.
     * @param footerMenuItem
     *            The link to embed in the footer of the widget.
     */
    protected SidebarWidget(String widgetTitle) {
        // Basic Building blocks
        widget = new DisclosurePanel(widgetTitle);
        widget.addStyleName(OneSwarmCss.SidebarWidget.MAIN_PANEL);
        widgetBody = new VerticalPanel();
        contentPanel = new VerticalPanel();
        footer = new MenuBar();
        footer.addStyleName(OneSwarmCss.SidebarWidget.FOOTER_MENU_BAR);

        widgetBody.add(contentPanel);
        widgetBody.add(footer);
        widget.add(widgetBody);

        // Initial State
        Boolean startOpen = true;
        String openCookie = Cookies.getCookie(widgetTitle);
        if (openCookie != null) {
            startOpen = Boolean.parseBoolean(openCookie);
        }
        widget.setOpen(startOpen);

        update();
        this.lastUpdate = System.currentTimeMillis() + Random.nextInt(UPDATE_INTERVAL);
        this.widgetTitle = widgetTitle;
        widget.addOpenHandler(openWidget);
        widget.addCloseHandler(closeWidget);

        OneSwarmGWT.addToUpdateTask(this);
    }

    @Override
    public void update(int count) {
        if (UPDATE_INTERVAL != 0
                && (System.currentTimeMillis() - lastUpdate > UPDATE_INTERVAL || count == 0)) {
            lastUpdate = System.currentTimeMillis();
            update();
        }
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    /**
     * Sets how often this widget should update. Defaults to 5 seconds. A value
     * of 0 means never update.
     */
    protected void setUpdateInterval(int seconds) {
        if (seconds == 0) {
            OneSwarmGWT.removeFromUpdateTask(this);
        }
        UPDATE_INTERVAL = seconds * 1000;
    }

    /**
     * Implement this method to update the data in the widget. Call any of the
     * protect methods of the SidebarWidget class to change the display. To
     * change the main contents call <code>setContent()</code> or call
     * <code>setClosedHeaderTextSuffix</code> to set the text displayed when
     * this widget is closed to alert the user of new info.
     * 
     * @return List of panels to be displayed in the widget.
     */
    protected abstract void update();

    /**
     * Sets the sting to be appended after a hyphen when the widget is closed.
     * e.g. A friend tracker widget may wish to add the number of friends online
     * when the widget is closed. "Online Friends - (2)", where "(2)" is the
     * suffix.
     * 
     * @param suffix
     *            The suffix to append after a hyphen, or null to indicate that
     *            no string should be appended.
     */
    protected void setClosedHeaderTextSuffix(String suffix) {
        closedHeaderSuffix = suffix;
        updateHeaderText();
    }

    protected void setContent(Widget content) {
        widgetBody.remove(0);
        widgetBody.insert(content, 0);
    }

    protected void addFooterMenuItem(boolean replaceExisting, MenuItem item) {
        item.setStyleName(OneSwarmCss.SidebarWidget.FOOTER_MENU_ITEM);
        if (replaceExisting) {
            footer.clearItems();
        }
        footer.addItem(item);
    }

    private void updateHeaderText() {
        if (!widget.isOpen() && closedHeaderSuffix != null) {
            widget.getHeaderTextAccessor().setText(widgetTitle + " - " + closedHeaderSuffix);
        } else {
            widget.getHeaderTextAccessor().setText(widgetTitle);
        }
    }

    private final OpenHandler<DisclosurePanel> openWidget = new OpenHandler<DisclosurePanel>() {
        @Override
        public void onOpen(OpenEvent<DisclosurePanel> event) {
            Cookies.setCookie(widgetTitle, true + "", OneSwarmConstants.TEN_YEARS_FROM_NOW);
            updateHeaderText();
        }
    };

    private final CloseHandler<DisclosurePanel> closeWidget = new CloseHandler<DisclosurePanel>() {
        @Override
        public void onClose(CloseEvent<DisclosurePanel> event) {
            Cookies.setCookie(widgetTitle, false + "", OneSwarmConstants.TEN_YEARS_FROM_NOW);
            updateHeaderText();
        }
    };
}
