package edu.washington.cs.oneswarm.ui.gwt.client.newui;

/**
 * A collection of CSS building blocks to build UI elements in the OneSwarm
 * style. If adding to this file, try to keep the categorization and naming as
 * general as possible.
 * 
 * @author nick
 */
public class OneSwarmCss {

    // Generic CSS Styles

    public static final String CLICKABLE = "os-clickable";
    public static final String SMALL_BUTTON = "os-small_button";
    public static final String TEXT_BOLD = "os-bold";
    public static final String TEXT_BLACK = "os-text_color_black";
    public static final String NO_FILES_LABEL = "os-no_files_label";
    public static final String INVALID_FORM_ENTRY = "os-invalid_form_entry";

    // UI Object Specific Styles

    public static class SidebarWidget {
        public static final String MAIN_PANEL = "os-sidebar_widget_main_panel";
        public static final String ITEM = "os-sidebar_widget_item";
        public static final String SELECTED_ITEM = "os-sidebar_widget_selected_item";
        public static final String FOOTER_MENU_BAR = "os-sidebar_widget_footer_menu_bar";
        public static final String FOOTER_MENU_ITEM = "os-sidebar_widget_footer_menu_item";
    }

    public static class SidebarBase {
        public static final String MAIN_VERTICAL_PANEL = "os-sidebar_main_vertical_panel";
        public static final String LINK = "os-sidebar_link";
        public static final String SELECTED_ITEM = "os-sidebar_selected_item";
        public static final String STATS_TEXT = "os-sidebar_stats_text";
        public static final String CHAT_NOTIFICATION = "os-chat_notification";
    }

    public static class Dialog {
        public static final String HEADER = "os-dialog_header";
    }
}
