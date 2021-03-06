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
 */
package com.csipsimple.phone.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

import com.csipsimple.phone.R;
import com.csipsimple.phone.db.DBAdapter;
import com.csipsimple.phone.service.SipService;
import com.csipsimple.phone.ui.prefs.MainPrefs;
import com.csipsimple.phone.utils.Log;

public class SipHome extends TabActivity {
    public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
	public static final int PARAMS_MENU = Menu.FIRST + 2;
	public static final int CLOSE_MENU = Menu.FIRST + 3;
	
	public static final String LAST_KNOWN_VERSION_PREF = "last_known_version";
	
	private static final String THIS_FILE = "SIP HOME";
	
	
	private Intent serviceIntent;
	
	private Intent dialerIntent;
	private Intent csipIntent;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(THIS_FILE, "On Create SIPHOME");
        super.onCreate(savedInstanceState);
        
        //Check sip stack
        if( !SipService.hasStackLibFile(this) ){
        	Log.d(THIS_FILE, "Has no sip stack....");
			Intent welcomeIntent = new Intent(this, WelcomeScreen.class);
			welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			welcomeIntent.putExtra(WelcomeScreen.KEY_MODE, WelcomeScreen.MODE_WELCOME);
			startActivity(welcomeIntent);
			finish();
			return;
        }else if(!SipService.isBundleStack(this)){
            //We have to check and save current version
            try {
    			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
    			int running_version = pinfo.versionCode;
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    			int last_seen_version = prefs.getInt(LAST_KNOWN_VERSION_PREF, 0);
    			
    			Log.d(THIS_FILE, "Last known version is "+last_seen_version+" and currently we are running "+running_version);
    			if(last_seen_version != running_version) {
    				//TODO : check if greater version
    				//(should be most of the case...but if not we should maybe popup the user that 
    				//if n+1 version doesn't work for him he could fill a bug on bug tracker)
    				
    				
    				Log.d(THIS_FILE, "Has no sip stack....");
    				Intent changelogIntent = new Intent(this, WelcomeScreen.class);
    				changelogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    				changelogIntent.putExtra(WelcomeScreen.KEY_MODE, WelcomeScreen.MODE_CHANGELOG);
    				startActivity(changelogIntent);
    				finish();
    				return;
    			}else {
    	        	Log.d(THIS_FILE, "WE CAN NOW start SIP service");
    		        //Start the service
    		        serviceIntent = new Intent(SipHome.this, SipService.class);
    		        startService(serviceIntent);
    			}
    		} catch (NameNotFoundException e) {
    			//Should not happen....or something is wrong with android...
    		}
        }else {
        	Log.d(THIS_FILE, "WE CAN NOW start SIP service");
	        //Start the service
	        serviceIntent = new Intent(SipHome.this, SipService.class);
	        startService(serviceIntent);
        }
        
        TabHost tabHost = getTabHost();
        Resources r = getResources();
        //dialerIntent = new Intent("com.csipsimple.phone.action.DIALER");
        csipIntent = new Intent(this, BuddyList.class);
        /*
        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator("Dialer", r.getDrawable(R.drawable.ic_tab_selected_dialer))
                .setContent(dialerIntent));
*/
        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator("Buddy list")
                .setContent(csipIntent));
        
        //If we have no account yet, open account panel,
        DBAdapter db = new DBAdapter(this);
        db.open();
        int nbrOfAccount = db.getNbrOfAccount();
        db.close();
        if(nbrOfAccount == 0) {
	        Intent accountIntent = new Intent(this, AccountsList.class);
	        accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(accountIntent);
        }
    }
    
    @Override
    protected void onPause() {
    	Log.d(THIS_FILE, "On Pause SIPHOME");
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
    	Log.d(THIS_FILE, "On Resume SIPHOME");
    	super.onResume();
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(THIS_FILE, "---DESTROY SIP HOME END---");
	}
    

    private void populateMenu(Menu menu) {
    	menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE, R.string.accounts).setIcon(
				R.drawable.ic_menu_accounts);
		menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, R.string.prefs).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, "Quit").setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
	}
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		populateMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ACCOUNTS_MENU:
			startActivity(new Intent(this, AccountsList.class));
			return true;
		case PARAMS_MENU:
			startActivity(new Intent(this, MainPrefs.class));
			return true;
		case CLOSE_MENU:
	    	Log.d(THIS_FILE, "CLOSE");
	    	if(serviceIntent != null){
				stopService(serviceIntent);
			}
	    	serviceIntent = null;
			this.finish();
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    
}
