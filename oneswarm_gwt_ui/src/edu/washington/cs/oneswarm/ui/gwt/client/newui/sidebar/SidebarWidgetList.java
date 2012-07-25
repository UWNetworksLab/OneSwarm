package edu.washington.cs.oneswarm.ui.gwt.client.newui.sidebar;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.IsWidget;

import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;

public abstract class SidebarWidgetList<T> extends SidebarWidget {
    /**
     * Maps a content panel which is equal to the currently displayed content
     * panel to the pointer that GWT is displaying. This overcomes a limitation
     * in GWT that causes an equal child (according to
     * <code>Object.equals()</code>)not to be seen as a child if its memory
     * address is not equal. This exploits a Map's ability to get an equal
     * object rather than the exact same object.
     */
    private Map<SidebarItem, SidebarItem> currentContents;
    private SidebarItem selectedItem;

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
     * @param hasSelectableContents
     *            Enables GUI interaction for selection events and maintains
     *            selection. If selection in this widget does not change the
     *            main content display, this should be false.
     * @param footerMenuItem
     *            The link to embed in the footer of the widget.
     */
    protected SidebarWidgetList(String widgetTitle) {
        super(widgetTitle);
        currentContents = new HashMap<SidebarItem, SidebarItem>();
    }

    public void clearSelected() {
        if (selectedItem != null) {
            selectedItem.asWidget().removeStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
            selectedItem = null;
        }
    }

    public T getSelected() {
        return selectedItem.item;
    }

    /**
     * Sets the content of the widget. Items that are <code>equal</code>
     * according to <code>Object.equals()</code> will be updated, and other
     * objects will be added or removed as necessary. It is recommended to
     * override equals() to reflect the fields that cannot change.
     * 
     * @param contents
     *            The contents to populate the widget with.
     */
    protected void setContent(Collection<SidebarWidgetList<T>.SidebarItem> contents) {
        Map<SidebarItem, SidebarItem> newContents = new HashMap<SidebarItem, SidebarItem>();

        // Keeps existing selection and ordering, adds new content at the
        // bottom.
        for (SidebarItem content : contents) {

            newContents.put(content, content);

            if (currentContents.containsKey(content)) {
                // Replaces the old content with the new content if there are
                // updates.
                SidebarItem oldContent = currentContents.get(content);
                if (content.hasUpdates(oldContent)) {
                    int index = contentPanel.getWidgetIndex(oldContent.asWidget());
                    contentPanel.insert(content.asWidget(), index);
                    if (selectedItem == oldContent) {
                        content.claimSelection();
                    }
                    contentPanel.remove(oldContent.asWidget());
                }

            } else {
                contentPanel.add(content.asWidget());
            }

            currentContents.remove(content);
        }

        // All that is left in currentContents should be removed.
        for (SidebarItem content : currentContents.values()) {
            contentPanel.remove(content.asWidget());
        }

        // 'currentContents' once again represents what is displayed.
        currentContents = newContents;
    }

    public abstract class SidebarItem implements IsWidget {
        private FocusPanel widget;
        public T item;

        public SidebarItem(T item) {
            this.item = item;
        }

        @Override
        public FocusPanel asWidget() {
            if (widget == null) {
                widget = asFocusPanel();
                widget.addStyleName(OneSwarmCss.SidebarWidget.ITEM);
                widget.addFocusHandler(new FocusHandler() {
                    @Override
                    public void onFocus(FocusEvent event) {
                        claimSelection();
                    }
                });
                widget.addStyleName(OneSwarmCss.CLICKABLE);
            }
            return widget;
        }

        /**
         * Lazy constructs a new GWT FocusPanel to display to the user. Once
         * constructed, this should be retained and future calls to
         * <code>asWidget()</code> should return the same widget.
         */
        protected abstract FocusPanel asFocusPanel();

        public abstract boolean hasUpdates(SidebarItem oldItem);

        @Override
        public abstract boolean equals(Object other);

        @Override
        public abstract int hashCode();

        private void claimSelection() {
            if (selectedItem == null) {
                EntireUIRoot.getRoot(widget).clearSidebarSelection();
            } else {
                clearSelected();
            }
            selectedItem = this;
            this.asWidget().addStyleName(OneSwarmCss.SidebarWidget.SELECTED_ITEM);
        }
    }

}
