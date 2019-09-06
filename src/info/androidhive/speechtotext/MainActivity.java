package info.androidhive.speechtotext;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

	private TextView txtSpeechInput;
	private ImageButton btnSpeak;
    private ProgressBar progressBar;
	private final int REQ_CODE_SPEECH_INPUT = 100;
	final static int RQS_1 = 1;
    List<Applications> applicationsArrayList = new ArrayList<Applications>();
    Map<String, String> contactList = new HashMap<String, String>();
    ApplicationTask applicationTask = new ApplicationTask();
    boolean done;


    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        done = false;
		txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
		btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(getBaseContext(),"Loading Resources. Please wait", Toast.LENGTH_LONG).show();

        applicationTask.execute();

		// hide the action bar
		getActionBar().hide();

		btnSpeak.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
                if(done)
				    promptSpeechInput();
                else
                    Toast.makeText(getBaseContext(),"Loading data.....Please wait",Toast.LENGTH_SHORT ).show();
			}
		});

	}

	/**
	 * Showing google speech input dialog
	 * */
	private void promptSpeechInput() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				getString(R.string.speech_prompt));
		try {
			startActivityForResult(intent, REQ_CODE_SPEECH_INPUT );
		} catch (ActivityNotFoundException a) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.speech_not_supported),
					Toast.LENGTH_SHORT).show();
		}
	}


	/**
	 * Receiving speech input
	 * */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQ_CODE_SPEECH_INPUT: {
			if (resultCode == RESULT_OK && null != data) {

				ArrayList<String> result = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				txtSpeechInput.setText(result.get(0));
				executeCommands(result.get(0).toUpperCase());
			}
			break;
		}

		}
	}

    private void executeCommands(String s){

        //Parse the commands
        String[] commands = s.split(" AND | THEN ");
        String[] executableCommands;

        //Select the first three if length>3
        if(commands.length>3){
            executableCommands = new String[3];
            for(int i=0; i<3; i++)
                executableCommands[i] = commands[i];
        }

        else {
            executableCommands = commands;
        }

        //Iterate through commands
        for(int i = executableCommands.length-1; i>=0; i--) {

            String command = executableCommands[i];
            //If the command starts with "then", remove it
            if(command.startsWith("THEN")){
                command = command.replace("THEN ","");
            }
            Log.e("COMMAND ", command);
            //Execute the command here
            handleCommand(command);
            //handleUnknown(command);
            //finish();
        }
    }

    private void handleCommand(String command) {

        if(command.contains("ALARM") || command.contains("WAKE ME UP") || command.contains("ALERT ME"))
            handleAlarm(command);

        else if(command.contains("REMIND ME"))
            handleReminder(command);

        else if(command.contains("CALL") || command.contains("DIAL"))
            handleCall(command);

        else if(command.contains("CONTACT") || command.contains("DETAILS"))
            handleContact(command);

        else if(command.contains("MESSAGE") || command.contains("TEXT"))
            handleMessage(command);

        else if(command.contains("OPEN") || command.contains("START"))
            handleApplication(command);

        else
            handleUnknown(command);

    }


    private void handleAlarm(String command){


		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, 1);
		cal.set(Calendar.YEAR, 2016);
		cal.set(Calendar.DAY_OF_MONTH, 24);
		cal.set(Calendar.HOUR_OF_DAY, 15);
		cal.set(Calendar.MINUTE, 30);

		setAlarm(cal);

    }

    private void handleReminder(String command) {



    }

    private void handleCall(String command) {

		String number = "";
		String contactName = "";
		//Get the number from contacts
		if(command.startsWith("CALL")){

			contactName = command.substring(command.indexOf(" ")+1);

		}
		else if(command.startsWith("PLACE A CALL TO ")){

            contactName = command.substring(16);

		}
		else if(command.startsWith("MAKE A CALL TO ")){

            contactName = command.substring(15);

		}
		else if(command.startsWith("DIAL THE NUMBER OF")){

            contactName = command.substring(19);

		}



        try {
            if(!isNumeric(contactName))
                number = contactList.get(contactName);
            else
                number = contactName;
            Log.e(contactName, number);


            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            startActivity(callIntent);
        }catch (NullPointerException e){
            Toast.makeText(getBaseContext(), "Contact Not Found", Toast.LENGTH_SHORT).show();
        }

    }



    private void handleContact(String command) {

    }

    private void handleMessage(String command) {

        String destination = "";
        String message = "";

        if(command.startsWith("SEND A MESSAGE TO ")){
            destination = command.substring(command.indexOf("TO")+3,command.indexOf("THAT")-1);
            message = command.substring(command.indexOf("THAT")+5);
        }
        else if(command.startsWith("MESSAGE")){
            destination = command.substring(command.indexOf(" ")+1, command.indexOf("THAT")-1);
            message = command.substring(command.indexOf("THAT")+5);
        }
        else if(command.startsWith("TEXT")){
            destination = command.substring(command.indexOf(" ")+1, command.indexOf("THAT")-1);
            message = command.substring(command.indexOf("THAT")+5);
        }
        else if(command.startsWith("SEND A TEXT TO")){
            destination = command.substring(command.indexOf("TO")+3,command.indexOf("THAT")-1);
            message = command.substring(command.indexOf("THAT")+5);
        }
        else if(command.startsWith("SEND A TEXT MESSAGE TO")){
            destination = command.substring(command.indexOf("TO")+3,command.indexOf("THAT")-1);
            message = command.substring(command.indexOf("THAT")+5);
        }
        else{
            handleUnknown(command);
        }

        //SmsManager smsManager = SmsManager.getDefault();
        //smsManager.sendTextMessage(contactList.get(destination),null,message,null,null);
        String number = contactList.get(destination);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, null));
        intent.putExtra("sms_body",message);
        startActivity(intent);
        //Toast.makeText(getBaseContext(),"Text Message sent to "+destination,Toast.LENGTH_SHORT).show();
    }

    private void handleApplication(String command) {

        String appName = "";
        if(command.startsWith("OPEN")){
            appName = command.substring(5).toLowerCase();
        }
        else if(command.startsWith("START")){
            appName = command.substring(6).toLowerCase();
        }
        else{
            handleUnknown(command);
        }
        openApplication(appName);

    }



    private void openApplication(String appName) {

        String packageName = null;

        // matching the package name with label name
        for (int i = 0; i < applicationsArrayList.size(); i++) {
            if (applicationsArrayList.get(i).labelName.trim().equals(
                    appName.trim())) {
                packageName = applicationsArrayList.get(i).packageName;
                break;
            }
        }

        // to launch the application
        Intent i;
        PackageManager manager = getPackageManager();
        try {
            i = manager.getLaunchIntentForPackage(packageName);
            if (i == null)
                throw new PackageManager.NameNotFoundException();
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(i);
        } catch (PackageManager.NameNotFoundException e) {

        }
    }

    private void handleUnknown(String command){
        final Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.setPackage("com.google.android.googlequicksearchbox");
        intent.putExtra(SearchManager.QUERY, command);
        startActivity(intent);
    }

    public static boolean isNumeric(String str)
    {
        return str.matches("[0123456789 ]+");
    }
	private void setAlarm(Calendar targetCal){
		Log.e("Alarm", "Handling Alarm");
		Intent intent = new Intent(this, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), RQS_1, intent, 0);
		AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, targetCal.getTimeInMillis(), pendingIntent);
	}




	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    private class ApplicationTask extends AsyncTask<Void, Void, Void>{


        @Override
        protected Void doInBackground(Void... voids) {
            applicationsArrayList.clear();
            getAllApps();
            getContacts();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            done = true;
            Toast.makeText(getBaseContext(),"Loading Complete", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.INVISIBLE);
        }

        private void getAllApps() {
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> activities = getPackageManager()
                    .queryIntentActivities(mainIntent, 0);
            for (ResolveInfo resolveInfo : activities) {
                Applications applications = new Applications();
                applications.labelName = resolveInfo.loadLabel(getPackageManager())
                        .toString().toLowerCase();
                applications.packageName = resolveInfo.activityInfo.packageName
                        .toString();
                applicationsArrayList.add(applications);
            }
        }

        private void getContacts() {
            contactList.clear();
            ContentResolver cr = getContentResolver();
            Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
            while (cursor.moveToNext()) {
                try{
                    String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name=cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)).toUpperCase();
                    String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                        Cursor phones = getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null);
                        while (phones.moveToNext()) {
                            String phoneNumber = phones.getString(phones.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER));
                            contactList.put(name,phoneNumber);
                        }
                        phones.close();
                    }
                }catch(Exception e){}
            }
        }

    }
}