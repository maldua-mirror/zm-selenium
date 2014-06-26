/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.selenium.projects.ajax.tests.addressbook.performance;

import org.testng.annotations.Test;

import com.zimbra.qa.selenium.framework.items.FolderItem;
import com.zimbra.qa.selenium.framework.ui.Button;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.framework.util.performance.*;
import com.zimbra.qa.selenium.projects.ajax.core.AjaxCommonTest;



public class ZmContactsAppFolders extends AjaxCommonTest {

	public ZmContactsAppFolders() {
		logger.info("New "+ ZmContactsAppFolders.class.getCanonicalName());


		super.startingPage = app.zPageMail;
		super.startingAccountPreferences = null;

	}

	@Test(	description = "Measure the time to load the contacts app, 1 addressbook",
			groups = { "performance" })
	public void ZmContactsAppFolders_01() throws HarnessException {

		// Create a folder
		FolderItem root = FolderItem.importFromSOAP(app.zGetActiveAccount(), FolderItem.SystemFolder.UserRoot);
		app.zGetActiveAccount().soapSend(
				"<CreateFolderRequest xmlns='urn:zimbraMail'>" +
					"<folder name='ab"+ ZimbraSeleniumProperties.getUniqueString() + "' view='contact' l='"+ root.getId() +"'/>" +
				"</CreateFolderRequest>");


		// Sync the changes to the client (notification block)
		app.zPageMail.zToolbarPressButton(Button.B_GETMAIL);
		
		PerfToken token = PerfMetrics.startTimestamp(PerfKey.ZmContactsAppOverviewPanel, "Load the contacts app, 1 addressbook");

		// Currently in the mail app
		// Navigate to the addressbook
		//app.zPageAddressbook.zNavigateTo();
		app.zPageAddressbook.zClickAt("css=td[id='zb__App__Contacts_title']","");

		PerfMetrics.waitTimestamp(token);

		// Wait for the app to load
		app.zPageAddressbook.zWaitForActive();


	}

	@Test(	description = "Measure the time to load the contacts app, 100 addressbooks",
			groups = { "performance" })
	public void ZmContactsAppFolders_02() throws HarnessException {

		// Create 100 folders
		FolderItem root = FolderItem.importFromSOAP(app.zGetActiveAccount(), FolderItem.SystemFolder.UserRoot);
		for (int i = 0; i < 100; i++) {
			app.zGetActiveAccount().soapSend(
					"<CreateFolderRequest xmlns='urn:zimbraMail'>" +
						"<folder name='ab"+ ZimbraSeleniumProperties.getUniqueString() + "' view='contact' l='"+ root.getId() +"'/>" +
					"</CreateFolderRequest>");
		}


		// Sync the changes to the client (notification block)
		app.zPageMail.zToolbarPressButton(Button.B_GETMAIL);

		PerfToken token = PerfMetrics.startTimestamp(PerfKey.ZmContactsAppOverviewPanel, "Load the contacts app, 100 addressbooks");

		// Currently in the mail app
		// Navigate to the addressbook
		//app.zPageAddressbook.zNavigateTo();
		app.zPageAddressbook.zClickAt("css=td[id='zb__App__Contacts_title']","");

		PerfMetrics.waitTimestamp(token);

		// Wait for the app to load
		app.zPageAddressbook.zWaitForActive();


	}


}
