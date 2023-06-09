package com.example.iotProject;


import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

public class AdvancedStatistics extends AppCompatActivity {

    private int mode = 0;
    private ArrayList<TrainingSession> trainings;
    // TODO todo



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_statistics);
        Button goBackButton = findViewById(R.id.goBackButton);
        goBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), HomeScreen.class);
            startActivity(intent);
            finish();
        });
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(getApplicationContext(), HomeScreen.class);
                startActivity(intent);
                finish();
            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, callback);
//        SwitchCompat switchMode = findViewById(R.id.switchMode);
//        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                mode = 1;
//                switchMode.setText(R.string.push_up_training);
//                // Switch is in the "on" or "checked" position
//                // Perform actions for the "on" state
//            } else {
//                mode = 0;
//                switchMode.setText(R.string.bsu_training);
//                // Switch is in the "off" or "unchecked" position
//                // Perform actions for the "off" state
//            }
//        });
//        switchMode.setChecked(true);
        getTrainings();
    }

    public void setAmountPushUp(){
        LineChart amountLineChart = (LineChart) findViewById(R.id.lineChartAmount);
        ArrayList<Entry> amounts = new ArrayList<>();
        int i = 1;
        for (TrainingSession ts: trainings){
            amounts.add(new Entry(i, ts.totalPushUps));
            i++;
        }
        LineDataSet amountDataSet =  new LineDataSet(amounts, "Number Of Push Ups");
        amountDataSet.setColor(Color.RED);
        amountDataSet.setCircleColor(Color.RED);
        ArrayList<ILineDataSet> amountDataSets = new ArrayList<>();
        amountDataSets.add(amountDataSet);
        LineData amountData = new LineData(amountDataSets);
        amountLineChart.getDescription().setEnabled(false);
        amountLineChart.getLegend().setEnabled(false);
        amountLineChart.setData(amountData);
        amountLineChart.invalidate();
    }
    public void setPowerPushUp(){
        LineChart amountLineChart = (LineChart) findViewById(R.id.lineChartPower);
        ArrayList<Entry> amounts = new ArrayList<>();
        int i = 1;
        for (TrainingSession ts: trainings){
            amounts.add(new Entry((float) i, (float) ts.explosiveness));
            i++;
        }
        LineDataSet amountDataSet =  new LineDataSet(amounts, "Number Of Push Ups");
        amountDataSet.setColor(Color.RED);
        amountDataSet.setCircleColor(Color.RED);
        ArrayList<ILineDataSet> amountDataSets = new ArrayList<>();
        amountDataSets.add(amountDataSet);
        LineData amountData = new LineData(amountDataSets);
        amountLineChart.getDescription().setEnabled(false);
        amountLineChart.getLegend().setEnabled(false);
        amountLineChart.setData(amountData);
        amountLineChart.invalidate();
    }
    public void setPushTime(){
        LineChart amountLineChart = (LineChart) findViewById(R.id.scatterChartPushTime);
        ArrayList<Entry> amounts = new ArrayList<>();
        int i = 1;
        for (TrainingSession ts: trainings){
            amounts.add(new Entry((float) i, (float) ts.avgPushUpTime));
            i++;
        }
        LineDataSet amountDataSet =  new LineDataSet(amounts, "Number Of Push Ups");
        amountDataSet.setColor(Color.RED);
        amountDataSet.setCircleColor(Color.RED);
        ArrayList<ILineDataSet> amountDataSets = new ArrayList<>();
        amountDataSets.add(amountDataSet);
        LineData amountData = new LineData(amountDataSets);
        amountLineChart.getDescription().setEnabled(false);
        amountLineChart.getLegend().setEnabled(false);
        amountLineChart.setData(amountData);
        amountLineChart.invalidate();
    }
    public void setSpeed(){
        LineChart amountLineChart = (LineChart) findViewById(R.id.barChartPushSpeed);
        ArrayList<Entry> amounts = new ArrayList<>();
        int i = 1;
        for (TrainingSession ts: trainings){
            amounts.add(new Entry((float) i, (float) (ts.totalPushUps/ts.avgPushUpTime)));
            i++;
        }
        LineDataSet amountDataSet =  new LineDataSet(amounts, "Number Of Push Ups");
        amountDataSet.setColor(Color.RED);
        amountDataSet.setCircleColor(Color.RED);
        ArrayList<ILineDataSet> amountDataSets = new ArrayList<>();
        amountDataSets.add(amountDataSet);
        LineData amountData = new LineData(amountDataSets);
        amountLineChart.getDescription().setEnabled(false);
        amountLineChart.getLegend().setEnabled(false);
        amountLineChart.setData(amountData);
        amountLineChart.invalidate();
    }

    private void setTrainingsTime(){
        BarChart trainingTimeChart = (BarChart) findViewById(R.id.barChartTrainingsTime);
        ArrayList<BarEntry> hours = new ArrayList<>();
        Date temp;
        for (TrainingSession ts: trainings){
            temp = TrainingSession.reverseHash(ts.returnDate());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(temp);
            hours.add(new BarEntry(calendar.get(Calendar.HOUR_OF_DAY), 1));
        }
        BarDataSet hoursDataSet =  new BarDataSet(hours, "Hours of trainings");
        hoursDataSet.setColor(Color.BLUE);
//        ArrayList<ILineDataSet> hourDataSets = new ArrayList<>();
//        hourDataSets.add(hoursDataSet);
        BarData timeData = new BarData();
        timeData.addDataSet(hoursDataSet);
        trainingTimeChart.getDescription().setEnabled(false);
        trainingTimeChart.getLegend().setEnabled(false);
        trainingTimeChart.setData(timeData);
        trainingTimeChart.invalidate();
    }


    public void getTrainings() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        trainings = new ArrayList<TrainingSession>();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference dataBase = FirebaseDatabase.
                    getInstance("https://iot-project-e6e76-default-rtdb.europe-west1.firebasedatabase.app/").
                    getReference("training_sessions/" + uid);

            dataBase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String date = String.valueOf(childSnapshot.getKey());
                        TrainingSession current = childSnapshot.getValue(TrainingSession.class);
                        Log.d("Firebase", "onDataChange: " + current.avgPushUpTime);
                        current.setDate(date);
                        trainings.add(current);
                    }
                    setAmountPushUp();
                    setTrainingsTime();
                    setSpeed();
                    setPowerPushUp();
                    setPushTime();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.w("Firebase", "loadPost:onCancelled", error.toException());
                }
            });
        }
    }
}
