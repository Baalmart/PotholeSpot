package dev.potholespot.android.db.sync;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import dev.potholespot.android.db.DatabaseHelper;
import dev.potholespot.uganda.R;

public class MainActivity extends ActionBarActivity 
{
	// DB Class to perform DB related operations..more like the database helper
	//DBController controller = new DBController(this);
	DatabaseHelper dbHelper = new DatabaseHelper(this);
	// Progress Dialog Object
	ProgressDialog prgDialog;
	HashMap<String, String> queryValues;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_sync);
		// Get User records from SQLite DB
		//ArrayList<HashMap<String, String>> userList = controller.getAllUsers();
		//getting label records from the database
		ArrayList<HashMap<String, String>> xyzList =  dbHelper.getAll_xyz();
		// If users exists in SQLite DB
		if (xyzList.size() != 0) 
		{
			// Set the labels Array list in ListView
			ListAdapter adapter = new SimpleAdapter
			      (MainActivity.this, xyzList, R.layout.view_user_entry, new String[] 
			            {"id", "time", "speed", "x", "y", "z" }, 
			            new int[] { R.id.id, R.id.time, R.id.speed, R.id.x, R.id.y, R.id.z});
			ListView myList = (ListView) findViewById(android.R.id.list);
			myList.setAdapter(adapter);
		}
		
		// Initialize Progress Dialog properties
		prgDialog = new ProgressDialog(this);
		prgDialog.setMessage("Transferring Data from Remote MySQL DB and Syncing SQLite. Please wait...");
		prgDialog.setCancelable(false);
		// BroadCase Receiver Intent Object
		Intent alarmIntent = new Intent(getApplicationContext(), SampleBC.class);
		// Pending Intent Object
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		// Alarm Manager Object
		AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		// Alarm Manager calls BroadCast for every Ten seconds (10 * 1000), BroadCase further calls service to check if new records are inserted in 
		// Remote MySQL DB
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 5000, 10 * 1000, pendingIntent);
	}
	
	// Options Menu (ActionBar Menu)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_sync, menu);
		return true;
	}

	// When Options Menu is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. 
		int id = item.getItemId();
		// When Sync action button is clicked
		if (id == R.id.refresh) {
			// Transfer data from remote MySQL DB to SQLite on Android and perform Sync
			syncSQLiteMySQLDB();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Method to Sync MySQL to SQLite DB
	public void syncSQLiteMySQLDB() {
		// Create AsycHttpClient object
		AsyncHttpClient client = new AsyncHttpClient();
		// Http Request Params Object
		RequestParams params = new RequestParams();
		// Show ProgressBar
		prgDialog.show();
		// Make Http call to getusers.php
		client.post("http://localhost/potholespot/web/mysqlsqlitesync/get_xyz.php", 
		      params, new AsyncHttpResponseHandler() 
		  {
				@Override
				public void onSuccess(String response) 
				{
					// Hide ProgressBar
					prgDialog.hide();
					// Update SQLite DB with response sent by getusers.php
					//updateSQLite(response);
				}
				// When error occured
				@Override
				public void onFailure(int statusCode, Throwable error, String content) 
				{
					// TODO Auto-generated method stub
					// Hide ProgressBar
					prgDialog.hide();
					if (statusCode == 404) 
					{
						Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
					} 
					else if (statusCode == 500) 
					{
						Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
					} 
					else 
					{
						Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]",
								Toast.LENGTH_LONG).show();
					}
				}
		});
	}
	
	//this method also takes advantage of the response and extracts a JSON array.
	public void updateSQLite(String response)
	{
		ArrayList<HashMap<String, String>> usersynclist;
		usersynclist = new ArrayList<HashMap<String, String>>();
		// Create GSON object
		Gson gson = new GsonBuilder().create();
		try {
			// Extract JSON array from the response
			JSONArray arr = new JSONArray(response);
			System.out.println(arr.length());
			// If no of array elements is not zero
			if(arr.length() != 0){
				// Loop through each array element, get JSON object which has the columns for x, y and z.
				for (int i = 0; i < arr.length(); i++) 
				{
					// Get JSON object
					JSONObject obj = (JSONObject) arr.get(i);
					System.out.println(obj.get("id"));
					System.out.println(obj.get("time"));
					System.out.println(obj.get("speed"));
					System.out.println(obj.get("x"));
					System.out.println(obj.get("y"));
					System.out.println(obj.get("z"));					
					
					// DB QueryValues Object to insert into SQLite
					queryValues = new HashMap<String, String>();
					
					
					// Add id extracted from Object
					queryValues.put("id", obj.get("id").toString());
					// Add time extracted from Object
					queryValues.put("time", obj.get("time").toString());
					 // Add speed extracted from Object
               queryValues.put("speed", obj.get("speed").toString());
               // Add x extracted from Object
               queryValues.put("x", obj.get("x").toString());
               // Add y extracted from Object
               queryValues.put("y", obj.get("y").toString());
               // Add z extracted from Object
               queryValues.put("z", obj.get("z").toString());
										
					// Insert values into SQLite DB
				  dbHelper.insert_xyz(queryValues);
					HashMap<String, String> map = new HashMap<String, String>();
					// Add status for each xyz in Hashmap
					map.put("Id", obj.get("userId").toString());
					map.put("time", obj.get("time").toString());
					map.put("speed", obj.get("speed").toString());
					map.put("x", obj.get("x").toString());
					map.put("y", obj.get("y").toString());
					map.put("z", obj.get("z").toString());
					map.put("status", "1");
					usersynclist.add(map);
				}
				// Inform Remote MySQL DB about the completion of Sync activity by passing Sync status of xyz values
				updateMySQLSyncSts(gson.toJson(usersynclist));
				// Reload the Main Activity
				reloadActivity();
			}
		} 
		
		catch (JSONException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Method to inform remote MySQL DB about completion of Sync activity
	public void updateMySQLSyncSts(String json) 
	{
		System.out.println(json);
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		params.put("syncsts", json);
		// Make Http call to updatesyncsts.php with JSON parameter which has Sync statuses of Users
		client.post("http://localhost/potholespot/web/mysqlsqlitesync/updatesyncsts.php", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				Toast.makeText(getApplicationContext(),	"MySQL DB has been informed about Sync activity", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onFailure(int statusCode, Throwable error, String content) {
					Toast.makeText(getApplicationContext(), "Error Occured", Toast.LENGTH_LONG).show();
			}
		});
	}
	
	// Reload MainActivity
	public void reloadActivity() 
	{
		Intent objIntent = new Intent(getApplicationContext(), MainActivity.class);
		startActivity(objIntent);
	}
}
