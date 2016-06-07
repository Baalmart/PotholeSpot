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

import dev.potholespot.uganda.R;

public class MainActivity extends ActionBarActivity
{
   // DB Class to perform DB related operations..more like the database helper
   DBController controller = new DBController(this);
   // Progress Dialog Object
   ProgressDialog prgDialog;
   HashMap<String, String> queryValues;

   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main_sync);
      // Get User records from SQLite DB
      final ArrayList<HashMap<String, String>> userList = this.controller.getAllUsers();
      // If users exists in SQLite DB
      if (userList.size() != 0)
      {
         // Set the User Array list in ListView
         final ListAdapter adapter = new SimpleAdapter(MainActivity.this, userList, R.layout.view_user_entry, new String[] { "userId", "userName" }, new int[] { R.id.userId, R.id.userName });
         final ListView myList = (ListView) findViewById(android.R.id.list);
         myList.setAdapter(adapter);
      }
      // Initialize Progress Dialog properties
      this.prgDialog = new ProgressDialog(this);
      this.prgDialog.setMessage("Transferring Data from Remote MySQL DB and Syncing SQLite. Please wait...");
      this.prgDialog.setCancelable(false);
      // BroadCase Receiver Intent Object
      final Intent alarmIntent = new Intent(getApplicationContext(), SampleBC.class);
      // Pending Intent Object
      final PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      // Alarm Manager Object
      final AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
      // Alarm Manager calls BroadCast for every Ten seconds (10 * 1000), BroadCase further calls service to check if new records are inserted in 
      // Remote MySQL DB
      alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 5000, 10 * 1000, pendingIntent);
   }

   // Options Menu (ActionBar Menu)
   @Override
   public boolean onCreateOptionsMenu(final Menu menu)
   {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main_sync, menu);
      return true;
   }

   // When Options Menu is selected
   @Override
   public boolean onOptionsItemSelected(final MenuItem item)
   {
      // Handle action bar item clicks here. 
      final int id = item.getItemId();
      // When Sync action button is clicked
      if (id == R.id.refresh)
      {
         // Transfer data from remote MySQL DB to SQLite on Android and perform Sync
         syncSQLiteMySQLDB();
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   // Method to Sync MySQL to SQLite DB
   public void syncSQLiteMySQLDB()
   {
      // Create AsycHttpClient object
      final AsyncHttpClient client = new AsyncHttpClient();
      // Http Request Params Object
      final RequestParams params = new RequestParams();
      // Show ProgressBar
      this.prgDialog.show();
      // Make Http call to getusers.php
      client.post("http://192.168.2.4:9000/mysqlsqlitesync/getusers.php", params, new AsyncHttpResponseHandler()
         {
            @Override
            public void onSuccess(final String response)
            {
               // Hide ProgressBar
               MainActivity.this.prgDialog.hide();
               // Update SQLite DB with response sent by getusers.php
               updateSQLite(response);
            }

            // When error occured
            @Override
            public void onFailure(final int statusCode, final Throwable error, final String content)
            {
               // TODO Auto-generated method stub
               // Hide ProgressBar
               MainActivity.this.prgDialog.hide();
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
                  Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
               }
            }
         });
   }

   public void updateSQLite(final String response)
   {
      ArrayList<HashMap<String, String>> usersynclist;
      usersynclist = new ArrayList<HashMap<String, String>>();
      // Create GSON object
      final Gson gson = new GsonBuilder().create();
      try
      {
         // Extract JSON array from the response
         final JSONArray arr = new JSONArray(response);
         System.out.println(arr.length());
         // If no of array elements is not zero
         if (arr.length() != 0)
         {
            // Loop through each array element, get JSON object which has userid and username
            for (int i = 0; i < arr.length(); i++)
            {
               // Get JSON object
               final JSONObject obj = (JSONObject) arr.get(i);
               System.out.println(obj.get("userId"));
               System.out.println(obj.get("userName"));
               // DB QueryValues Object to insert into SQLite
               this.queryValues = new HashMap<String, String>();
               // Add userID extracted from Object
               this.queryValues.put("userId", obj.get("userId").toString());
               // Add userName extracted from Object
               this.queryValues.put("userName", obj.get("userName").toString());
               // Insert User into SQLite DB
               this.controller.insertUser(this.queryValues);
               final HashMap<String, String> map = new HashMap<String, String>();
               // Add status for each User in Hashmap
               map.put("Id", obj.get("userId").toString());
               map.put("status", "1");
               usersynclist.add(map);
            }
            // Inform Remote MySQL DB about the completion of Sync activity by passing Sync status of Users
            updateMySQLSyncSts(gson.toJson(usersynclist));
            // Reload the Main Activity
            reloadActivity();
         }
      }
      catch (final JSONException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   // Method to inform remote MySQL DB about completion of Sync activity
   public void updateMySQLSyncSts(final String json)
   {
      System.out.println(json);
      final AsyncHttpClient client = new AsyncHttpClient();
      final RequestParams params = new RequestParams();
      params.put("syncsts", json);
      // Make Http call to updatesyncsts.php with JSON parameter which has Sync statuses of Users
      client.post("http://192.168.2.4:9000/mysqlsqlitesync/updatesyncsts.php", params, new AsyncHttpResponseHandler()
         {
            @Override
            public void onSuccess(final String response)
            {
               Toast.makeText(getApplicationContext(), "MySQL DB has been informed about Sync activity", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(final int statusCode, final Throwable error, final String content)
            {
               Toast.makeText(getApplicationContext(), "Error Occured", Toast.LENGTH_LONG).show();
            }
         });
   }

   // Reload MainActivity
   public void reloadActivity()
   {
      final Intent objIntent = new Intent(getApplicationContext(), MainActivity.class);
      startActivity(objIntent);
   }
}
