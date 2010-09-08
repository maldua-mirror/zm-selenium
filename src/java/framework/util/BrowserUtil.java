package framework.util;

import framework.core.*;

public class BrowserUtil extends SelNGBase {
	private static String userAgent = null;

	private static void setBrowserAgent() {
		//ps: getZimbraVersion is being passed just to trick selenium.call to return string(because of get*)
		if (userAgent == null) {
			userAgent = ClientSessionFactory.session().selenium().getEval("navigator.userAgent;");
			if ( userAgent.equals("") ) {
				userAgent = null;
			}
		}
	}

	/**
	 * Get the browser name
	 * @return the browser name
	 */
	public static String getBrowserName() {
		setBrowserAgent();
		String browserName = "";
		if (userAgent.indexOf("Firefox/") >= 0){
			browserName = "FF " + userAgent.split("Firefox/")[1];
			String[] temp = browserName.split(" ");
			browserName = temp[0]+ " "+ temp[1];
		} else if (userAgent.indexOf("MSIE") >= 0) {
			String[] arry = userAgent.split(";");
			for (int t = 0; t < arry.length; t++) {
				if (arry[t].indexOf("MSIE") >= 0) {
					browserName = arry[t];
					break;
				}
			}
		} else if (userAgent.indexOf("Chrome") >= 0) {
			String[] arry = userAgent.split("/");
			for (int t = 0; t < arry.length; t++) {
				if (arry[t].indexOf("Chrome") >= 0) {
					String [] tmp = arry[t].split(" ");
					browserName = tmp[1] + " " +tmp[0];
					break;
				}
			}
		}else if (userAgent.indexOf("Safari") >= 0) {
			String[] arry = userAgent.split("/");
			for (int t = 0; t < arry.length; t++) {
				if (arry[t].indexOf("Safari") >= 0) {
					String [] tmp = arry[t].split(" ");
					browserName = tmp[1] + " " +tmp[0];
					break;
				}
			}
		}
		return browserName;
	}

}