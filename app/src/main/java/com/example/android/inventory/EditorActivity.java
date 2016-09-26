package com.example.android.inventory;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.ContextWrapper;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by joe on 16/09/2016.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PERMISSIONS_PICK_IMAGE_REQUEST_CODE = 1;
    private static final int EXISTING_ITEM_LOADER = 0;
    //Logging identifier for class
    private final String LOG_TAG = EditorActivity.class.getSimpleName();
    public String image;
    public String price;
    public String quantity;
    public String name;
    private Button mImagePathButton;
    private ImageView mImageEdit;
    private Uri mCurrentItemUri;
    private EditText mNameEditText;
    private EditText mQuantityEditText;
    private EditText mPriceEditText;
    private boolean mItemHasChanged = false;


    private Uri selectedImageUri;


    /**
     * On TouchListener that listens for any user touches on a View, implying that they are
     * modifying the view, adn we change the mItemHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);


        //Find all relevant views needed to read user input
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mImagePathButton = (Button) findViewById(R.id.add_photo_button);
        mImageEdit = (ImageView) findViewById(R.id.edit_image);


        // Examine the intent that was used to launch this activity in order to figure out if
        // we're creating a new item or editing an existing one
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();
        Long id = intent.getLongExtra("id", 0);


        //if the intent DOES NOT contain an item content URI, then we are creating a new item
        if (mCurrentItemUri == null) {
            //This is a new itme, so change the app bar to say "Add an Item"
            setTitle(getString(R.string.editor_activity_title_new_item));

            //Invalidate the options menu so the "Delete" menu option can be hidden.
            invalidateOptionsMenu();
        } else {
            //Otheriwse, this is an existing item, so change the app bar to say "Edit Item"
            setTitle(getString(R.string.editor_activity_title_edit_item));

            //Initialize a loader to read the  data from the database and display
            // the current values in the editor
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
            ContextWrapper cw = new ContextWrapper(this);
            File dir = cw.getFilesDir();

            // Load product image
            String imageLocationDir = dir.toString();



            String imagePath = imageLocationDir + "/" + id;
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            mImageEdit.setImageBitmap(bitmap);
        }


        //Setup OnTouchListeners on all the input fields so we can determine if the user has
        // touched or modified them. This will let us know if there are unsaved changees
        // or not if the user tried to leave the editor without saving
        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mImagePathButton.setOnTouchListener(mTouchListener);
    }


    // Get user input from editor and save item into database

    public void onClickSubmit() {

        name = mNameEditText.getText().toString();
        price = mPriceEditText.getText().toString();
        quantity = mQuantityEditText.getText().toString();
        // image = mImageEdit.getDrawable().toString();


        if (name.length() == 0) {
            Toast.makeText(getApplicationContext(), "Name can\'t be empty", Toast.LENGTH_LONG).show();
            mNameEditText.setError("Name can\'t be empty");
            return;
        } else if (quantity.length() == 0) {
            Toast.makeText(getApplicationContext(), "Quantity Needed", Toast.LENGTH_LONG).show();
            mQuantityEditText.setError("Invalid Input");
            return;
        } else if (price.length() == 0) {
            Toast.makeText(getApplicationContext(), "Price Needed", Toast.LENGTH_LONG).show();
            mPriceEditText.setError("Invalid Price");
            return;
        } else if (mImagePathButton.getText().toString().length() == 0) {
            Toast.makeText(getApplicationContext(), "Upload an image", Toast.LENGTH_LONG).show();
            return;

        } else {
            saveItem();
            mImagePathButton.getText().toString();
            finish();
        }
    }


    private void saveItem() {
        //Read from input fields, use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String imgString = mImagePathButton.getText().toString();

        String id = "";


        //Check if this is supposed to be a new item and check if all fields are blank
        if (mCurrentItemUri == null &&
                TextUtils.isEmpty(nameString) &&
                TextUtils.isEmpty(quantityString) &&
                TextUtils.isEmpty(imgString) &&
                TextUtils.isEmpty(priceString)) {
            return;
        }

        //Create a ContenteValues object where column names are the keys and
        // attributes from the editor are the values
        ContentValues values = new ContentValues();
        values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME, nameString);
        values.put(InventoryContract.InventoryEntry.COLUMN_CURRENT_QUANTITY, quantityString);
        values.put(InventoryContract.InventoryEntry.COLUMN_PRICE, priceString);
        values.put(InventoryContract.InventoryEntry.COLUMN_IMAGE, imgString);


        //Determine if this is a new or existing item by checking if mCurrentItemUri is null or not
        if (mCurrentItemUri == null) {
            //This is a NEW item, so insert a new item into the provider, returning the
            //content URI for the new item
            Uri newUri = getContentResolver().insert(InventoryContract.InventoryEntry.CONTENT_URI, values);

            //Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                Toast.makeText(this, getString(R.string.editor_insert_item_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_item_successful), Toast.LENGTH_SHORT).show();
                id = InventoryContract.InventoryEntry.getIdFromUri(newUri);
            }
        } else {
            //Otherwise, this is an EXISTING item, update the item with content URI:
            // mCurrentItemUri and pass in the new ContentValues/ Pass in null for the selection
            // and selection args because mCurrentItemUri will already identify the correct
            // row in the database that we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

            //Show a toast message depending on whether or not the update was successful
            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_update_item_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_item_successful), Toast.LENGTH_SHORT).show();
                id = InventoryContract.InventoryEntry.getIdFromUri(mCurrentItemUri);
            }


        }

        if (!id.equals("")) {
            ContextWrapper cw = new ContextWrapper(this);
            File dir = cw.getFilesDir();

            //Load product image
            String imageLocationDir = dir.toString();
            String imagePath = id;
            Bitmap bitmap = ((BitmapDrawable) mImageEdit.getDrawable()).getBitmap();
            saveToInternalStorage(bitmap, imagePath);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        //If this is a new item, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            //respond to a click on the "save: menu option
            case R.id.action_save:
                onClickSubmit();
                return true;
            //Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            //Respond to a click on the "Sell" menu option
            case R.id.action_sell:
                sellItem();
                return true;
            //Respond to a click on the "Received" menu option
            case R.id.action_receive:
                receiveItem();
                return true;
            case R.id.action_order:
                orderItem();
                return true;
            //Respind to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                //If the item hasn't changed, continue with navigating up to the parent activity,
                // which is the {@link MainActivity}
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                //Otherwise, if there are unsaved changes, setup a dialog to warn the user.
                //Create a click listener to handle the user confirming that
                // changes should be discarded
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                //Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //If the item hasn't changed, continue with handling the back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        //Otherwise, if there are unsaved changed, setup a dialog to warn the user
        //Create a click listener to handle the user confirming that changes should be discareded.
        DialogInterface.OnClickListener discaredButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        };

        //Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discaredButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        //Since the editor shows all the attributes, define a projection that contains all columns
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_ITEM_NAME,
                InventoryContract.InventoryEntry.COLUMN_CURRENT_QUANTITY,
                InventoryContract.InventoryEntry.COLUMN_PRICE,
                InventoryContract.InventoryEntry.COLUMN_IMAGE};

        //this loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,
                mCurrentItemUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        //Bail early if the cursor is null or there is less than one row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        //Proceed with moving to the first row of the cursor and reading data from it
        //This should be the only row in the cursor
        if (cursor.moveToFirst()) {
            //Find the columns of item attributes that we're intereseted in
            int nameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_CURRENT_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_PRICE);
            int imageColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_IMAGE);

            //Extract out the value from the cursor for the given index
            String name = cursor.getString(nameColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            Double price = cursor.getDouble(priceColumnIndex);
            String imageUri = cursor.getString(imageColumnIndex);

            //Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(Double.toString(price));
            // mImageEdit.setImageDrawable(null);

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //If the loader is invalidated, clear out the data from the input fields
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
        mImageEdit.setImageDrawable(null);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        //Create an AlertDialog.Builder and set the message, and click listeners
        //for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                //User clicked the "keep editing" button, so dismiss the dialog and
                // continue editing the item
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        //Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //Prompt the user to confirm they want to tdelete this item
    private void showDeleteConfirmationDialog() {
        //Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                //User clicked the "Delete" Button, so delete the item
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                //User clicked the "Cancel" button, so dismiss the dialog and continue editing
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        //Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    //Perform the deletion of the item in the database
    private void deleteItem() {
        //Only perform the delete if the item is existing
        if (mCurrentItemUri != null) {
            //Call the Content Resolver to delete the pet at the fiven content URIf.
            //Pass in null for the selection and selection args because the mCurrentItemUri
            //content Uri already identifies the item that we want
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

            //Show a toast message depending on whether o rnot the delete was successful
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_item_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_item_successful), Toast.LENGTH_SHORT).show();
            }
        }

        //Close the activity
        finish();
    }

    private void sellItem() {

        ContentValues values = new ContentValues();

        int sell = Integer.valueOf(mQuantityEditText.getText().toString());


        if (sell == 0) {
            return;
        } else {
            sell = sell - 1;
        }

        values.put(InventoryContract.InventoryEntry.COLUMN_CURRENT_QUANTITY, sell);

        int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

        Toast.makeText(this, getString(R.string.editor_update_item_successful), Toast.LENGTH_SHORT).show();
    }


    private void receiveItem() {

        ContentValues values = new ContentValues();

        int receive = Integer.valueOf(mQuantityEditText.getText().toString());

        receive = receive + 1;

        values.put(InventoryContract.InventoryEntry.COLUMN_CURRENT_QUANTITY, receive);

        int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

        Toast.makeText(this, getString(R.string.editor_update_item_successful), Toast.LENGTH_SHORT).show();


    }

    public void orderItem() {
        String subject = "Reorder ";
        String message = "Product Name: " + mNameEditText.getText() +
                "\nProduct Price: " + mPriceEditText.getText() +
                "\nQuantity To be ordered: " + mQuantityEditText.getText();
        String[] emails = {"vendor@gmail.com"};
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, emails);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void addPhoto(View v) {
        Intent intent = new Intent();
        // Accept only images
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 &&
                resultCode == RESULT_OK && null != data) {
            Toast.makeText(this, "Uploading...", Toast.LENGTH_LONG).show();
            Uri selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ImageView imageView = (ImageView) findViewById(R.id.edit_image);
                imageView.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void saveToInternalStorage(Bitmap bmp, String filename) {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File appDirectory = contextWrapper.getFilesDir();

        File currentPath = new File(appDirectory, filename);

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(currentPath);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkWriteToExternalPerms() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Explain to the user why we need to read the sd card
                }
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_PICK_IMAGE_REQUEST_CODE);
            } else {
            }
        } else {
        }
    }

}
