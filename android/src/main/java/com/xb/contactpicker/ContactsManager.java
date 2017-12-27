package com.xb.contactpicker;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.List;

public class ContactsManager extends ReactContextBaseJavaModule implements ActivityEventListener {

  private static final int CONTACT_REQUEST = 1;

  private Callback mCallback;
  private String TAG = ContactsManager.class.getSimpleName();
  private final ContentResolver contentResolver;
  private Activity mCtx;
  private AlertDialog.Builder mBuilder;

  @Override
  public String getName() {
    return "RNContactPicker";
  }

  public ContactsManager(ReactApplicationContext reactContext) {
    super(reactContext);
    this.contentResolver = getReactApplicationContext().getContentResolver();
    reactContext.addActivityEventListener(this);
  }

  @ReactMethod
  public void openContactPicker(Callback callback) {
    mCallback = callback;
    launchPicker();
  }

  @ReactMethod
  public void getAllContact(Callback callback) {
    mCallback = callback;
    AsyncTask.execute(new Runnable() {
      @Override
      public void run() {
        Context context = getReactApplicationContext();
        ContentResolver cr = context.getContentResolver();

        ContactsProvider contactsProvider = new ContactsProvider(cr);
        WritableArray contacts = contactsProvider.getContacts();

        WritableArray result = formatContacts(contacts);
        invokeCallback(createArrayResult(result));
      }
    });
  }

  @ReactMethod
  public void checkContactPermissions(Callback callback) {
    mCallback = callback;
    WritableMap result = Arguments.createMap();
    result.putBoolean("status", isPermissionGranted());
    invokeCallback(result);
  }

  private boolean isPermissionGranted() {
    // return -1 for denied and 1
    int res = getReactApplicationContext().checkCallingOrSelfPermission(
        Manifest.permission.READ_CONTACTS);
    return res == PackageManager.PERMISSION_GRANTED;
  }

  /**
   * Lanch the contact picker, with the specified requestCode for returned data.
   */
  private void launchPicker() {
    //        this.contentResolver.query(Uri.parse("content://com.android.contacts/contacts/lookup/0r3-A7416BA07AEA92F2/3"), null, null, null, null);
    Cursor cursor =
        this.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
    if (cursor != null) {
      Intent intent = new Intent(Intent.ACTION_PICK);
      intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
      mCtx = getCurrentActivity();
      if (intent.resolveActivity(mCtx.getPackageManager()) != null) {
        mCtx.startActivityForResult(intent, CONTACT_REQUEST);
      }
      cursor.close();
    } else {
      invokeCallback(createErr(1, "no permission"));
    }
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    if (requestCode == CONTACT_REQUEST) {
      switch (resultCode) {
        case (Activity.RESULT_OK):
          Uri contactUri = data.getData();
          try {
          /* Retrieve all possible data about contact and return as a JS object */
            //First get ID
            String id = null;
            int idx;
            final WritableMap contactData = Arguments.createMap();
            Cursor cursor = this.contentResolver.query(contactUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
              idx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
              id = cursor.getString(idx);
            } else {
              invokeCallback(createErr(1, "Contact Data Not Found"));
              return;
            }

            // Build the Entity URI.
            Uri.Builder b =
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id).buildUpon();
            b.appendPath(ContactsContract.Contacts.Entity.CONTENT_DIRECTORY);
            contactUri = b.build();

            // Create the projection (SQL fields) and sort order.
            String[] projection = {
                ContactsContract.Contacts.Entity.MIMETYPE, ContactsContract.Contacts.Entity.DATA1
            };
            String sortOrder = ContactsContract.Contacts.Entity.RAW_CONTACT_ID + " ASC";
            cursor = this.contentResolver.query(contactUri, projection, null, null, sortOrder);
            if (cursor == null) return;

            String mime;
            final List<CharSequence> numbers = new ArrayList<>();
            String name = null;

            int dataIdx = cursor.getColumnIndex(ContactsContract.Contacts.Entity.DATA1);
            int mimeIdx = cursor.getColumnIndex(ContactsContract.Contacts.Entity.MIMETYPE);
            if (cursor.moveToFirst()) {
              do {
                mime = cursor.getString(mimeIdx);
                if (name == null &&
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(mime)) {
                  name = cursor.getString(dataIdx);
                }
                if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mime)) {
                  numbers.add(cursor.getString(dataIdx));
                }
              } while (cursor.moveToNext());
            }
            cursor.close();

            contactData.putString("name", name);

            if (numbers.size() == 1) {
              contactData.putString("phone", String.valueOf(numbers.get(0)));
              callResult(true, contactData);
            } else if (numbers.size() > 1) {
              if (mBuilder == null)
                mBuilder = new AlertDialog.Builder(getCurrentActivity());
              mBuilder
                  .setTitle(name)
                  .setItems(numbers.toArray(new CharSequence[numbers.size()]),
                      new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                          contactData.putString("phone", String.valueOf(numbers.get(i)));
                          callResult(true, contactData);
                        }
                      })
                  .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                      invokeCallback(createErr(2, "Cancelled"));
                    }
                  })
                  .create()
                  .show();
            } else {
              callResult(false, null);
            }
            return;
          } catch (Exception e) {
            invokeCallback(createErr(1, e.getMessage()));
            return;
          }
        default:
          invokeCallback(createErr(2, "Cancelled"));
          break;
      }
    }
  }

  @Override
  public void onNewIntent(Intent intent) {

  }

  private void callResult(boolean foundData, WritableMap contactData) {
    if (foundData) {
      invokeCallback(createMapResult(contactData));
      return;
    } else {
      invokeCallback(createErr(1, "No data found for contact"));
      return;
    }
  }

  private void invokeCallback(Object... args) {
    if (mCallback != null) {
      mCallback.invoke(args);
      mCallback = null;
    }
  }

  @NonNull
  private WritableArray formatContacts(WritableArray contacts) {
    WritableArray result = Arguments.createArray();
    for (int i = 0, length = contacts.size(); i < length; i++) {
      ReadableMap temp = contacts.getMap(i);
      WritableMap item = Arguments.createMap();
      item.putString("name", temp.getString("displayName"));
      ReadableArray phoneNumbers = temp.getArray("phoneNumbers");
      WritableArray phoneArray = Arguments.createArray();
      for (int j = 0, l = phoneNumbers.size(); j < l; j++) {
        phoneArray.pushString(phoneNumbers.getMap(j).getString("number"));
      }
      item.putArray("phoneArray", phoneArray);
      result.pushMap(item);
    }
    return result;
  }

  private WritableMap createErr(int code, String msg) {
    WritableMap err = Arguments.createMap();
    err.putInt("code", code);
    err.putString("msg", msg);
    return err;
  }

  private WritableMap createMapResult(WritableMap data) {
    WritableMap result = Arguments.createMap();
    result.putInt("code", 0);
    result.putMap("data", data);
    return result;
  }

  private WritableMap createArrayResult(WritableArray data) {
    WritableMap result = Arguments.createMap();
    result.putInt("code", 0);
    result.putArray("data", data);
    return result;
  }
}
