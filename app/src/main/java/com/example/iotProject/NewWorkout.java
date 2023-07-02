package com.example.iotProject;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NewWorkout extends AppCompatActivity {
    private AlertDialog dialog;
    private TrainingPlan tp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_workout_activity);
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(getApplicationContext(), TrainSettings.class);
                startActivity(intent);
                finish();
            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, callback);
        Button goBack = findViewById(R.id.goBackButton);
        goBack.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), TrainSettings.class);
            startActivity(intent);
            finish();
        });
        CardView beginnerCard = findViewById(R.id.begginerWorkoutButton);
        beginnerCard.setOnClickListener(v -> {
            tp = new TrainingPlan("Beginner Workout", 3, 10);
            goInTraining();
        });
        CardView intermediateCard = findViewById(R.id.intermediateWorkoutButton);
        intermediateCard.setOnClickListener(v -> {
            tp = new TrainingPlan("intermediate Workout", 5, 15);
            goInTraining();
        });
        CardView advancedCard = findViewById(R.id.advancedWorkoutButton);
        advancedCard.setOnClickListener(v -> {
            tp = new TrainingPlan("advanced Workout", 5, 15);
            goInTraining();
        });

        CardView customCard = findViewById(R.id.customWorkoutButton);
        customCard.setOnClickListener(v -> buildDialog());
    }
    private void buildDialog() {
        View view = getLayoutInflater().inflate(R.layout.custom_training_dialog, null);
        final EditText setsText = view.findViewById(R.id.setsEdit);
        final EditText repsText = view.findViewById(R.id.repsEdit);
        final EditText name = view.findViewById(R.id.nameEdit);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(NewWorkout.this)
                .setView(view)
                .setMessage("Custom Training")
                .setNegativeButton("OK", (dialogInterface, i) -> {
                    tp = new TrainingPlan(name.getText().toString(),
                            Integer.parseInt(setsText.getText().toString()),
                            Integer.parseInt(repsText.getText().toString()));
                    writeTPToDataBase();
                    goInTraining();
                })
                .setPositiveButton("Cancel", null)
                .create();
        dialog.show();
    }
    private void goInTraining(){
        Intent intent = new Intent(getApplicationContext(), DeviceActivity.class);
        intent.putExtra("trainingPlan", tp);
        intent.putExtra("type", 0);
        startActivity(intent);
        finish();
    }
    private void writeTPToDataBase(){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser!=null){
            String uid = currentUser.getUid();
            DatabaseReference dataBase = FirebaseDatabase.
                    getInstance("https://iot-project-e6e76-default-rtdb.europe-west1.firebasedatabase.app/").
                    getReference("training_plans/"+uid+"/"+tp.getTrainingName());
            dataBase.child("reps").setValue(tp.reps);
            dataBase.child("setsAmount").setValue(tp.setsAmount);
        }
    }
}
