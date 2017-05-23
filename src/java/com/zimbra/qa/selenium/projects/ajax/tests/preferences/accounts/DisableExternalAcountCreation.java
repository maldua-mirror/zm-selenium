/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
package com.zimbra.qa.selenium.projects.ajax.tests.preferences.accounts;

import org.testng.annotations.Test;

import com.zimbra.qa.selenium.framework.ui.Action;
import com.zimbra.qa.selenium.framework.util.HarnessException;
import com.zimbra.qa.selenium.framework.util.ZAssert;
import com.zimbra.qa.selenium.framework.util.ZimbraAdminAccount;
import com.zimbra.qa.selenium.projects.ajax.core.AjaxCommonTest;
import com.zimbra.qa.selenium.projects.ajax.ui.preferences.PagePreferences.Locators;
import com.zimbra.qa.selenium.projects.ajax.ui.preferences.TreePreferences.TreeItem;

public class DisableExternalAcountCreation extends AjaxCommonTest {

	public DisableExternalAcountCreation() {
		super.startingPage = app.zPagePreferences;
	}

	@Test(description = "Verify the display of 'Add External Account' button when External IMAP and POP3 is disabled", 
			groups = { "functional", "L3" })

	public void DisableExternalAcountCreation_01() throws HarnessException {

		// Navigate to preferences -> Accounts
		app.zTreePreferences.zTreeItem(Action.A_LEFTCLICK, TreeItem.MailAccounts);

		// See https://bugzilla.zimbra.com/show_bug.cgi?id=106132
		// Verify the display of Add External Account button when External IMAP/POP3 access is allowed.
		ZAssert.assertTrue(app.zPagePreferences.sIsElementPresent(Locators.zAddExternalAccountButton), "Add External Account button is not present!");

		//Disable the External IMAP and POP3 access for the user
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
						+		"<id>"+app.zGetActiveAccount().ZimbraId +"</id>"
						+		"<a n='zimbraFeatureImapDataSourceEnabled'>FALSE</a>"
						+		"<a n='zimbraFeaturePop3DataSourceEnabled'>FALSE</a>"
						+	"</ModifyAccountRequest>");

		//Refresh Web-client
		app.zPageMail.sRefresh();

		//Navigate to Preferences-->Accounts
		startingPage.zNavigateTo();
		app.zTreePreferences.zTreeItem(Action.A_LEFTCLICK, TreeItem.MailAccounts);

		// Verify the display of Add External Account button when External IMAP/POP3 accesses are not allowed.
		ZAssert.assertFalse(app.zPagePreferences.sIsElementPresent(Locators.zAddExternalAccountButton), 
				"Add External Account button is displayed even if the external IMAP and POP3 accesses are disabled!");

		//Enable External IMAP and POP3 access for the user
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
						+		"<id>"+app.zGetActiveAccount().ZimbraId +"</id>"
						+		"<a n='zimbraFeatureImapDataSourceEnabled'>TRUE</a>"
						+		"<a n='zimbraFeaturePop3DataSourceEnabled'>TRUE</a>"
						+	"</ModifyAccountRequest>");

		//Refresh Web-client
		app.zPageMail.sRefresh();

		//Navigate to Preferences-->Accounts
		startingPage.zNavigateTo();
		app.zTreePreferences.zTreeItem(Action.A_LEFTCLICK, TreeItem.MailAccounts);

		//Add an external account
		app.zPagePreferences.sClick(Locators.zAddExternalAccountButton);

		//Verify that a new External account row has been added
		ZAssert.assertTrue(app.zPagePreferences.sIsElementPresent(Locators.zExternalAccountRow1), "External account is not getting created!");
	}

}
