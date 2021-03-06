/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2010 Jan Tschirschwitz <jan.tschirschwitz@googlemail.com>
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
package com.csipsimple.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.pjsip.NativeLibManager;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.help.Help;
import com.csipsimple.ui.messages.ConversationList;
import com.csipsimple.ui.prefs.MainPrefs;
import com.csipsimple.ui.prefs.PrefsFast;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.contacts.ContactsWrapper;
import com.csipsimple.utils.contacts.ContactsWrapper.OnPhoneNumberSelected;
import com.csipsimple.widgets.IndicatorTab;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class SipHome extends TabActivity {
	public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
	public static final int PARAMS_MENU = Menu.FIRST + 2;
	public static final int CLOSE_MENU = Menu.FIRST + 3;
	public static final int HELP_MENU = Menu.FIRST + 4;
	public static final int DISTRIB_ACCOUNT_MENU = Menu.FIRST + 5;
	

	public static final String LAST_KNOWN_VERSION_PREF = "last_known_version";
	public static final String HAS_ALREADY_SETUP = "has_already_setup";

	private static final String THIS_FILE = "SIP HOME";
	
	private static final String TAB_DIALER = "dialer";
	private static final String TAB_CALLLOG = "calllog";
	private static final String TAB_MESSAGES = "messages";
	
	protected static final int PICKUP_PHONE = 0;
	private static final int REQUEST_EDIT_DISTRIBUTION_ACCOUNT = PICKUP_PHONE + 1;

	private Intent serviceIntent;

	private Intent dialerIntent,calllogsIntent, messagesIntent;
	private PreferencesWrapper prefWrapper;

	private boolean has_tried_once_to_activate_account = false;
	private ImageButton pickupContact;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "On Create SIPHOME");
		prefWrapper = new PreferencesWrapper(this);
		super.onCreate(savedInstanceState);
		
		
		
		// Check sip stack
		if (!NativeLibManager.hasStackLibFile(this)) {
			//If not -> FIRST RUN : Just launch stack getter
			Log.d(THIS_FILE, "Has no sip stack....");
			Intent welcomeIntent = new Intent(this, WelcomeScreen.class);
			welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			welcomeIntent.putExtra(WelcomeScreen.KEY_MODE, WelcomeScreen.MODE_WELCOME);
			startActivity(welcomeIntent);
			finish();
			return;
		} else if (!NativeLibManager.hasBundleStack(this)) {
			// It's not the first setup and there is debug stack
			// We have to check and save current version
			Integer runningVersion = needUpgrade();
			if(runningVersion != null) {
				// Just to be sure delete the current stack and anyway upgrade it.
				NativeLibManager.cleanStack(this);
				
				// We have to launch WelcomeScreen again
				Log.d(THIS_FILE, "Sip stack may have changed");
				Intent changelogIntent = new Intent(this, WelcomeScreen.class);
				changelogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				changelogIntent.putExtra(WelcomeScreen.KEY_MODE, WelcomeScreen.MODE_CHANGELOG);
				startActivity(changelogIntent);
				finish();
				return;
			}
		}else {
			// DEV MODE -- still upgrade settings
			Integer runningVersion = needUpgrade();
			if(runningVersion != null) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = prefs.edit();
				editor.putInt(SipHome.LAST_KNOWN_VERSION_PREF, runningVersion);
				editor.commit();
			}
		}
		
		

		setContentView(R.layout.home);

		dialerIntent = new Intent(this, Dialer.class);
		calllogsIntent = new Intent(this, CallLogsList.class);
		messagesIntent = new Intent(this, ConversationList.class);

		addTab(TAB_DIALER, getString(R.string.dial_tab_name_text), R.drawable.ic_tab_selected_dialer, R.drawable.ic_tab_unselected_dialer, dialerIntent);
		addTab(TAB_CALLLOG, getString(R.string.calllog_tab_name_text), R.drawable.ic_tab_selected_recent, R.drawable.ic_tab_unselected_recent, calllogsIntent);
		addTab(TAB_MESSAGES, getString(R.string.messages_tab_name_text), R.drawable.ic_tab_selected_messages, R.drawable.ic_tab_unselected_messages, messagesIntent);
		
		pickupContact = (ImageButton) findViewById(R.id.pickup_contacts);
		pickupContact.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(Compatibility.getContactPhoneIntent(), PICKUP_PHONE);
			}
		});
		
		has_tried_once_to_activate_account = false;
		

		if(!prefWrapper.getPreferenceBooleanValue(PreferencesWrapper.PREVENT_SCREEN_ROTATION)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
		
		selectTabWithAction(getIntent());
	}
	
	/**
	 * Check wether an upgrade is needed
	 * @return null if not needed, else the new version to upgrade to
	 */
	private Integer needUpgrade() {
		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			int runningVersion = pinfo.versionCode;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			int lastSeenVersion = prefs.getInt(LAST_KNOWN_VERSION_PREF, 0);
	
			Log.d(THIS_FILE, "Last known version is " + lastSeenVersion + " and currently we are running " + runningVersion);
			if (lastSeenVersion != runningVersion) {
				Compatibility.updateVersion(prefWrapper, lastSeenVersion, runningVersion);
				return runningVersion;
			}
			return null;
		} catch (NameNotFoundException e) {
			// Should not happen....or something is wrong with android...
			Log.e(THIS_FILE, "Not possible to find self name", e);
		}
		return null;
	}

	private void startSipService() {
		if (serviceIntent == null) {
			serviceIntent = new Intent(SipHome.this, SipService.class);
		}
		Thread t = new Thread() {
			public void run() {
				startService(serviceIntent);
				postStartSipService();
			};
		};
		t.start();

	}
	
	private void postStartSipService() {
		// If we have never set fast settings
		if (!prefWrapper.hasAlreadySetup()) {
			Intent prefsIntent = new Intent(this, PrefsFast.class);
			prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(prefsIntent);
			return;
		}

		// If we have no account yet, open account panel,
		if (!has_tried_once_to_activate_account) {
			SipProfile account = null;
			DBAdapter db = new DBAdapter(this);
			db.open();
			int nbrOfAccount = db.getNbrOfAccount();
			
			if (nbrOfAccount == 0) {
				WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
				if(distribWizard != null) {
					account = db.getAccountForWizard(distribWizard.id);
				}
			}
			
			db.close();
			
			if(nbrOfAccount == 0) {
				Intent accountIntent = null;
				if(account != null) {
					if(account.id == SipProfile.INVALID_ID) {
						accountIntent = new Intent(this, BasePrefsWizard.class);
						accountIntent.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
						startActivity(new Intent(this, AccountsList.class));
					}
				}else {
					accountIntent = new Intent(this, AccountsList.class);
				}
				
				if(accountIntent != null) {
					accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(accountIntent);
					has_tried_once_to_activate_account = true;
					return;
				}
			}
			has_tried_once_to_activate_account = true;
		}
	}
	
	
	private void addTab(String tag, String label, int icon, int ficon, Intent content) {
		TabHost tabHost = getTabHost();
		TabSpec tabspecDialer = tabHost.newTabSpec(tag).setContent(content);

		boolean fails = true;
		if (Compatibility.isCompatible(4)) {
			IndicatorTab icTab = new IndicatorTab(this, null);
			icTab.setResources(label, icon, ficon);
			try {
				Method method = tabspecDialer.getClass().getDeclaredMethod("setIndicator", View.class);
				method.invoke(tabspecDialer, icTab);
				fails = false;
			} catch (Exception e) {
				Log.d(THIS_FILE, "We are probably on 1.5 : use standard simple tabs");
			}

		}
		if (fails) {
			tabspecDialer.setIndicator(label, getResources().getDrawable(icon));
		}

		tabHost.addTab(tabspecDialer);
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
		prefWrapper.setQuit(false);

		Log.d(THIS_FILE, "WE CAN NOW start SIP service");
		startSipService();

		
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		selectTabWithAction(intent);
	}

	private void selectTabWithAction(Intent intent) {
		if(intent != null) {
			String callAction = intent.getAction();
			if(SipManager.ACTION_SIP_CALLLOG.equalsIgnoreCase(callAction)) {
				getTabHost().setCurrentTab(1);
			}else if(SipManager.ACTION_SIP_DIALER.equalsIgnoreCase(callAction)) {
				getTabHost().setCurrentTab(0);
			}else if(SipManager.ACTION_SIP_MESSAGES.equalsIgnoreCase(callAction)) {
				getTabHost().setCurrentTab(2);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(THIS_FILE, "---DESTROY SIP HOME END---");
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
	    if (keyCode == KeyEvent.KEYCODE_BACK 
	    		&& event.getRepeatCount() == 0
	    		&& !Compatibility.isCompatible(5)) {
	    	onBackPressed();
	    	
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	public void onBackPressed() {
		if(prefWrapper != null) {
			Log.d(THIS_FILE, "On back pressed ! ");
    		ArrayList<String> networks = prefWrapper.getAllIncomingNetworks();
			if (networks.size() == 0) {
				disconnectAndQuit();
				return;
			}
    	}
		finish();
	}


	private void populateMenu(Menu menu) {
		WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
		if(distribWizard != null) {
			menu.add(Menu.NONE, DISTRIB_ACCOUNT_MENU, Menu.NONE, "My " + distribWizard.label).setIcon(distribWizard.icon);
		}
		if(CustomDistribution.distributionWantsOtherAccounts()) {
			menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE, (distribWizard == null)?R.string.accounts:R.string.other_accounts).setIcon(R.drawable.ic_menu_accounts);
		}
		menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, R.string.prefs).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, HELP_MENU, Menu.NONE, R.string.help).setIcon(android.R.drawable.ic_menu_help);
		menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, R.string.menu_disconnect).setIcon(R.drawable.ic_lock_power_off);

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

	//	PreferencesWrapper prefsWrapper = new PreferencesWrapper(this);
	//	menu.findItem(CLOSE_MENU).setVisible(!prefsWrapper.isValidConnectionForIncoming());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		populateMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SipProfile account;
		switch (item.getItemId()) {
		case ACCOUNTS_MENU:
			startActivity(new Intent(this, AccountsList.class));
			return true;
		case PARAMS_MENU:
			startActivity(new Intent(this, MainPrefs.class));
			return true;
		case CLOSE_MENU:
			Log.d(THIS_FILE, "CLOSE");
			if(prefWrapper.isValidConnectionForIncoming()) {
				//Alert user that we will disable for all incoming calls as he want to quit
				new AlertDialog.Builder(this)
					.setTitle(R.string.warning)
					.setMessage(getString(R.string.disconnect_and_incoming_explaination))
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							//prefWrapper.disableAllForIncoming();
							prefWrapper.setQuit(true);
							disconnectAndQuit();
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.show();
			}else {
				ArrayList<String> networks = prefWrapper.getAllIncomingNetworks();
				if (networks.size() > 0) {
					String msg = getString(R.string.disconnect_and_will_restart, TextUtils.join(", ", networks));
					Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
				}
				disconnectAndQuit();
			}
			return true;
		case HELP_MENU:
			startActivity(new Intent(this, Help.class));
			return true;
		case DISTRIB_ACCOUNT_MENU:
			WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
			DBAdapter db = new DBAdapter(this);
			db.open();
			account = db.getAccountForWizard(distribWizard.id);
			db.close();
			
			Intent it = new Intent(this, BasePrefsWizard.class);
			if(account.id != SipProfile.INVALID_ID) {
				it.putExtra(Intent.EXTRA_UID,  (int) account.id);
			}
			it.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
			startActivityForResult(it, REQUEST_EDIT_DISTRIBUTION_ACCOUNT);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void disconnectAndQuit() {
		Log.d(THIS_FILE, "True disconnection...");
		if (serviceIntent != null) {
			stopService(serviceIntent);
		}
		serviceIntent = null;
		finish();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PICKUP_PHONE:
			if(resultCode == RESULT_OK) {
				ContactsWrapper.getInstance().treatContactPickerPositiveResult(this, data, new OnPhoneNumberSelected() {
					@Override
					public void onTrigger(String number) {
						startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("sip", number, null)));
					}
				});
				return;
			}
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	
	
}
