/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.selenium.projects.ajax.tests.preferences.outofoffice;

import java.util.HashMap;
import org.testng.annotations.Test;
import com.zimbra.common.soap.Element;
import com.zimbra.qa.selenium.framework.core.Bugs;
import com.zimbra.qa.selenium.framework.ui.Action;
import com.zimbra.qa.selenium.framework.ui.Button;
import com.zimbra.qa.selenium.framework.util.HarnessException;
import com.zimbra.qa.selenium.framework.util.SleepUtil;
import com.zimbra.qa.selenium.framework.util.ZAssert;
import com.zimbra.qa.selenium.framework.util.ZimbraAccount;
import com.zimbra.qa.selenium.framework.util.ConfigProperties;
import com.zimbra.qa.selenium.projects.ajax.core.AjaxCommonTest;
import com.zimbra.qa.selenium.projects.ajax.ui.AppAjaxClient;
import com.zimbra.qa.selenium.projects.ajax.ui.preferences.DialogOOOAlert;
import com.zimbra.qa.selenium.projects.ajax.ui.preferences.TreePreferences.TreeItem;

public class ZimbraPrefOutOfOfficeReplyEnabledFalse extends AjaxCommonTest {

	public static final String autoReplyMessage = "OOO" + ConfigProperties.getUniqueString();

	public ZimbraPrefOutOfOfficeReplyEnabledFalse() throws HarnessException {

		super.startingAccountPreferences = new HashMap<String, String>() {
			private static final long serialVersionUID = -3101848474022410670L;
			{
				put("zimbraPrefOutOfOfficeReplyEnabled", "TRUE");
				put("zimbraPrefOutOfOfficeReply", autoReplyMessage);
				put("zimbraPrefOutOfOfficeStatusAlertOnLogin", "TRUE");
			}
		};
	}

	
	@Bugs(ids = "101356")
	@Test(description = "Disable out of office", 
		groups = { "smoke" })

	public void ZimbraPrefOutOfOfficeReplyEnabledFalse_01() throws HarnessException {
		
		ZimbraAccount account = app.zGetActiveAccount();
		String subject = "subject" + ConfigProperties.getUniqueString();

		// Client must display out of office dialog, wait for some time and take an action on it
		SleepUtil.sleepLong();
		DialogOOOAlert alert = new DialogOOOAlert(app, ((AppAjaxClient) app).zPageMail);
		ZAssert.assertTrue(alert.zIsActive(), "Verify turn off auto-reply alert dialog is displayed");
		alert.zCheckboxSet(true);
		alert.zClickButton(Button.B_YES);

		/* GUI steps */

		// Navigate to preferences -> mail -> Out of Office
		app.zPagePreferences.zNavigateTo();
		app.zTreePreferences.zTreeItem(Action.A_LEFTCLICK, TreeItem.MailOutOfOffice);

		// Disable the preferences
		app.zPagePreferences.sClick("css=input[id$='VACATION_MSG_DISABLED_input']");

		// Click "Save"
		app.zPagePreferences.zToolbarPressButton(Button.B_SAVE);

		/* Test verification */

		app.zGetActiveAccount().soapSend("<GetPrefsRequest xmlns='urn:zimbraAccount'>"
				+ "<pref name='zimbraPrefOutOfOfficeReplyEnabled'/>" + "<pref name='zimbraPrefOutOfOfficeReply'/>"
				+ "<pref name='zimbraPrefOutOfOfficeStatusAlertOnLogin'/>" + "</GetPrefsRequest>");

		String zimbraPrefOutOfOfficeReplyEnabled = app.zGetActiveAccount()
				.soapSelectValue("//acct:pref[@name='zimbraPrefOutOfOfficeReplyEnabled']", null);
		String zimbraPrefOutOfOfficeReply = app.zGetActiveAccount()
				.soapSelectValue("//acct:pref[@name='zimbraPrefOutOfOfficeReply']", null);
		String zimbraPrefOutOfOfficeStatusAlertOnLogin = app.zGetActiveAccount()
				.soapSelectValue("//acct:pref[@name='zimbraPrefOutOfOfficeStatusAlertOnLogin']", null);

		// zimbraPrefOutOfOfficeReplyEnabled should be FALSE, but all other properties should not be cleared (in case re-enabled in the future)
		ZAssert.assertEquals(zimbraPrefOutOfOfficeReplyEnabled, "FALSE",
				"Verify zimbraPrefOutOfOfficeReplyEnabled is FALSE");
		ZAssert.assertEquals(zimbraPrefOutOfOfficeReply, autoReplyMessage,
				"Verify zimbraPrefOutOfOfficeReply contains the message");
		ZAssert.assertEquals(zimbraPrefOutOfOfficeStatusAlertOnLogin, "TRUE",
				"Verify zimbraPrefOutOfOfficeStatusAlertOnLogin is TRUE");
		
		account.soapSend("<SendMsgRequest xmlns='urn:zimbraMail'>" + "<m>" + "<e t='t' a='" + account.EmailAddress
				+ "'/>" + "<su>" + subject + "</su>" + "<mp ct='text/plain'>" + "<content>content"
				+ ConfigProperties.getUniqueString() + "</content>" + "</mp>" + "</m>" + "</SendMsgRequest>");

		// Verify the message is no longer in the mailbox
		app.zGetActiveAccount().soapSend(
				"<SearchRequest xmlns='urn:zimbraMail' types='message'>"
			+		"<query>" + "in:inbox " + autoReplyMessage + "</query>"
			+	"</SearchRequest>");

		Element[] nodes = app.zGetActiveAccount().soapSelectNodes("//mail:m");
		ZAssert.assertEquals(nodes.length, 0, "Verify auto-reply message is not received");

	}
}