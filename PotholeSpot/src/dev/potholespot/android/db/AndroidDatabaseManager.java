//add your package name here example: package com.example.dbm;
package dev.potholespot.android.db;

//all required import files
import java.util.ArrayList;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidDatabaseManager extends Activity implements OnItemClickListener
{
   //a static class to save cursor,table values etc which is used by functions to share data in the program.
   static class indexInfo
   {
      public static int index = 10; //the items shown in the spinner for table names # size
      public static int numberofpages = 0;
      public static int currentpage = 0;
      public static String table_name = "";
      public static Cursor maincursor;
      public static int cursorpostion = 0;
      public static ArrayList<String> value_string;
      public static ArrayList<String> tableheadernames;
      public static ArrayList<String> emptytablecolumnnames;
      public static boolean isEmpty;
      public static boolean isCustomQuery;
   }

   // all global variables

   DatabaseHelper dbm;
   TableLayout tableLayout;
   TableRow.LayoutParams tableRowParams;
   HorizontalScrollView hsv;
   ScrollView mainscrollview;
   LinearLayout mainLayout;
   TextView tvmessage;
   Button previous;
   Button next;
   Spinner select_table;
   TextView tv;

   indexInfo info = new indexInfo();

   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      this.dbm = new DatabaseHelper(AndroidDatabaseManager.this);

      this.mainscrollview = new ScrollView(AndroidDatabaseManager.this);

      //the main linear layout to which all tables spinners etc will be added. In this activity every element is created dynamically  
      //to avoid using xml file
      this.mainLayout = new LinearLayout(AndroidDatabaseManager.this);
      this.mainLayout.setOrientation(LinearLayout.VERTICAL);
      this.mainLayout.setBackgroundColor(Color.WHITE);
      this.mainLayout.setScrollContainer(true);
      this.mainscrollview.addView(this.mainLayout);

      //all required layouts are created dynamically and added to the main scrollview
      setContentView(this.mainscrollview);

      //the first row of layout which has a text view and spinner
      final LinearLayout firstrow = new LinearLayout(AndroidDatabaseManager.this);
      firstrow.setPadding(0, 10, 0, 20);
      final LinearLayout.LayoutParams firstrowlp = new LinearLayout.LayoutParams(0, 150);
      firstrowlp.weight = 1;

      final TextView maintext = new TextView(AndroidDatabaseManager.this);
      maintext.setText("Select Table");
      maintext.setTextSize(22);
      maintext.setLayoutParams(firstrowlp);
      this.select_table = new Spinner(AndroidDatabaseManager.this);
      this.select_table.setLayoutParams(firstrowlp);

      firstrow.addView(maintext);
      firstrow.addView(this.select_table);
      this.mainLayout.addView(firstrow);

      ArrayList<Cursor> alc;

      //the horizontal scroll view for table if the table content does not fit into screen
      this.hsv = new HorizontalScrollView(AndroidDatabaseManager.this);

      //the main table layout where the content of the sql tables will be displayed when user selects a table	
      this.tableLayout = new TableLayout(AndroidDatabaseManager.this);
      this.tableLayout.setHorizontalScrollBarEnabled(true);
      this.hsv.addView(this.tableLayout);

      //the second row of the layout which shows number of records in the table selected by user
      final LinearLayout secondrow = new LinearLayout(AndroidDatabaseManager.this);
      secondrow.setPadding(0, 20, 0, 10);
      final LinearLayout.LayoutParams secondrowlp = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
      secondrowlp.weight = 1;
      final TextView secondrowtext = new TextView(AndroidDatabaseManager.this);
      secondrowtext.setText("No. Of Records : ");
      secondrowtext.setTextSize(20);
      secondrowtext.setLayoutParams(secondrowlp);
      this.tv = new TextView(AndroidDatabaseManager.this);
      this.tv.setTextSize(20);
      this.tv.setLayoutParams(secondrowlp);
      secondrow.addView(secondrowtext);
      secondrow.addView(this.tv);
      this.mainLayout.addView(secondrow);
      //A button which generates a text view from which user can write custome queries
      final EditText customquerytext = new EditText(this);
      customquerytext.setVisibility(View.GONE);
      customquerytext.setHint("Enter Your Query here and Click on Submit Query Button .Results will be displayed below");
      this.mainLayout.addView(customquerytext);

      final Button submitQuery = new Button(AndroidDatabaseManager.this);
      submitQuery.setVisibility(View.GONE);
      submitQuery.setText("Submit Query");

      submitQuery.setBackgroundColor(Color.parseColor("#BAE7F6"));
      this.mainLayout.addView(submitQuery);

      final TextView help = new TextView(AndroidDatabaseManager.this);
      help.setText("Click on the row below to update values or delete the tuple");
      help.setPadding(0, 5, 0, 5);

      // the spinner which gives user a option to add new row , drop or delete table
      final Spinner spinnertable = new Spinner(AndroidDatabaseManager.this);
      this.mainLayout.addView(spinnertable);
      this.mainLayout.addView(help);
      this.hsv.setPadding(0, 10, 0, 10);
      this.hsv.setScrollbarFadingEnabled(false);
      this.hsv.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
      this.mainLayout.addView(this.hsv);
      //the third layout which has buttons for the pagination of content from database
      final LinearLayout thirdrow = new LinearLayout(AndroidDatabaseManager.this);
      this.previous = new Button(AndroidDatabaseManager.this);
      this.previous.setText("Previous");

      this.previous.setBackgroundColor(Color.parseColor("#BAE7F6"));
      this.previous.setLayoutParams(secondrowlp);
      this.next = new Button(AndroidDatabaseManager.this);
      this.next.setText("Next");
      this.next.setBackgroundColor(Color.parseColor("#BAE7F6"));
      this.next.setLayoutParams(secondrowlp);
      final TextView tvblank = new TextView(this);
      tvblank.setLayoutParams(secondrowlp);
      thirdrow.setPadding(0, 10, 0, 10);
      thirdrow.addView(this.previous);
      thirdrow.addView(tvblank);
      thirdrow.addView(this.next);
      this.mainLayout.addView(thirdrow);

      //the text view at the bottom of the screen which displays error or success messages after a query is executed
      this.tvmessage = new TextView(AndroidDatabaseManager.this);

      this.tvmessage.setText("Error Messages will be displayed here");
      final String Query = "SELECT name _id FROM sqlite_master WHERE type ='table'";
      this.tvmessage.setTextSize(18);
      this.mainLayout.addView(this.tvmessage);

      final Button customQuery = new Button(AndroidDatabaseManager.this);
      customQuery.setText("Custom Query");
      customQuery.setBackgroundColor(Color.parseColor("#BAE7F6"));
      this.mainLayout.addView(customQuery);
      customQuery.setOnClickListener(new OnClickListener()
         {

            @Override
            public void onClick(final View v)
            {
               //set drop down to custom Query 
               indexInfo.isCustomQuery = true;
               secondrow.setVisibility(View.GONE);
               spinnertable.setVisibility(View.GONE);
               help.setVisibility(View.GONE);
               customquerytext.setVisibility(View.VISIBLE);
               submitQuery.setVisibility(View.VISIBLE);
               AndroidDatabaseManager.this.select_table.setSelection(0);
               customQuery.setVisibility(View.GONE);
            }
         });

      //when user enter a custom query in text view and clicks on submit query button
      //display results in tablelayout
      submitQuery.setOnClickListener(new OnClickListener()
         {
            @Override
            public void onClick(final View v)
            {
               AndroidDatabaseManager.this.tableLayout.removeAllViews();
               customQuery.setVisibility(View.GONE);

               ArrayList<Cursor> alc2;
               final String Query10 = customquerytext.getText().toString();
               Log.d("query", Query10);
               //pass the query to getdata method and get results
               alc2 = AndroidDatabaseManager.this.dbm.getData(Query10);
               final Cursor c4 = alc2.get(0);
               final Cursor Message2 = alc2.get(1);
               Message2.moveToLast();

               //if the query returns results display the results in table layout
               if (Message2.getString(0).equalsIgnoreCase("Success"))
               {

                  AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#2ecc71"));
                  if (c4 != null)
                  {
                     AndroidDatabaseManager.this.tvmessage.setText("Queru Executed successfully.Number of rows returned :" + c4.getCount());
                     if (c4.getCount() > 0)
                     {
                        indexInfo.maincursor = c4;
                        refreshTable(1);
                     }
                  }
                  else
                  {
                     AndroidDatabaseManager.this.tvmessage.setText("Queru Executed successfully");
                     refreshTable(1);
                  }

               }
               else
               {
                  //if there is any error we displayed the error message at the bottom of the screen	
                  AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#e74c3c"));
                  AndroidDatabaseManager.this.tvmessage.setText("Error:" + Message2.getString(0));

               }
            }
         });

      //layout parameters for each row in the table
      this.tableRowParams = new TableRow.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
      this.tableRowParams.setMargins(0, 0, 2, 0);

      // a query which returns a cursor with the list of tables in the database.We use this cursor to populate spinner in the first row
      alc = this.dbm.getData(Query);

      //the first cursor has reults of the query
      final Cursor c = alc.get(0);

      //the second cursor has error messages
      final Cursor Message = alc.get(1);

      Message.moveToLast();
      final String msg = Message.getString(0);
      Log.d("Message from sql = ", msg);

      final ArrayList<String> tablenames = new ArrayList<String>();

      if (c != null)
      {

         c.moveToFirst();
         tablenames.add("click here");
         do
         {
            //add names of the table to tablenames array list
            tablenames.add(c.getString(0));
         }
         while (c.moveToNext());
      }
      //an array adapter with above created arraylist
      final ArrayAdapter<String> tablenamesadapter = new ArrayAdapter<String>(AndroidDatabaseManager.this, android.R.layout.simple_spinner_item, tablenames)
         {

            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent)
            {
               final View v = super.getView(position, convertView, parent);

               v.setBackgroundColor(Color.WHITE);
               final TextView adap = (TextView) v;
               adap.setTextSize(20);
               return adap;
            }

            @Override
            public View getDropDownView(final int position, final View convertView, final ViewGroup parent)
            {
               final View v = super.getDropDownView(position, convertView, parent);

               v.setBackgroundColor(Color.WHITE);

               return v;
            }
         };

      tablenamesadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

      if (tablenamesadapter != null)
      {
         //set the adpater to select_table spinner
         this.select_table.setAdapter(tablenamesadapter);
      }

      // when a table names is selecte display the table contents
      this.select_table.setOnItemSelectedListener(new OnItemSelectedListener()
         {

            @Override
            public void onItemSelected(final AdapterView< ? > parent, final View view, final int pos, final long id)
            {
               if (pos == 0 && !indexInfo.isCustomQuery)
               {
                  secondrow.setVisibility(View.GONE);
                  AndroidDatabaseManager.this.hsv.setVisibility(View.GONE);
                  thirdrow.setVisibility(View.GONE);
                  spinnertable.setVisibility(View.GONE);
                  help.setVisibility(View.GONE);
                  AndroidDatabaseManager.this.tvmessage.setVisibility(View.GONE);
                  customquerytext.setVisibility(View.GONE);
                  submitQuery.setVisibility(View.GONE);
                  customQuery.setVisibility(View.GONE);
               }
               if (pos != 0)
               {
                  secondrow.setVisibility(View.VISIBLE);
                  spinnertable.setVisibility(View.VISIBLE);
                  help.setVisibility(View.VISIBLE);
                  customquerytext.setVisibility(View.GONE);
                  submitQuery.setVisibility(View.GONE);
                  customQuery.setVisibility(View.VISIBLE);
                  AndroidDatabaseManager.this.hsv.setVisibility(View.VISIBLE);

                  AndroidDatabaseManager.this.tvmessage.setVisibility(View.VISIBLE);

                  thirdrow.setVisibility(View.VISIBLE);
                  c.moveToPosition(pos - 1);
                  indexInfo.cursorpostion = pos - 1;
                  //displaying the content of the table which is selected in the select_table spinner
                  Log.d("selected table name is", "" + c.getString(0));
                  indexInfo.table_name = c.getString(0);
                  AndroidDatabaseManager.this.tvmessage.setText("Error Messages will be displayed here");
                  AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.WHITE);

                  //removes any data if present in the table layout
                  AndroidDatabaseManager.this.tableLayout.removeAllViews();
                  final ArrayList<String> spinnertablevalues = new ArrayList<String>();
                  spinnertablevalues.add("Click here to change this table");
                  spinnertablevalues.add("Add row to this table");
                  spinnertablevalues.add("Delete this table");
                  spinnertablevalues.add("Drop this table");
                  final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, spinnertablevalues);
                  spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);

                  // a array adapter which add values to the spinner which helps in user making changes to the table
                  final ArrayAdapter<String> adapter = new ArrayAdapter<String>(AndroidDatabaseManager.this, android.R.layout.simple_spinner_item, spinnertablevalues)
                     {

                        @Override
                        public View getView(final int position, final View convertView, final ViewGroup parent)
                        {
                           final View v = super.getView(position, convertView, parent);

                           v.setBackgroundColor(Color.WHITE);
                           final TextView adap = (TextView) v;
                           adap.setTextSize(20);

                           return adap;
                        }

                        @Override
                        public View getDropDownView(final int position, final View convertView, final ViewGroup parent)
                        {
                           final View v = super.getDropDownView(position, convertView, parent);

                           v.setBackgroundColor(Color.WHITE);

                           return v;
                        }
                     };

                  adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                  spinnertable.setAdapter(adapter);
                  final String Query2 = "select * from " + c.getString(0);
                  Log.d("", "" + Query2);

                  //getting contents of the table which user selected from the select_table spinner
                  final ArrayList<Cursor> alc2 = AndroidDatabaseManager.this.dbm.getData(Query2);
                  final Cursor c2 = alc2.get(0);
                  //saving cursor to the static indexinfo class which can be resued by the other functions
                  indexInfo.maincursor = c2;

                  // if the cursor returned form the database is not null we display the data in table layout
                  if (c2 != null)
                  {
                     final int counts = c2.getCount();
                     indexInfo.isEmpty = false;
                     Log.d("counts", "" + counts);
                     AndroidDatabaseManager.this.tv.setText("" + counts);

                     //the spinnertable has the 3 items to drop , delete , add row to the table selected by the user
                     //here we handle the 3 operations.
                     spinnertable.setOnItemSelectedListener((new AdapterView.OnItemSelectedListener()
                        {
                           @Override
                           public void onItemSelected(final AdapterView< ? > parentView, final View selectedItemView, final int position, final long id)
                           {

                              ((TextView) parentView.getChildAt(0)).setTextColor(Color.rgb(0, 0, 0));
                              //when user selects to drop the table the below code in if block will be executed
                              if (spinnertable.getSelectedItem().toString().equals("Drop this table"))
                              {
                                 // an alert dialog to confirm user selection
                                 runOnUiThread(new Runnable()
                                    {
                                       @Override
                                       public void run()
                                       {
                                          if (!isFinishing())
                                          {

                                             new AlertDialog.Builder(AndroidDatabaseManager.this).setTitle("Are you sure ?")
                                                   .setMessage("Pressing yes will remove " + indexInfo.table_name + " table from database")
                                                   .setPositiveButton("yes", new DialogInterface.OnClickListener()
                                                      {
                                                         // when user confirms by clicking on yes we drop the table by executing drop table query 	
                                                         @Override
                                                         public void onClick(final DialogInterface dialog, final int which)
                                                         {

                                                            final String Query6 = "Drop table " + indexInfo.table_name;
                                                            final ArrayList<Cursor> aldropt = AndroidDatabaseManager.this.dbm.getData(Query6);
                                                            final Cursor tempc = aldropt.get(1);
                                                            tempc.moveToLast();
                                                            Log.d("Drop table Mesage", tempc.getString(0));
                                                            if (tempc.getString(0).equalsIgnoreCase("Success"))
                                                            {
                                                               AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#2ecc71"));
                                                               AndroidDatabaseManager.this.tvmessage.setText(indexInfo.table_name + "Dropped successfully");
                                                               refreshactivity();
                                                            }
                                                            else
                                                            {
                                                               //if there is any error we displayd the error message at the bottom of the screen	
                                                               AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#e74c3c"));
                                                               AndroidDatabaseManager.this.tvmessage.setText("Error:" + tempc.getString(0));
                                                               spinnertable.setSelection(0);
                                                            }
                                                         }
                                                      }).setNegativeButton("No", new DialogInterface.OnClickListener()
                                                      {
                                                         @Override
                                                         public void onClick(final DialogInterface dialog, final int which)
                                                         {
                                                            spinnertable.setSelection(0);
                                                         }
                                                      }).create().show();
                                          }
                                       }
                                    });

                              }
                              //when user selects to drop the table the below code in if block will be executed
                              if (spinnertable.getSelectedItem().toString().equals("Delete this table"))
                              { // an alert dialog to confirm user selection
                                 runOnUiThread(new Runnable()
                                    {
                                       @Override
                                       public void run()
                                       {
                                          if (!isFinishing())
                                          {

                                             new AlertDialog.Builder(AndroidDatabaseManager.this).setTitle("Are you sure?")
                                                   .setMessage("Clicking on yes will delete all the contents of " + indexInfo.table_name + " table from database")
                                                   .setPositiveButton("yes", new DialogInterface.OnClickListener()
                                                      {

                                                         // when user confirms by clicking on yes we drop the table by executing delete table query 
                                                         @Override
                                                         public void onClick(final DialogInterface dialog, final int which)
                                                         {
                                                            final String Query7 = "Delete  from " + indexInfo.table_name;
                                                            Log.d("delete table query", Query7);
                                                            final ArrayList<Cursor> aldeletet = AndroidDatabaseManager.this.dbm.getData(Query7);
                                                            final Cursor tempc = aldeletet.get(1);
                                                            tempc.moveToLast();
                                                            Log.d("Delete table Mesage", tempc.getString(0));
                                                            if (tempc.getString(0).equalsIgnoreCase("Success"))
                                                            {
                                                               AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#2ecc71"));
                                                               AndroidDatabaseManager.this.tvmessage.setText(indexInfo.table_name + " table content deleted successfully");
                                                               indexInfo.isEmpty = true;
                                                               refreshTable(0);
                                                            }
                                                            else
                                                            {
                                                               AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#e74c3c"));
                                                               AndroidDatabaseManager.this.tvmessage.setText("Error:" + tempc.getString(0));
                                                               spinnertable.setSelection(0);
                                                            }
                                                         }
                                                      }).setNegativeButton("No", new DialogInterface.OnClickListener()
                                                      {
                                                         @Override
                                                         public void onClick(final DialogInterface dialog, final int which)
                                                         {
                                                            spinnertable.setSelection(0);
                                                         }
                                                      }).create().show();
                                          }
                                       }
                                    });

                              }

                              //when user selects to add row to the table the below code in if block will be executed
                              if (spinnertable.getSelectedItem().toString().equals("Add row to this table"))
                              {
                                 //we create a layout which has textviews with column names of the table and edittexts where
                                 //user can enter value which will be inserted into the datbase.
                                 final LinkedList<TextView> addnewrownames = new LinkedList<TextView>();
                                 final LinkedList<EditText> addnewrowvalues = new LinkedList<EditText>();
                                 final ScrollView addrowsv = new ScrollView(AndroidDatabaseManager.this);
                                 final Cursor c4 = indexInfo.maincursor;
                                 if (indexInfo.isEmpty)
                                 {
                                    getcolumnnames();
                                    for (int i = 0; i < indexInfo.emptytablecolumnnames.size(); i++)
                                    {
                                       final String cname = indexInfo.emptytablecolumnnames.get(i);
                                       final TextView tv = new TextView(getApplicationContext());
                                       tv.setText(cname);
                                       addnewrownames.add(tv);

                                    }
                                    for (int i = 0; i < addnewrownames.size(); i++)
                                    {
                                       final EditText et = new EditText(getApplicationContext());

                                       addnewrowvalues.add(et);
                                    }

                                 }
                                 else
                                 {
                                    for (int i = 0; i < c4.getColumnCount(); i++)
                                    {
                                       final String cname = c4.getColumnName(i);
                                       final TextView tv = new TextView(getApplicationContext());
                                       tv.setText(cname);
                                       addnewrownames.add(tv);

                                    }
                                    for (int i = 0; i < addnewrownames.size(); i++)
                                    {
                                       final EditText et = new EditText(getApplicationContext());

                                       addnewrowvalues.add(et);
                                    }
                                 }
                                 final RelativeLayout addnewlayout = new RelativeLayout(AndroidDatabaseManager.this);
                                 final RelativeLayout.LayoutParams addnewparams = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                       android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                                 addnewparams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                                 for (int i = 0; i < addnewrownames.size(); i++)
                                 {
                                    final TextView tv = addnewrownames.get(i);
                                    final EditText et = addnewrowvalues.get(i);
                                    final int t = i + 400;
                                    final int k = i + 500;
                                    final int lid = i + 600;

                                    tv.setId(t);
                                    tv.setTextColor(Color.parseColor("#000000"));
                                    et.setBackgroundColor(Color.parseColor("#F2F2F2"));
                                    et.setTextColor(Color.parseColor("#000000"));
                                    et.setId(k);
                                    final LinearLayout ll = new LinearLayout(AndroidDatabaseManager.this);
                                    final LinearLayout.LayoutParams tvl = new LinearLayout.LayoutParams(0, 100);
                                    tvl.weight = 1;
                                    ll.addView(tv, tvl);
                                    ll.addView(et, tvl);
                                    ll.setId(lid);

                                    Log.d("Edit Text Value", "" + et.getText().toString());

                                    final RelativeLayout.LayoutParams rll = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                          android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                                    rll.addRule(RelativeLayout.BELOW, ll.getId() - 1);
                                    rll.setMargins(0, 20, 0, 0);
                                    addnewlayout.addView(ll, rll);

                                 }
                                 addnewlayout.setBackgroundColor(Color.WHITE);
                                 addrowsv.addView(addnewlayout);
                                 Log.d("Button Clicked", "");
                                 //the above form layout which we have created above will be displayed in an alert dialog
                                 runOnUiThread(new Runnable()
                                    {
                                       @Override
                                       public void run()
                                       {
                                          if (!isFinishing())
                                          {
                                             new AlertDialog.Builder(AndroidDatabaseManager.this).setTitle("values").setCancelable(false).setView(addrowsv)
                                                   .setPositiveButton("Add", new DialogInterface.OnClickListener()
                                                      {
                                                         // after entering values if user clicks on add we take the values and run a insert query
                                                         @Override
                                                         public void onClick(final DialogInterface dialog, final int which)
                                                         {

                                                            indexInfo.index = 10;
                                                            //tableLayout.removeAllViews();
                                                            //trigger select table listener to be triggerd
                                                            String Query4 = "Insert into " + indexInfo.table_name + " (";
                                                            for (int i = 0; i < addnewrownames.size(); i++)
                                                            {
                                                               final TextView tv = addnewrownames.get(i);
                                                               tv.getText().toString();
                                                               if (i == addnewrownames.size() - 1)
                                                               {
                                                                  Query4 = Query4 + tv.getText().toString();
                                                               }
                                                               else
                                                               {
                                                                  Query4 = Query4 + tv.getText().toString() + ", ";
                                                               }
                                                            }
                                                            Query4 = Query4 + " ) VALUES ( ";
                                                            for (int i = 0; i < addnewrownames.size(); i++)
                                                            {
                                                               final EditText et = addnewrowvalues.get(i);
                                                               et.getText().toString();

                                                               if (i == addnewrownames.size() - 1)
                                                               {

                                                                  Query4 = Query4 + "'" + et.getText().toString() + "' ) ";
                                                               }
                                                               else
                                                               {
                                                                  Query4 = Query4 + "'" + et.getText().toString() + "' , ";
                                                               }

                                                            }
                                                            //this is the insert query which has been generated
                                                            Log.d("Insert Query", Query4);
                                                            final ArrayList<Cursor> altc = AndroidDatabaseManager.this.dbm.getData(Query4);
                                                            final Cursor tempc = altc.get(1);
                                                            tempc.moveToLast();
                                                            Log.d("Add New Row", tempc.getString(0));
                                                            if (tempc.getString(0).equalsIgnoreCase("Success"))
                                                            {
                                                               AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#2ecc71"));
                                                               AndroidDatabaseManager.this.tvmessage.setText("New Row added succesfully to " + indexInfo.table_name);
                                                               refreshTable(0);
                                                            }
                                                            else
                                                            {
                                                               AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#e74c3c"));
                                                               AndroidDatabaseManager.this.tvmessage.setText("Error:" + tempc.getString(0));
                                                               spinnertable.setSelection(0);
                                                            }

                                                         }
                                                      }).setNegativeButton("close", new DialogInterface.OnClickListener()
                                                      {
                                                         @Override
                                                         public void onClick(final DialogInterface dialog, final int which)
                                                         {
                                                            spinnertable.setSelection(0);
                                                         }
                                                      }).create().show();
                                          }
                                       }
                                    });
                              }
                           }

                           @Override
                           public void onNothingSelected(final AdapterView< ? > arg0)
                           {
                           }
                        }));

                     //display the first row of the table with column names of the table selected by the user
                     final TableRow tableheader = new TableRow(getApplicationContext());

                     tableheader.setBackgroundColor(Color.BLACK);
                     tableheader.setPadding(0, 2, 0, 2);
                     for (int k = 0; k < c2.getColumnCount(); k++)
                     {
                        final LinearLayout cell = new LinearLayout(AndroidDatabaseManager.this);
                        cell.setBackgroundColor(Color.WHITE);
                        cell.setLayoutParams(AndroidDatabaseManager.this.tableRowParams);
                        final TextView tableheadercolums = new TextView(getApplicationContext());
                        // tableheadercolums.setBackgroundDrawable(gd);
                        tableheadercolums.setPadding(0, 0, 4, 3);
                        tableheadercolums.setText("" + c2.getColumnName(k));
                        tableheadercolums.setTextColor(Color.parseColor("#000000"));

                        //columsView.setLayoutParams(tableRowParams);
                        cell.addView(tableheadercolums);
                        tableheader.addView(cell);

                     }
                     AndroidDatabaseManager.this.tableLayout.addView(tableheader);
                     c2.moveToFirst();

                     //after displaying columnnames in the first row  we display data in the remaining columns
                     //the below paginatetbale function will display the first 10 tuples of the tables
                     //the remaining tuples can be viewed by clicking on the next button
                     paginatetable(c2.getCount());

                  }
                  else
                  {
                     //if the cursor returned from the database is empty we show that table is empty 	
                     help.setVisibility(View.GONE);
                     AndroidDatabaseManager.this.tableLayout.removeAllViews();
                     getcolumnnames();
                     final TableRow tableheader2 = new TableRow(getApplicationContext());
                     tableheader2.setBackgroundColor(Color.BLACK);
                     tableheader2.setPadding(0, 2, 0, 2);

                     final LinearLayout cell = new LinearLayout(AndroidDatabaseManager.this);
                     cell.setBackgroundColor(Color.WHITE);
                     cell.setLayoutParams(AndroidDatabaseManager.this.tableRowParams);
                     final TextView tableheadercolums = new TextView(getApplicationContext());

                     tableheadercolums.setPadding(0, 0, 4, 3);
                     tableheadercolums.setText("   Table   Is   Empty   ");
                     tableheadercolums.setTextSize(30);
                     tableheadercolums.setTextColor(Color.RED);

                     cell.addView(tableheadercolums);
                     tableheader2.addView(cell);

                     AndroidDatabaseManager.this.tableLayout.addView(tableheader2);

                     AndroidDatabaseManager.this.tv.setText("" + 0);
                  }
               }
            }

            @Override
            public void onNothingSelected(final AdapterView< ? > arg0)
            {
            }
         });
   }

   //get columnnames of the empty tables and save them in a array list
   public void getcolumnnames()
   {
      final ArrayList<Cursor> alc3 = this.dbm.getData("PRAGMA table_info(" + indexInfo.table_name + ")");
      final Cursor c5 = alc3.get(0);
      indexInfo.isEmpty = true;
      if (c5 != null)
      {
         indexInfo.isEmpty = true;

         final ArrayList<String> emptytablecolumnnames = new ArrayList<String>();
         c5.moveToFirst();
         do
         {
            emptytablecolumnnames.add(c5.getString(1));
         }
         while (c5.moveToNext());
         indexInfo.emptytablecolumnnames = emptytablecolumnnames;
      }

   }

   //displays alert dialog from which use can update or delete a row 
   public void updateDeletePopup(final int row)
   {
      final Cursor c2 = indexInfo.maincursor;
      // a spinner which gives options to update or delete the row which user has selected
      final ArrayList<String> spinnerArray = new ArrayList<String>();
      spinnerArray.add("Click Here to Change this row");
      spinnerArray.add("Update this row");
      spinnerArray.add("Delete this row");

      //create a layout with text values which has the column names and 
      //edit texts which has the values of the row which user has selected
      final ArrayList<String> value_string = indexInfo.value_string;
      final LinkedList<TextView> columnames = new LinkedList<TextView>();
      final LinkedList<EditText> columvalues = new LinkedList<EditText>();

      for (int i = 0; i < c2.getColumnCount(); i++)
      {
         final String cname = c2.getColumnName(i);
         final TextView tv = new TextView(getApplicationContext());
         tv.setText(cname);
         columnames.add(tv);

      }
      for (int i = 0; i < columnames.size(); i++)
      {
         final String cv = value_string.get(i);
         final EditText et = new EditText(getApplicationContext());
         value_string.add(cv);
         et.setText(cv);
         columvalues.add(et);
      }

      int lastrid = 0;
      // all text views , edit texts are added to this relative layout lp
      final RelativeLayout lp = new RelativeLayout(AndroidDatabaseManager.this);
      lp.setBackgroundColor(Color.WHITE);
      final RelativeLayout.LayoutParams lay = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
      lay.addRule(RelativeLayout.ALIGN_PARENT_TOP);

      final ScrollView updaterowsv = new ScrollView(AndroidDatabaseManager.this);
      final LinearLayout lcrud = new LinearLayout(AndroidDatabaseManager.this);

      final LinearLayout.LayoutParams paramcrudtext = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

      paramcrudtext.setMargins(0, 20, 0, 0);

      //spinner which displays update , delete options
      final Spinner crud_dropdown = new Spinner(getApplicationContext());

      final ArrayAdapter<String> crudadapter = new ArrayAdapter<String>(AndroidDatabaseManager.this, android.R.layout.simple_spinner_item, spinnerArray)
         {

            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent)
            {
               final View v = super.getView(position, convertView, parent);

               v.setBackgroundColor(Color.WHITE);
               final TextView adap = (TextView) v;
               adap.setTextSize(20);

               return adap;
            }

            @Override
            public View getDropDownView(final int position, final View convertView, final ViewGroup parent)
            {
               final View v = super.getDropDownView(position, convertView, parent);

               v.setBackgroundColor(Color.WHITE);

               return v;
            }
         };

      crudadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

      crud_dropdown.setAdapter(crudadapter);
      lcrud.setId(299);
      lcrud.addView(crud_dropdown, paramcrudtext);

      final RelativeLayout.LayoutParams rlcrudparam = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
      rlcrudparam.addRule(RelativeLayout.BELOW, lastrid);

      lp.addView(lcrud, rlcrudparam);
      for (int i = 0; i < columnames.size(); i++)
      {
         final TextView tv = columnames.get(i);
         final EditText et = columvalues.get(i);
         final int t = i + 100;
         final int k = i + 200;
         final int lid = i + 300;

         tv.setId(t);
         tv.setTextColor(Color.parseColor("#000000"));
         et.setBackgroundColor(Color.parseColor("#F2F2F2"));

         et.setTextColor(Color.parseColor("#000000"));
         et.setId(k);
         Log.d("text View Value", "" + tv.getText().toString());
         final LinearLayout ll = new LinearLayout(AndroidDatabaseManager.this);
         ll.setBackgroundColor(Color.parseColor("#FFFFFF"));
         ll.setId(lid);
         final LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(0, 100);
         lpp.weight = 1;
         tv.setLayoutParams(lpp);
         et.setLayoutParams(lpp);
         ll.addView(tv);
         ll.addView(et);

         Log.d("Edit Text Value", "" + et.getText().toString());

         final RelativeLayout.LayoutParams rll = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
         rll.addRule(RelativeLayout.BELOW, ll.getId() - 1);
         rll.setMargins(0, 20, 0, 0);
         lastrid = ll.getId();
         lp.addView(ll, rll);

      }

      updaterowsv.addView(lp);
      //after the layout has been created display it in a alert dialog  
      runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               if (!isFinishing())
               {
                  new AlertDialog.Builder(AndroidDatabaseManager.this).setTitle("values").setView(updaterowsv).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener()
                     {

                        //this code will be executed when user changes values of edit text or spinner and clicks on ok button	
                        @Override
                        public void onClick(final DialogInterface dialog, final int which)
                        {

                           //get spinner value
                           final String spinner_value = crud_dropdown.getSelectedItem().toString();

                           //it he spinner value is update this row get the values from 
                           //edit text fields generate a update query and execute it
                           if (spinner_value.equalsIgnoreCase("Update this row"))
                           {
                              indexInfo.index = 10;
                              String Query3 = "UPDATE " + indexInfo.table_name + " SET ";

                              for (int i = 0; i < columnames.size(); i++)
                              {
                                 final TextView tvc = columnames.get(i);
                                 final EditText etc = columvalues.get(i);

                                 if (!etc.getText().toString().equals("null"))
                                 {

                                    Query3 = Query3 + tvc.getText().toString() + " = ";

                                    if (i == columnames.size() - 1)
                                    {

                                       Query3 = Query3 + "'" + etc.getText().toString() + "'";

                                    }
                                    else
                                    {

                                       Query3 = Query3 + "'" + etc.getText().toString() + "' , ";

                                    }
                                 }

                              }
                              Query3 = Query3 + " where ";
                              for (int i = 0; i < columnames.size(); i++)
                              {
                                 final TextView tvc = columnames.get(i);
                                 if (!value_string.get(i).equals("null"))
                                 {

                                    Query3 = Query3 + tvc.getText().toString() + " = ";

                                    if (i == columnames.size() - 1)
                                    {

                                       Query3 = Query3 + "'" + value_string.get(i) + "' ";

                                    }
                                    else
                                    {
                                       Query3 = Query3 + "'" + value_string.get(i) + "' and ";
                                    }

                                 }
                              }
                              Log.d("Update Query", Query3);
                              //dbm.getData(Query3);
                              final ArrayList<Cursor> aluc = AndroidDatabaseManager.this.dbm.getData(Query3);
                              final Cursor tempc = aluc.get(1);
                              tempc.moveToLast();
                              Log.d("Update Mesage", tempc.getString(0));

                              if (tempc.getString(0).equalsIgnoreCase("Success"))
                              {
                                 AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#2ecc71"));
                                 AndroidDatabaseManager.this.tvmessage.setText(indexInfo.table_name + " table Updated Successfully");
                                 refreshTable(0);
                              }
                              else
                              {
                                 AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#e74c3c"));
                                 AndroidDatabaseManager.this.tvmessage.setText("Error:" + tempc.getString(0));
                              }
                           }
                           //it he spinner value is delete this row get the values from 
                           //edit text fields generate a delete query and execute it

                           if (spinner_value.equalsIgnoreCase("Delete this row"))
                           {

                              indexInfo.index = 10;
                              String Query5 = "DELETE FROM " + indexInfo.table_name + " WHERE ";

                              for (int i = 0; i < columnames.size(); i++)
                              {
                                 final TextView tvc = columnames.get(i);
                                 if (!value_string.get(i).equals("null"))
                                 {

                                    Query5 = Query5 + tvc.getText().toString() + " = ";

                                    if (i == columnames.size() - 1)
                                    {

                                       Query5 = Query5 + "'" + value_string.get(i) + "' ";

                                    }
                                    else
                                    {
                                       Query5 = Query5 + "'" + value_string.get(i) + "' and ";
                                    }

                                 }
                              }
                              Log.d("Delete Query", Query5);

                              AndroidDatabaseManager.this.dbm.getData(Query5);

                              final ArrayList<Cursor> aldc = AndroidDatabaseManager.this.dbm.getData(Query5);
                              final Cursor tempc = aldc.get(1);
                              tempc.moveToLast();
                              Log.d("Update Mesage", tempc.getString(0));

                              if (tempc.getString(0).equalsIgnoreCase("Success"))
                              {
                                 AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#2ecc71"));
                                 AndroidDatabaseManager.this.tvmessage.setText("Row deleted from " + indexInfo.table_name + " table");
                                 refreshTable(0);
                              }
                              else
                              {
                                 AndroidDatabaseManager.this.tvmessage.setBackgroundColor(Color.parseColor("#e74c3c"));
                                 AndroidDatabaseManager.this.tvmessage.setText("Error:" + tempc.getString(0));
                              }
                           }
                        }

                     }).setNegativeButton("close", new DialogInterface.OnClickListener()
                     {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which)
                        {

                        }
                     }).create().show();
               }
            }
         });
   }

   public void refreshactivity()
   {

      finish();
      startActivity(getIntent());
   }

   public void refreshTable(final int d)
   {
      Cursor c3 = null;
      this.tableLayout.removeAllViews();
      if (d == 0)
      {
         final String Query8 = "select * from " + indexInfo.table_name;
         final ArrayList<Cursor> alc3 = this.dbm.getData(Query8);
         c3 = alc3.get(0);
         //saving cursor to the static indexinfo class which can be resued by the other functions
         indexInfo.maincursor = c3;
      }
      if (d == 1)
      {
         c3 = indexInfo.maincursor;
      }
      // if the cursor returened form tha database is not null we display the data in table layout
      if (c3 != null)
      {
         final int counts = c3.getCount();

         Log.d("counts", "" + counts);
         this.tv.setText("" + counts);
         final TableRow tableheader = new TableRow(getApplicationContext());

         tableheader.setBackgroundColor(Color.BLACK);
         tableheader.setPadding(0, 2, 0, 2);
         for (int k = 0; k < c3.getColumnCount(); k++)
         {
            final LinearLayout cell = new LinearLayout(AndroidDatabaseManager.this);
            cell.setBackgroundColor(Color.WHITE);
            cell.setLayoutParams(this.tableRowParams);
            final TextView tableheadercolums = new TextView(getApplicationContext());
            tableheadercolums.setPadding(0, 0, 4, 3);
            tableheadercolums.setText("" + c3.getColumnName(k));
            tableheadercolums.setTextColor(Color.parseColor("#000000"));
            cell.addView(tableheadercolums);
            tableheader.addView(cell);

         }
         this.tableLayout.addView(tableheader);
         c3.moveToFirst();

         //after displaying column names in the first row  we display data in the remaining columns
         //the below paginate table function will display the first 10 tuples of the tables
         //the remaining tuples can be viewed by clicking on the next button
         paginatetable(c3.getCount());
      }
      else
      {

         final TableRow tableheader2 = new TableRow(getApplicationContext());
         tableheader2.setBackgroundColor(Color.BLACK);
         tableheader2.setPadding(0, 2, 0, 2);

         final LinearLayout cell = new LinearLayout(AndroidDatabaseManager.this);
         cell.setBackgroundColor(Color.WHITE);
         cell.setLayoutParams(this.tableRowParams);

         final TextView tableheadercolums = new TextView(getApplicationContext());
         tableheadercolums.setPadding(0, 0, 4, 3);
         tableheadercolums.setText("   Table   Is   Empty   ");
         tableheadercolums.setTextSize(30);
         tableheadercolums.setTextColor(Color.RED);

         cell.addView(tableheadercolums);
         tableheader2.addView(cell);

         this.tableLayout.addView(tableheader2);

         this.tv.setText("" + 0);
      }

   }

   //the function which displays tuples from database in a table layout
   public void paginatetable(final int number)
   {

      final Cursor c3 = indexInfo.maincursor;
      indexInfo.numberofpages = (c3.getCount() / 10) + 1;
      indexInfo.currentpage = 1;
      c3.moveToFirst();
      int currentrow = 0;

      //display the first 10 tuples of the table selected by user
      do
      {

         final TableRow tableRow = new TableRow(getApplicationContext());
         tableRow.setBackgroundColor(Color.BLACK);
         tableRow.setPadding(0, 2, 0, 2);

         for (int j = 0; j < c3.getColumnCount(); j++)
         {
            final LinearLayout cell = new LinearLayout(this);
            cell.setBackgroundColor(Color.WHITE);
            cell.setLayoutParams(this.tableRowParams);
            final TextView columsView = new TextView(getApplicationContext());
            String column_data = "";
            try
            {
               column_data = c3.getString(j);
            }
            catch (final Exception e)
            {
               // Column data is not a string , do not display it	
            }
            columsView.setText(column_data);
            columsView.setTextColor(Color.parseColor("#000000"));
            columsView.setPadding(0, 0, 4, 3);
            cell.addView(columsView);
            tableRow.addView(cell);

         }

         tableRow.setVisibility(View.VISIBLE);
         currentrow = currentrow + 1;
         //we create listener for each table row when clicked a alert dialog will be displayed 
         //from where user can update or delete the row 
         tableRow.setOnClickListener(new OnClickListener()
            {
               @Override
               public void onClick(final View v)
               {

                  final ArrayList<String> value_string = new ArrayList<String>();
                  for (int i = 0; i < c3.getColumnCount(); i++)
                  {
                     final LinearLayout llcolumn = (LinearLayout) tableRow.getChildAt(i);
                     final TextView tc = (TextView) llcolumn.getChildAt(0);

                     final String cv = tc.getText().toString();
                     value_string.add(cv);

                  }
                  indexInfo.value_string = value_string;
                  //the below function will display the alert dialog
                  updateDeletePopup(0);
               }
            });
         this.tableLayout.addView(tableRow);

      }
      while (c3.moveToNext() && currentrow < 10);

      indexInfo.index = currentrow;

      // when user clicks on the previous button update the table with the previous 10 tuples from the database
      this.previous.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(final View v)
            {
               final int tobestartindex = (indexInfo.currentpage - 2) * 10;

               //if the tbale layout has the first 10 tuples then toast that this is the first page
               if (indexInfo.currentpage == 1)
               {
                  Toast.makeText(getApplicationContext(), "This is the first page", Toast.LENGTH_LONG).show();
               }
               else
               {
                  indexInfo.currentpage = indexInfo.currentpage - 1;
                  c3.moveToPosition(tobestartindex);

                  boolean decider = true;
                  for (int i = 1; i < AndroidDatabaseManager.this.tableLayout.getChildCount(); i++)
                  {
                     final TableRow tableRow = (TableRow) AndroidDatabaseManager.this.tableLayout.getChildAt(i);

                     if (decider)
                     {
                        tableRow.setVisibility(View.VISIBLE);
                        for (int j = 0; j < tableRow.getChildCount(); j++)
                        {
                           final LinearLayout llcolumn = (LinearLayout) tableRow.getChildAt(j);
                           final TextView columsView = (TextView) llcolumn.getChildAt(0);

                           columsView.setText("" + c3.getString(j));

                        }
                        decider = !c3.isLast();
                        if (!c3.isLast())
                        {
                           c3.moveToNext();
                        }
                     }
                     else
                     {
                        tableRow.setVisibility(View.GONE);
                     }

                  }

                  indexInfo.index = tobestartindex;

                  Log.d("index =", "" + indexInfo.index);
               }
            }
         });

      // when user clicks on the next button update the table with the next 10 tuples from the database
      this.next.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(final View v)
            {

               //if there are no tuples to be shown toast that this the last page	
               if (indexInfo.currentpage >= indexInfo.numberofpages)
               {
                  Toast.makeText(getApplicationContext(), "This is the last page", Toast.LENGTH_LONG).show();
               }
               else
               {
                  indexInfo.currentpage = indexInfo.currentpage + 1;
                  boolean decider = true;

                  for (int i = 1; i < AndroidDatabaseManager.this.tableLayout.getChildCount(); i++)
                  {
                     final TableRow tableRow = (TableRow) AndroidDatabaseManager.this.tableLayout.getChildAt(i);

                     if (decider)
                     {
                        tableRow.setVisibility(View.VISIBLE);
                        for (int j = 0; j < tableRow.getChildCount(); j++)
                        {
                           final LinearLayout llcolumn = (LinearLayout) tableRow.getChildAt(j);
                           final TextView columsView = (TextView) llcolumn.getChildAt(0);

                           columsView.setText("" + c3.getString(j));

                        }
                        decider = !c3.isLast();
                        if (!c3.isLast())
                        {
                           c3.moveToNext();
                        }
                     }
                     else
                     {
                        tableRow.setVisibility(View.GONE);
                     }
                  }
               }
            }
         });

   }

   @Override
   public void onItemClick(final AdapterView< ? > arg0, final View arg1, final int arg2, final long arg3)
   {
      // TODO Auto-generated method stub

   }

}
