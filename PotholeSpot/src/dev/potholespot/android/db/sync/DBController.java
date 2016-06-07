package dev.potholespot.android.db.sync;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBController extends SQLiteOpenHelper
{

   public DBController(final Context applicationcontext)
   {
      super(applicationcontext, "user.db", null, 1);
   }

   //Creates Table
   @Override
   public void onCreate(final SQLiteDatabase database)
   {
      String query;
      query = "CREATE TABLE users ( userId INTEGER, userName TEXT)";
      database.execSQL(query);
   }

   @Override
   public void onUpgrade(final SQLiteDatabase database, final int version_old, final int current_version)
   {
      String query;
      query = "DROP TABLE IF EXISTS users";
      database.execSQL(query);
      onCreate(database);
   }

   /**
    * Inserts User into SQLite DB
    * 
    * @param queryValues
    */
   public void insertUser(final HashMap<String, String> queryValues)
   {
      final SQLiteDatabase database = getWritableDatabase();
      final ContentValues values = new ContentValues();
      values.put("userId", queryValues.get("userId"));
      values.put("userName", queryValues.get("userName"));
      database.insert("users", null, values);
      database.close();
   }

   /**
    * Get list of Users from SQLite DB as Array List
    * 
    * @return
    */
   public ArrayList<HashMap<String, String>> getAllUsers()
   {
      ArrayList<HashMap<String, String>> usersList;
      usersList = new ArrayList<HashMap<String, String>>();
      final String selectQuery = "SELECT  * FROM users";
      final SQLiteDatabase database = getWritableDatabase();
      final Cursor cursor = database.rawQuery(selectQuery, null);
      if (cursor.moveToFirst())
      {
         do
         {
            final HashMap<String, String> map = new HashMap<String, String>();
            map.put("userId", cursor.getString(0));
            map.put("userName", cursor.getString(1));
            usersList.add(map);
         }
         while (cursor.moveToNext());
      }
      database.close();
      return usersList;
   }

}
