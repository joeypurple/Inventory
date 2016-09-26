package com.example.android.inventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventory.data.InventoryContract;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    //Identifier for the data loader
    private static final int ITEM_LOADER = 0;

    //Adapter for the ListView
    InventoryCursorAdapter mCursorAdapter;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        //Find the ListView which will be populated with the data
        ListView itemListView = (ListView) findViewById(R.id.list);

        //Find and set empty view on the ListView so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        itemListView.setEmptyView(emptyView);

        //Setup an Adapter to create a list item for each row of data in the cursor
        //There is no data yet (until the loader finishes) so pass in null for the cursor
        mCursorAdapter = new InventoryCursorAdapter(this, null, getContentResolver());
        itemListView.setAdapter(mCursorAdapter);



        //Setup the item click listener
        itemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                //Create a new intent to go to {@link EditorActivity}
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);

                /**
                 * Form the content URI that represents the specific item that was clicked on,
                 * by appending the "id" (passed as input to this method) onto the
                 * {@link InventoryEntry#CONTENT_URI}.
                 *
                 * For example, the URI would be "content://com.example.android.inventory/inventory/2"
                 * if the item with ID 2 was clicked on.
                 *
                 */
                Uri currentItemUri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, id);

                //Set the URI on the data field of the intent
                intent.setData(currentItemUri);

                //Launch the {@link EditorActivity} to display the data for the current item
                intent.putExtra("id",id);

                startActivity(intent);
            }
        });

        //Kick off the loader
        getLoaderManager().initLoader(ITEM_LOADER, null, this);

    }





    //Helper Method to delete all items in the database
    private void deleteAllItems() {
        int rowsDeleted = getContentResolver().delete(InventoryContract.InventoryEntry.CONTENT_URI, null, null);
        Log.v("MainActivity", rowsDeleted + " rows deleted from database");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu options from the res/menu/menu_catalog.xml file
        getMenuInflater().inflate((R.menu.menu_main), menu);
        return true;
    }

           @Override
           public boolean onOptionsItemSelected(MenuItem item) {

            //User clicked on a menu option in the app bar overflow menu
            switch (item.getItemId()) {

                //Respond to a click on the Delete All Entries menu option
                case R.id.action_delete_all_entries:
                    deleteAllItems();
                    return true;
            }

    return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        //Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_ITEM_NAME,
                InventoryContract.InventoryEntry.COLUMN_CURRENT_QUANTITY,
                InventoryContract.InventoryEntry.COLUMN_PRICE};

        //This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,
                InventoryContract.InventoryEntry.CONTENT_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }
}