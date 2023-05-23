package com.example.tutorial6;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    LineChart mpLineChart;
    LineDataSet lineDataSet1, lineDataSet2, lineDataSet3;
    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    LineData data;
    EditText steps;
    EditText fileName;
    boolean workout = false;
    String time_str = "";
    Spinner mySpinner;
    float firstTime;


    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");

    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

//        sendText = view.findViewById(R.id.send_text);
//        hexWatcher = new TextUtil.HexWatcher(sendText);
//        hexWatcher.enable(hexEnabled);
//        sendText.addTextChangedListener(hexWatcher);
//        sendText.setHint(hexEnabled ? "HEX mode" : "");


//        View sendBtn = view.findViewById(R.id.send_btn);
//        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));

        mpLineChart = (LineChart) view.findViewById(R.id.line_chart);
        lineDataSet1 =  new LineDataSet(emptyDataValues(), "X Acceleration");
        lineDataSet1.setColor(Color.RED);
        lineDataSet1.setCircleColor(Color.RED);
        lineDataSet2 =  new LineDataSet(emptyDataValues(), "Y Acceleration");
        lineDataSet2.setColor(Color.BLUE);
        lineDataSet2.setCircleColor(Color.BLUE);
        lineDataSet3 =  new LineDataSet(emptyDataValues(), "Z Acceleration");
        lineDataSet3.setColor(Color.GREEN);
        lineDataSet3.setCircleColor(Color.GREEN);


        dataSets.add(lineDataSet1);
        dataSets.add(lineDataSet2);
        dataSets.add(lineDataSet3);
        data = new LineData(dataSets);
        mpLineChart.getDescription().setEnabled(false);
        mpLineChart.setData(data);
        mpLineChart.invalidate();

        Button buttonCsvShow = (Button) view.findViewById(R.id.openCsv);
        Button buttonStart = (Button) view.findViewById(R.id.btnStart);
        Button buttonStop = (Button) view.findViewById(R.id.btnStop);
        Button buttonReset = (Button) view.findViewById(R.id.btnReset);
        Button buttonSave = (Button) view.findViewById(R.id.btnSave);

        steps = (EditText) view.findViewById(R.id.editSteps);
        fileName = (EditText) view.findViewById(R.id.editFileName);


        // for some reason the spinner crashes it all
        mySpinner = (Spinner) view.findViewById(R.id.spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity().getApplicationContext(),
                R.array.modes,R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter);

//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss aaa");


//        buttonClear.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                Toast.makeText(getContext(),"Clear",Toast.LENGTH_SHORT).show();
//                LineData data = mpLineChart.getData();
//                ILineDataSet set = data.getDataSetByIndex(0);
//                data.getDataSetByIndex(0);
//                while(set.removeLast()){}
//
//            }
//        });

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (!workout)
                    time_str = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(
                            LocalDateTime.now());
                workout = true;
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workout = false;

            }
        });

        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });

        buttonCsvShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenLoadCSV();

            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private String[] clean_str(String[] stringsArr){
         for (int i = 0; i < stringsArr.length; i++)  {
             stringsArr[i]=stringsArr[i].replaceAll(" ","");
        }


        return stringsArr;
    }
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] message) {
        if(hexEnabled) {
            receiveText.append(TextUtil.toHexString(message) + '\n');
        } else {
            String msg = new String(message);
            if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                String msg_to_save = msg;
                msg_to_save = msg.replace(TextUtil.newline_crlf, TextUtil.emptyString);
                // check message length
                if (msg_to_save.length() > 1) {
                    // split message string by ',' char
                    String[] parts = msg_to_save.split(",");
                    // function to trim blank spaces
                    parts = clean_str(parts);

                    if (workout) {
                        if (lineDataSet1.getValues().size() == 0)
                            firstTime = Float.parseFloat(parts[0]);
                        // add received values to line dataset for plotting the line-chart
                        data.addEntry(new Entry(Float.parseFloat(parts[0]) - firstTime,
                                Float.parseFloat(parts[1])), 0);
                        data.addEntry(new Entry(Float.parseFloat(parts[0]) - firstTime,
                                Float.parseFloat(parts[2])), 1);
                        data.addEntry(new Entry(Float.parseFloat(parts[0]) - firstTime,
                                Float.parseFloat(parts[3])), 2);

                        lineDataSet1.notifyDataSetChanged(); // let the data know a dataSet changed
                        lineDataSet2.notifyDataSetChanged(); // let the data know a dataSet changed
                        lineDataSet3.notifyDataSetChanged(); // let the data know a dataSet changed
                        mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                        mpLineChart.invalidate(); // refresh


                    }

                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                    // send msg to function that saves it to csv
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg.charAt(0) == '\n') {
                        Editable edt = receiveText.getEditableText();
                        if (edt != null && edt.length() > 1)
                            edt.replace(edt.length() - 2, edt.length(), "");
                    }
                    pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                }
                receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
            }
        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)),
                0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        try {
        receive(data);}
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private ArrayList<Entry> emptyDataValues()
    {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        return dataVals;
    }

    private void OpenLoadCSV(){
        Intent intent = new Intent(getContext(),LoadCSV.class);
        startActivity(intent);
    }

    private void reset(){
        workout = false;
        while(lineDataSet1.removeLast()){}
        while(lineDataSet2.removeLast()){}
        while(lineDataSet3.removeLast()){}
        lineDataSet1.notifyDataSetChanged(); // let the data know a dataSet changed
        lineDataSet2.notifyDataSetChanged(); // let the data know a dataSet changed
        lineDataSet3.notifyDataSetChanged(); // let the data know a dataSet changed
        mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
        mpLineChart.invalidate(); // refresh
    }

    private void saveData(){

        // create new csv unless file already exists
        String path = "/sdcard/csv_dir/";
        File folder = new File(path);
        folder.mkdirs();
        File[] listOfFiles = folder.listFiles();
        String currentName = fileName.getText().toString();
        String fileRealName = "";
        for(int i = 0; i < listOfFiles.length; i++){
            fileRealName = listOfFiles[i].getName().substring(
                    0, listOfFiles[i].getName().length() - 4);
            if (currentName.equals(fileRealName)){
                Toast.makeText(getContext(),"This file already exist",Toast.LENGTH_SHORT)
                        .show();
                return;
            }
        }
        if (lineDataSet1.getValues().size() == 0) {
            Toast.makeText(getContext(), "There are no data", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        // saving data to csv
        try {
            String csvName = path + currentName + ".csv";
            CSVWriter csvWriter = new CSVWriter(new FileWriter(csvName, true));
            List<Entry> vals1 = lineDataSet1.getValues();
            List<Entry> vals2 = lineDataSet2.getValues();
            List<Entry> vals3 = lineDataSet3.getValues();

            String[] row = new String[]{"NAME:", currentName + ".csv"};
            csvWriter.writeNext(row);

            row = new String[]{"EXPERIMENT TIME:", time_str};
            csvWriter.writeNext(row);

            row = new String[]{"ACTIVITY TYPE:",mySpinner.getSelectedItem().toString()};
            csvWriter.writeNext(row);

            row = new String[]{"COUNT OF ACTUAL STEPS:",steps.getText().toString()};
            csvWriter.writeNext(row);

            row = new String[]{};
            csvWriter.writeNext(row);

            row = new String[]{"Time [sec]","ACC X","ACC Y","ACC Z"};
            csvWriter.writeNext(row);

            for(int i = 0; i < vals1.size(); i++) {

                // now [0] is t, [1] is x a, [2] is y a, [3] is z
                row = new String[]{vals1.get(i).getX()+"", vals1.get(i).getY()+"",
                        vals2.get(i).getY()+"", vals3.get(i).getY()+""};
                csvWriter.writeNext(row);
            }
            csvWriter.close();
            Toast.makeText(getContext(),"This file saved!",Toast.LENGTH_SHORT)
                    .show();
            reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
