package com.example.groceriesmanager.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.groceriesmanager.Adapters.FoodCategorySpinnerAdapter;
import com.example.groceriesmanager.Models.FoodItem;
import com.example.groceriesmanager.R;
import com.example.groceriesmanager.ReminderBroadcastReceiver;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class EditFoodItemActivity extends AppCompatActivity {
    FoodItem foodItem;
    private EditText etFoodQty;
    private EditText etFoodName;
    private static final String TAG = "EditFoodItemActivity";
    // these date integers are the date that will be opened in the date picker
    int selectedYear;
    int selectedMonth;
    int selectedDayOfMonth;
    Date today = new Date();
    DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_food_item);

        TextView etFoodMeasure = findViewById(R.id.etFoodMeasure);
        etFoodName = findViewById(R.id.etFoodName);
        etFoodQty = findViewById(R.id.etFoodQty);
        Spinner spinnerFoodMeasure = findViewById(R.id.spinnerFoodMeasure);
        ImageButton ibFoodMeasure = findViewById(R.id.ibFoodMeasure);
        Spinner spinnerFoodCategory = findViewById(R.id.spinnerFoodCategory);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);
        TextView tvTitle = findViewById(R.id.tvTitle);
        ImageButton ibDatePicker = findViewById(R.id.ibDatePicker);
        ImageButton ibRemoveDate = findViewById(R.id.ibRemoveDate);
        EditText etExpiryDate = findViewById(R.id.etExpiryDate);
        TextView tvExpiryLabel = findViewById(R.id.tvExpiryLabel);

        selectedYear = today.getYear()+1900;  // the addition is because only three numbers are returned and any 21sy century year starts with 1
        selectedMonth = today.getMonth();
        selectedDayOfMonth = today.getDate();

        String process = getIntent().getStringExtra("process");
        String type = getIntent().getStringExtra("type");

        if (!Objects.equals(type, "pantry")){ // only let user edit expiry date if the item is a pantry item
            ibDatePicker.setVisibility(View.GONE);
            ibRemoveDate.setVisibility(View.GONE);
            etExpiryDate.setVisibility(View.GONE);
            tvExpiryLabel.setVisibility(View.GONE);
        }


        // array adapter for rendering items into the food measure spinner
        ArrayAdapter<CharSequence> foodMeasureAdapter = ArrayAdapter.createFromResource(this, R.array.food_measures, android.R.layout.simple_spinner_item);
        foodMeasureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerFoodMeasure.setAdapter(foodMeasureAdapter);
        // Our custom Adapter class that we created
        FoodCategorySpinnerAdapter adapter = new FoodCategorySpinnerAdapter(getApplicationContext(), Arrays.asList(getResources().getStringArray(R.array.food_categories)));
        adapter.setDropDownViewResource(R.layout.spinner_item_food_category);
        spinnerFoodCategory.setAdapter(adapter);

        ibFoodMeasure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinnerFoodMeasure.setVisibility(View.VISIBLE);
                spinnerFoodMeasure.performClick();
            }
        });

        // if intent process is edit, get the food item passed in and set the values in the edit text, etc
        if (Objects.equals(process, "edit")){
            tvTitle.setText("Edit Food Item"); // change title from "create food item"
            foodItem = getIntent().getParcelableExtra("foodItem");
            etFoodName.setText(foodItem.getName());
            etFoodQty.setText(foodItem.getQuantity());
            etFoodMeasure.setText(foodItem.getMeasure());
            if (Arrays.asList(getResources().getStringArray(R.array.food_measures)).contains(foodItem.getMeasure())){
                spinnerFoodMeasure.setSelection(Arrays.asList(getResources().getStringArray(R.array.food_measures)).indexOf(foodItem.getMeasure()));
            }

            if (Arrays.asList(getResources().getStringArray(R.array.food_categories)).contains(foodItem.getFoodCategory())){
                spinnerFoodCategory.setSelection(Arrays.asList(getResources().getStringArray(R.array.food_categories)).indexOf(foodItem.getFoodCategory()));
            }

            if (foodItem.getExpiryDate()!=null){
                int year = foodItem.getExpiryDate().getYear()+1900; // the addition is because only three numbers are returned and any 21st century year starts with 1
                int month = foodItem.getExpiryDate().getMonth()+1; // add one to fix error in etExpiryDate
                int day = foodItem.getExpiryDate().getDate();
                selectedYear = year;
                selectedMonth = month-1; // subtract one to fix error in date picker
                selectedDayOfMonth = day;
                etExpiryDate.setText(year + "/" + month + "/" + day);
            }
        }

        spinnerFoodMeasure.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position!=0){
                Log.i(TAG, "selected: " + position + spinnerFoodMeasure.getSelectedItem().toString());
                etFoodMeasure.setText(spinnerFoodMeasure.getItemAtPosition(position).toString());
                spinnerFoodMeasure.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerFoodMeasure.setVisibility(View.GONE);
            }
        });

        ibDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // create Date Select Listener
                DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {

                        // format date to yyyy/mm/dd format
                        etExpiryDate.setText(year + "/" + (monthOfYear + 1) + "/" + dayOfMonth);
                        selectedYear = year;
                        selectedMonth = monthOfYear;
                        selectedDayOfMonth = dayOfMonth;
                    }
                };

                // Create DatePickerDialog (Spinner Mode):
                DatePickerDialog datePickerDialog = new DatePickerDialog(EditFoodItemActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        dateSetListener, selectedYear, selectedMonth, selectedDayOfMonth);

                // Show
                datePickerDialog.show();
            }
        });

        ibRemoveDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etExpiryDate.setText(null);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date expiryDate = null;

                String foodName = etFoodName.getText().toString();
                String foodQty = etFoodQty.getText().toString();
                String expiryDateStr = etExpiryDate.getText().toString();
                if (expiryDateStr!=null && !Objects.equals(expiryDateStr, "")){
                    // todo: check if date format is valid i.e. must be yyyy/dd/mm or yyyy/d/m if the date or month is one digit
                    try {
                        expiryDate = formatter.parse(expiryDateStr);

//                        Log.i(TAG, "today: " + today);
//                        Log.i(TAG, "expiry date: " + expiryDate);
                        if(expiryDate.compareTo(today)<0&&Objects.equals(type, "pantry")){ // if expiry date is set in future
                            Toast.makeText(EditFoodItemActivity.this, "expiry date must be in the future!", Toast.LENGTH_LONG).show();
                            return;
                        }
                    } catch (java.text.ParseException e) {
                        Log.e(TAG, "error formatting date: " + e.toString());
                        e.printStackTrace();
                    }
                }
                String foodMeasure = etFoodMeasure.getText().toString();
                String foodCategory = Arrays.asList(getResources().getStringArray(R.array.food_categories)).get(spinnerFoodCategory.getSelectedItemPosition());



                if (foodName.replaceAll("\\s+", "").length()==0){ // if the user did not type in a food name or types only spaces
                    Toast.makeText(EditFoodItemActivity.this, "type in the food name", Toast.LENGTH_LONG).show();
                }
                else{
                    // using FoodStruct so we do not need to keep altering the function signature of add/editFoodItem
                    FoodStruct foodStruct = new FoodStruct();
                    foodStruct.foodCategory = foodCategory;
                    foodStruct.foodName = foodName;
                    foodStruct.foodQty = foodQty;
                    foodStruct.foodMeasure = foodMeasure;
                    foodStruct.type = type;
                    foodStruct.expiryDate = expiryDate;
                    if (Objects.equals(process, "new")){ // is user is creating new food item
                        addFoodItem(foodStruct);
                    }
                    else{ // process is edit
                        editFoodItem(foodStruct);
                    }
                }
        }
        });
    }

    class FoodStruct{
        public String foodName;
        public String foodQty;
        public String foodMeasure;
        public String type;
        public String foodCategory;
        public Date expiryDate;
    };

    private void editFoodItem(FoodStruct foodStruct) {
        foodItem.setName(foodStruct.foodName.replaceAll("\n", ""));

        if (!Objects.equals(foodStruct.foodQty, "")){
            foodItem.setQuantity(foodStruct.foodQty);
            foodItem.setMeasure(foodStruct.foodMeasure);
        }
        else{
            foodItem.remove(FoodItem.KEY_MEASURE);
            foodItem.remove(FoodItem.KEY_QUANTITY);
        }

        if (!Objects.equals(foodStruct.foodCategory, "--no selection--")){
            foodItem.setCategory(foodStruct.foodCategory);
        }
        else {
            // if set to no selection, remove food category
            foodItem.remove(FoodItem.KEY_CATEGORY);
        }

        if (foodStruct.expiryDate!=null){
            if (!Objects.equals(foodStruct.type, "pantry")){ // only pantry items should have expiry dates
                foodItem.remove(FoodItem.KEY_EXPIRY_DATE);
                // todo (optional): check if notification scheduled and remove it
            }
            else {
                foodItem.setExpiryDate(foodStruct.expiryDate);
                setNotification(foodStruct.expiryDate, foodStruct.foodName);
            }
        }
        else {
            foodItem.remove(FoodItem.KEY_EXPIRY_DATE);
            /* todo: check if notification scheduled and remove it
            * (stretch) only include notification if it is a vegetable, for five days after creation date
             */
        }

        foodItem.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e!=null){
                    Log.e(TAG, "error saving edited food item: " + e.toString());
                    Toast.makeText(EditFoodItemActivity.this, "Could not edit food item", Toast.LENGTH_SHORT).show();
                }
                else {
                    // Prepare data intent to be sent back to grocery/pantry list
                    Intent data = new Intent();
                    // Pass relevant data back as a result
                    data.putExtra("process", "edit");
                    data.putExtra("foodItem", foodItem);
                    // Activity finished ok, return the data
                    setResult(RESULT_OK, data); // set result code and bundle data for response
                    finish();
                }
            }
        });
    }

    private void addFoodItem(FoodStruct foodStruct){
        FoodItem newFoodItem = new FoodItem();
        newFoodItem.setName(foodStruct.foodName.replaceAll("\n", ""));
        newFoodItem.setType(foodStruct.type);
        newFoodItem.setUser(ParseUser.getCurrentUser());

        if (!Objects.equals(foodStruct.foodQty, "")){
            newFoodItem.setQuantity(foodStruct.foodQty);
            newFoodItem.setMeasure(foodStruct.foodMeasure);
        }
        else {
            newFoodItem.remove(FoodItem.KEY_QUANTITY);
            newFoodItem.remove(FoodItem.KEY_MEASURE);
        }

        if (!Objects.equals(foodStruct.foodCategory, "--no selection--")){
            newFoodItem.setCategory(foodStruct.foodCategory);
        }
        else {
            newFoodItem.remove(FoodItem.KEY_CATEGORY);
        }

        if (foodStruct.expiryDate!=null){
            newFoodItem.setExpiryDate(foodStruct.expiryDate);
            Log.i(TAG, "expiry date: " + newFoodItem.getExpiryDate().toString());
            setNotification(foodStruct.expiryDate, foodStruct.foodName);
        }
        else {
            newFoodItem.remove(FoodItem.KEY_EXPIRY_DATE);
        }

        // update info in parse server
        newFoodItem.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e!=null){
                    Log.e(TAG, "error saving food item to server");
                }
                else{
                    Log.i(TAG, "food item saved successfully");
                    etFoodName.setText("");
                    etFoodQty.setText("");

                    // Prepare data intent to be sent back to grocery/pantry list
                    Intent data = new Intent();
                    // Pass relevant data back as a result
                    data.putExtra("process", "new");
                    data.putExtra("foodItem", newFoodItem);
                    // Activity finished ok, return the data
                    setResult(RESULT_OK, data); // set result code and bundle data for response
                    finish();
                }
            }
        });
        }

    private void setNotification(Date expiryDate, String name){
        // todo: schedule notification 1-5 days before expiry. check if up to three - five days already.
        Intent intent = new Intent(EditFoodItemActivity.this, ReminderBroadcastReceiver.class);
        intent.putExtra("name", name);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(EditFoodItemActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        // using alarm service for receiving intents at time of choosing for notifications i.e. at time before expiry date
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

         /* set notification based on expiry date
         * 1. get epiry date in milliseconds
         * 2. subtract a day from it. if it is earlier than today, recursively subtract less days until it isn't.
         * 3. program the notification for then.
         * */
        long expiryTimeInMilliseconds = expiryDate.getTime();
        Log.i(TAG, "expiryTimeInMilliseconds: " + expiryTimeInMilliseconds);

        long currentTimeInMillis = System.currentTimeMillis();
        Log.i(TAG, "currentTimeInMillis: " + currentTimeInMillis);

        long twelveHoursInMillis = 12 * 3600000;
        long fiveDaysBefore = twelveHoursInMillis * 7;
        long threeDaysBefore = twelveHoursInMillis * 5;

        long triggerAtMillis = expiryTimeInMilliseconds - fiveDaysBefore;
        if (triggerAtMillis > currentTimeInMillis){
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
        else {
            triggerAtMillis = expiryTimeInMilliseconds - threeDaysBefore;
        }

        if (triggerAtMillis > currentTimeInMillis){
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
        else {
            triggerAtMillis = expiryTimeInMilliseconds - twelveHoursInMillis;
        }

        if (triggerAtMillis > currentTimeInMillis){
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }

//        // notifications for 10 seconds after (for testing only)
//        Intent intent = new Intent(EditFoodItemActivity.this, ReminderBroadcastReceiver.class);
//        intent.putExtra("name", name);
//        intent.putExtra("fragment", "pantry");
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(EditFoodItemActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
//
//        // using alarm service for receiving intents at time of choosing for notifications i.e. at time before expiry date
//        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//
//        long timeAtButtonClick = System.currentTimeMillis();
//        long timeDelayForNotificationInMillis = 1000 * 10;
//        // the arguments for set are the type of alarm, the time it goes off and the action to take when it goes off
//        alarmManager.set(AlarmManager.RTC_WAKEUP, timeDelayForNotificationInMillis + timeAtButtonClick, pendingIntent);
    }


}