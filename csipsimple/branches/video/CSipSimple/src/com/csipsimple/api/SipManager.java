/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  This file and this file only is released under dual Apache license
 */
package com.csipsimple.api;

import android.os.RemoteException;

public final class SipManager {
	// -------
	// Static constants
	// PERMISSION
	public static final String PERMISSION_USE_SIP = "android.permission.USE_SIP";
	public static final String PERMISSION_CONFIGURE_SIP = "android.permission.CONFIGURE_SIP";
	
	// SERVICE intents

	public static final String INTENT_SIP_CONFIGURATION = "com.csipsimple.service.SipConfiguration";
	public static final String INTENT_SIP_SERVICE = "com.csipsimple.service.SipService";
	public static final String INTENT_SIP_ACCOUNT_ACTIVATE = "com.csipsimple.accounts.activate";
	
	// -------
	// ACTIONS
	public static final String ACTION_SIP_CALL_UI = "com.csipsimple.phone.action.INCALL";
	public static final String ACTION_SIP_DIALER = "com.csipsimple.phone.action.DIALER";
	public static final String ACTION_SIP_CALLLOG = "com.csipsimple.phone.action.CALLLOG";
	public static final String ACTION_SIP_MESSAGES = "com.csipsimple.phone.action.MESSAGES";
	
	// SERVICE BROADCASTS
	public static final String ACTION_SIP_CALL_CHANGED = "com.csipsimple.service.CALL_CHANGED";
	public static final String ACTION_SIP_REGISTRATION_CHANGED = "com.csipsimple.service.REGISTRATION_CHANGED";
	public static final String ACTION_SIP_MEDIA_CHANGED = "com.csipsimple.service.MEDIA_CHANGED";
	public static final String ACTION_SIP_ACCOUNT_ACTIVE_CHANGED = "com.csipsimple.service.ACCOUNT_ACTIVE_CHANGED";
	public static final String ACTION_SIP_CAN_BE_STOPPED = "com.csipsimple.service.ACTION_SIP_CAN_BE_STOPPED";
	public static final String ACTION_ZRTP_SHOW_SAS = "com.csipsimple.service.SHOW_SAS";
	
	public static final String ACTION_SIP_MESSAGE_RECEIVED = "com.csipsimple.service.MESSAGE_RECEIVED";
	//TODO : message sent?
	public static final String ACTION_SIP_MESSAGE_STATUS = "com.csipsimple.service.MESSAGE_STATUS";
	
	public static final String ACTION_GET_DRAWABLES = "com.csipsimple.themes.GET_DRAWABLES";
	
	// EXTRAS
	public static final String EXTRA_CALL_INFO = "call_info";
	public static final String EXTRA_ACCOUNT_ID = "acc_id";
	public static final String EXTRA_ACTIVATE = "activate";
	
	// Constants
	public static final int SUCCESS = 0;
	public static final int CURRENT_API = 1000;
	
	public static boolean isApiCompatible(ISipService service) {
		if(service != null) {
			try {
				int version = service.getVersion();
				return (Math.floor(version / 1000) == Math.floor(CURRENT_API % 1000));
			} catch (RemoteException e) {
				// We consider this is a bad api version that does not have versionning at all
				return false;
			}
		}
		
		return false;
	}
}
