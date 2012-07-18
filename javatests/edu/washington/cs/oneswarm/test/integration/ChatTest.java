package edu.washington.cs.oneswarm.test.integration;

import java.util.List;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.chat.Chat;
import edu.washington.cs.oneswarm.f2f.chat.ChatDAO;
import edu.washington.cs.oneswarm.test.util.ConditionWaiter;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.test.util.TwoProcessTestBase;

public class ChatTest extends TwoProcessTestBase {
    private static Logger logger = Logger.getLogger(ChatTest.class.getName());

    private static final String FIRST_CHAT_FRIEND_XPATH = "//div[@class='os-friendListElement']";
    private static final String CHAT_TEXT_BOX_XPATH = "//input[@id='chatTextBox']";

    @Test
    public void testSendReceiveChat() throws Exception {
        logger.info("Start testSendReceiveChat().");

        /*
         * Test plan: Send a chat message using the Web UI and verify that it is
         * displayed in the web UI of the remote host.
         */
        try {
            selenium.openWindow("http://127.0.0.1:4000/", "jvm");
            selenium.openWindow("http://127.0.0.1:3000/", "local");

            selenium.selectWindow("jvm");

            // Wait for the friends list AJAX load to complete
            TestUtils.awaitElement(selenium, FIRST_CHAT_FRIEND_XPATH);
            selenium.doubleClick(FIRST_CHAT_FRIEND_XPATH);

            // Send chat message
            final String chatMessage = "ChatMessage JVM to Local";
            TestUtils.awaitElement(selenium, CHAT_TEXT_BOX_XPATH);
            selenium.focus(CHAT_TEXT_BOX_XPATH);
            selenium.type(CHAT_TEXT_BOX_XPATH, chatMessage);
            selenium.keyDown(CHAT_TEXT_BOX_XPATH, "\\13");
            selenium.keyUp(CHAT_TEXT_BOX_XPATH, "\\13");

            // Verify local display of the chat message.
            new ConditionWaiter(new ConditionWaiter.Predicate() {
                @Override
                public boolean satisfied() {
                    return selenium.isTextPresent(chatMessage);
                }
            }, 5000).awaitFail();

            // Switch to the other instance
            selenium.selectWindow("local");

            // Verify notification presence -- this could take up to 10 seconds
            // since
            // we have a 10 seconds poll (See {@code FriendListPanel.java}).
            new ConditionWaiter(new ConditionWaiter.Predicate() {
                @Override
                public boolean satisfied() {
                    return selenium.isElementPresent("link=1 unread message");
                }
            }, 15000).awaitFail();

            // Click to bring up chat box
            selenium.click("link=1 unread message");

            // Verify message in chat box
            new ConditionWaiter(new ConditionWaiter.Predicate() {
                @Override
                public boolean satisfied() {
                    return selenium.isTextPresent("ChatMessage JVM to Local");
                }
            }, 5000).awaitFail();

            // Finally, verify that this message was stored in our local
            // database
            List<Chat> storedMessage = ChatDAO.get().getMessagesForUser(
                    localOneSwarm.getPublicKey(), true, 1);
            Assert.assertEquals(storedMessage.get(0).getMessage(), chatMessage);

            selenium.close();
        } finally {
            logger.info("End testSendReceiveChat().");
        }
    }

    /** Closes the web UI */
    @After
    public void tearDownTest() throws Exception {
        selenium.close();
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(ChatTest.class);
    }
}
