package com.example.android.inventory;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventory.data.InventoryContract;

import static com.example.android.inventory.R.id.price;
import static com.example.android.inventory.R.id.quantity;

/**
 * Created by joe on 18/09/2016.
 */
public class InventoryCursorAdapter extends CursorAdapter {
    ContentResolver mContentResolver;

    public InventoryCursorAdapter(Context context, Cursor c, ContentResolver contentResolver) {
        super(context, c, 0);
        mContentResolver = contentResolver;
    }

    /**
     * Makes a new blank list item view. No data is set or bound to the view yet
     *
     * @param context app context
     * @param cursor  The Cursor from which to get the data. The cursor is already moved
     *                to the correct position.
     * @param parent  The parent to which the new view is attached
     * @return the newly created list item view
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
//Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the inventory data in the current row pointed to by the cursor
     * to the given list item layout. For example, the name for the current item can be set on the
     * name TextView in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        //Find individual views that we want to modify in the list item layout
        final TextView nameTextView = (TextView) view.findViewById(R.id.name);
        final TextView quantityTextView = (TextView) view.findViewById(quantity);
        TextView priceTextView = (TextView) view.findViewById(price);


        //Find the columns of item attributes that we are interested in
        int idColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_CURRENT_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_PRICE);



        // Update the TextViews with the attributes for the current item
        int rowId = cursor.getInt(idColumnIndex);
        String itemName = cursor.getString(nameColumnIndex);
        final int itemQuantity = cursor.getInt(quantityColumnIndex);
        double itemPrice = cursor.getDouble(priceColumnIndex);




        //Update the TextViews with the attributes for the current item
        nameTextView.setText(itemName);
        nameTextView.setTag(rowId);
        quantityTextView.setText(Integer.toString(itemQuantity));
        priceTextView.setText("$" + Double.toString(itemPrice));




        Button makeSale = (Button) view.findViewById(R.id.list_item_sell_button);
        makeSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ContentValues values = new ContentValues();

                int sell = Integer.valueOf(quantityTextView.getText().toString());


                if (sell == 0) {

                    return;
                } else {
                    sell = sell - 1;


                }

                values.put(InventoryContract.InventoryEntry.COLUMN_CURRENT_QUANTITY, sell);

                int rowid = (Integer) nameTextView.getTag();


                Uri currentItemUri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, rowid);

                int rowsAffected = mContentResolver.update(currentItemUri, values, null, null);


            }

        });

    }

}