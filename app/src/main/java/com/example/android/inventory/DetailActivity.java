package com.example.android.inventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.ItemContract.ItemEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DetailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>{

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();
    private static final int EXISTING_ITEM_LOADER = 0;
    private Uri currentItemUri;
    private EditText nameEditText;
    private EditText priceEditText;
    private TextView quantityTextView;
    private EditText emailEditText;
    private ImageView imageView;
    private Button incrementButton;
    private Button decrementButton;
    private Button deleteButton;
    private Button orderButton;

    static final int RESULT_LOAD_IMAGE = 1;

    private int mQuantity = 0;
    private String saveImageText;
    private boolean itemHasChanged = false;

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            itemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        currentItemUri = intent.getData();

        incrementButton = (Button) findViewById(R.id.item_increment_button);
        decrementButton = (Button) findViewById(R.id.item_decrement_button);
        deleteButton = (Button) findViewById(R.id.delate_button);
        orderButton = (Button) findViewById(R.id.order_button);

        if(currentItemUri==null) {
            setTitle("Add Item");
            deleteButton.setVisibility(View.GONE);
            orderButton.setVisibility(View.GONE);
        } else {
            setTitle("Detail information");

            getLoaderManager().initLoader(EXISTING_ITEM_LOADER,null,this);
        }

        nameEditText = (EditText) findViewById(R.id.edit_item_name);
        priceEditText = (EditText) findViewById(R.id.edit_item_price);
        quantityTextView = (TextView) findViewById(R.id.item_quantity);
        emailEditText = (EditText) findViewById(R.id.edit_supplier_email);
        imageView = (ImageView) findViewById(R.id.image_view);

        nameEditText.setOnTouchListener(touchListener);
        priceEditText.setOnTouchListener(touchListener);
        imageView.setOnTouchListener(touchListener);
        emailEditText.setOnTouchListener(touchListener);

        quantityTextView.setText(String.valueOf(mQuantity));
        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQuantity++;
                quantityTextView.setText(String.valueOf(mQuantity));
                if(currentItemUri!=null) itemHasChanged = true;
            }
        });

        decrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQuantity--;
                if(mQuantity<0) mQuantity=0;
                quantityTextView.setText(String.valueOf(mQuantity));
                if(currentItemUri!=null) itemHasChanged = true;
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
              if (Build.VERSION.SDK_INT < 19) {
                  intent = new Intent(Intent.ACTION_GET_CONTENT);
              } else {
                  intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                  intent.addCategory(Intent.CATEGORY_OPENABLE);
              }
              intent.setType("image/*");
                startActivityForResult(intent, RESULT_LOAD_IMAGE);

            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitOrder();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);


        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK
                && data != null) {
            try {


                Uri mUri = data.getData();
                Log.i(LOG_TAG, "Uri: " + mUri.toString());
                int takeFlags = data.getFlags();
                takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    getContentResolver().takePersistableUriPermission(mUri, takeFlags);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                saveImageText = mUri.toString();
                imageView.setImageBitmap(getBitmapFromUri(mUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        Intent intent = super.getSupportParentActivityIntent();
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        return intent;
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        if (uri == null || uri.toString().isEmpty()) return null;

        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        if(targetH==0) targetH=1;
        if(targetW==0) targetW=1;

        InputStream input = null;

        try {
            input = this.getContentResolver().openInputStream(uri);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input,null,bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            int scaleFactor = Math.min(photoW/targetW,photoH/targetH);
            bmOptions.inJustDecodeBounds=false;
            bmOptions.inSampleSize = scaleFactor;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            return bitmap;
        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.",fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image",e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe){

            }
        }
    }

    private void saveItem() {

        String nameString = nameEditText.getText().toString().trim();
        String price = priceEditText.getText().toString().trim();
        String quantity = quantityTextView.getText().toString().trim();
        String supplierEmail = emailEditText.getText().toString().trim();
        String imageText = saveImageText;

        if(currentItemUri == null) {
            if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(supplierEmail)
                    ||   TextUtils.isEmpty(imageText)){
                Toast.makeText(this,"Fill the data",Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);

        int priceInt =0;
        if(!TextUtils.isEmpty(price)) {
            priceInt = Integer.parseInt(price);
        }
        values.put(ItemEntry.COLUMN_ITEM_PRICE, priceInt);

        int quantityInt = 0;
        if (!TextUtils.isEmpty(quantity)) {
            quantityInt = Integer.parseInt(quantity);
        }
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY,quantityInt);
        values.put(ItemEntry.COLUMN_ITEM_SUPPLIER_EMAIL,supplierEmail);
        values.put(ItemEntry.COLUMN_ITEM_IMAGE,imageText);

        if (currentItemUri ==null) {

            Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);
            if(newUri == null) {
                Toast.makeText(this, "Error with saving item",Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(this,"Item saved",Toast.LENGTH_SHORT).show();
            }
        } else {

            int rowsAffected = getContentResolver().update(currentItemUri,values,null,null);
            if (rowsAffected == 0) {
                Toast.makeText(this, "Error with updating item",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Item updated",Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_save:

                saveItem();
                return true;

            case android.R.id.home:

                if (!itemHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (!itemHasChanged) {
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_SUPPLIER_EMAIL,
                ItemEntry.COLUMN_ITEM_IMAGE
        };

        return new CursorLoader(this,
                currentItemUri,
                projection,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if ( cursor.getCount() ==0 ) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int emailColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER_EMAIL);
            int imageColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);

            String name = cursor.getString(nameColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String email = cursor.getString(emailColumnIndex);
            String imageString = cursor.getString(imageColumnIndex);
            mQuantity = quantity;

            if(imageString==null){
                imageView.setImageResource(R.drawable.ic_image);
            } else {
                Uri image = Uri.parse(cursor.getString(imageColumnIndex));
                imageView.setImageBitmap(getBitmapFromUri(image));
            }

            nameEditText.setText(name);
            priceEditText.setText(Integer.toString(price));
            quantityTextView.setText(Integer.toString(quantity));
            emailEditText.setText(email);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        nameEditText.setText("");
        priceEditText.setText("0");
        quantityTextView.setText("0");
        emailEditText.setText("");
        imageView.setImageResource(R.drawable.ic_image);
    }


    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Delete this item?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteItem();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void deleteItem(){
        if (currentItemUri!=null) {
            int rowsDeleted = getContentResolver().delete(currentItemUri,null,null);

            if(rowsDeleted == 0) {
                Toast.makeText(this,"Error with deleting item",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,"Item deleted",Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }

    private void showUnsavedChangesDialog( DialogInterface.OnClickListener discardButtonClickListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Discard your changes and quit editing?");
        builder.setPositiveButton("Discard",discardButtonClickListener);
        builder.setNegativeButton("Keep Editing", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                if (dialog !=null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void submitOrder() {
        String name = nameEditText.getText().toString();
        String[] email = {emailEditText.getText().toString()};
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT,"Order of "+ name);
        emailIntent.putExtra(Intent.EXTRA_EMAIL,email);
        if(emailIntent.resolveActivity(getPackageManager())!=null);{
            startActivity(emailIntent);
        }
    }
}
