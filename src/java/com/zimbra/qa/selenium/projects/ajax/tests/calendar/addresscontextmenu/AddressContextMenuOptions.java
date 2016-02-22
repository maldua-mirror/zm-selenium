package com.zimbra.qa.selenium.projects.ajax.tests.calendar.addresscontextmenu;

import org.testng.annotations.*;
import com.zimbra.qa.selenium.framework.items.*;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.PrefGroupMailByMessageTest;
import com.zimbra.qa.selenium.projects.ajax.ui.calendar.FormApptNew;

public class AddressContextMenuOptions extends PrefGroupMailByMessageTest {

	public AddressContextMenuOptions() {
		logger.info("New " + AddressContextMenuOptions.class.getCanonicalName());
		super.startingPage = app.zPageCalendar;
	}

	@Test(description = "Right click To address bubble and verify Delete, Copy, Edit, Expand and Add to Contacts menus", groups = { "sanity" })
	
	public void VerifyAttendeesContextMenuOptions() throws HarnessException {

		String apptAttendee1;
		AppointmentItem appt = new AppointmentItem();
		apptAttendee1 = ZimbraAccount.AccountA().EmailAddress;
		appt.setAttendees(apptAttendee1);
		
		FormApptNew apptForm = (FormApptNew) app.zPageCalendar.zToolbarPressButton(Button.B_NEW);
		apptForm.zFill(appt);
		
		app.zPageCalendar.zRightClickAddressBubble();

		logger.info(app.zPageMail.zVerifyAllAddressContextMenu("calendar"));
		ZAssert.assertTrue(	app.zPageMail.zVerifyAllAddressContextMenu("calendar"),	"Delete, Copy, Edit, Expand and Add to Contacts menus doesn't exists");

	}

}