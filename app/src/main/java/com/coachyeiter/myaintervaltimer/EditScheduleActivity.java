package com.coachyeiter.myaintervaltimer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class EditScheduleActivity extends AppCompatActivity {

    private Schedule schedule;
    private ListView lvSchedule;

    private ScheduleAdapter adpSchedule;
    private MenuItem mnuIncludePeriodZero;

    private boolean[] paAnnounceTimes;
    private boolean includePeriodZero;
    //stores whether pa announcements should be made at certain times
    //if an index has a value of true that means an announcement should be made
    //at time index * 30 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_schedule);

        schedule = new Schedule();

        //load in paAnnounceTimes from preferesnces
        paAnnounceTimes = new boolean[61];
        for (int i = 0; i < paAnnounceTimes.length; i++){
            boolean prefValue = getPreferences(Context.MODE_PRIVATE).getBoolean(String.valueOf(i), false);
            paAnnounceTimes[i]=prefValue;
        }

        setUpPeriodWidgets();
        setUpListView();
        updateTitleBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_schedule, menu);

        mnuIncludePeriodZero = menu.findItem(R.id.mnu_item_include_period_zero);
        updateIncludePeriodZeroItem();
        return true;
    }


    private void updateTitleBar(){
        getSupportActionBar().setTitle("MYIntervalTimer     " + schedule.getFormattedTime());
    }

    private void updateIncludePeriodZeroItem(){
        if (includePeriodZero){
            mnuIncludePeriodZero.setTitle("Don't Include Period Zero");
        }
        else{
            mnuIncludePeriodZero.setTitle("Include Period Zero");
        }
        if (adpSchedule != null){
            adpSchedule.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.mnu_item_include_period_zero:
                includePeriodZero = !includePeriodZero;
                updateIncludePeriodZeroItem();
                return true;
            case R.id.mnu_item_announcements:
                paAnnouncementTimesDlg();
                return true;
            case R.id.mnu_item_load:
                loadDlg();
                return true;
            case R.id.mnu_item_save:
                saveDlg();
                return true;
            case R.id.mnu_item_delete:
                deleteDlg();
                return true;
            case R.id.mnu_item_start_timer:
                if (schedule.getPeriods().size() > 0) {
                    Intent startTimerIntent = new Intent(this, TimerActivity.class);
                    startTimerIntent.putExtra("schedule", schedule);
                    startTimerIntent.putExtra("paAnnouncements", paAnnounceTimes);
                    startTimerIntent.putExtra("includePeriodZero", includePeriodZero);
                    startActivity(startTimerIntent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private void loadSchedule(String filename){
        FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
            fis = openFileInput(filename);
            in = new ObjectInputStream(fis);
            schedule = (Schedule) in.readObject();

            setUpListView();
            updateTitleBar();

            in.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Couldn't load Schedule", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Couldn't load Schedule", Toast.LENGTH_SHORT).show();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Couldn't load Schedule", Toast.LENGTH_SHORT).show();
        }
        finally {
            if (in != null) try { in.close();} catch (IOException e) {e.printStackTrace();}
            if (fis != null) try {fis.close();} catch (IOException e) {e.printStackTrace();}
        }
    }

    private void saveSchedule(String filename){
        if (filename == null || filename.length() == 0){
            Toast.makeText(this, "Couldn't Save Schedule", Toast.LENGTH_SHORT).show();
            return;
        }
        filename += ".itsched";
        FileOutputStream fos = null;
        ObjectOutputStream out = null;

        try {
            fos = openFileOutput(filename, Context.MODE_PRIVATE);
            out = new ObjectOutputStream(fos);
            out.writeObject(schedule);

            out.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Couldn't Save Schedule", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Couldn't Save Schedule", Toast.LENGTH_SHORT).show();
        }
        finally {
            if (out != null) try { out.close();} catch (IOException e) {e.printStackTrace();}
            if (fos != null) try {fos.close();} catch (IOException e) {e.printStackTrace();}
        }
    }

    private void deleteSchedules(ArrayList<String> filesToDelete){
        for (String filename: filesToDelete){
            File file = new File(getFilesDir().getAbsolutePath(), filename);
            file.delete();
        }

    }

    private void saveDlg(){
        //save dialog initialization
        LayoutInflater inflater = getLayoutInflater();
        View saveLayout = inflater.inflate(R.layout.dialog_save_schedule, null);
        final EditText etSaveScheduleName = saveLayout.findViewById(R.id.et_save_schedule_name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save");
        builder.setView(saveLayout);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String scheduleName = etSaveScheduleName.getText().toString();
                saveSchedule(scheduleName);
                etSaveScheduleName.getText().clear();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void loadDlg(){
        //use dialog to get filename
        LayoutInflater inflater = getLayoutInflater();
        View loadLayout = inflater.inflate(R.layout.dialog_load_schedule, null);
        final ListView lvLoadScheduleNames = loadLayout.findViewById(R.id.lv_filenames);
        final ArrayList<String> scheduleNames = new ArrayList<String>();
        final StringBuilder loadFileNameBuilder = new StringBuilder();

        //find all the files in directory that match .itsched
        File scheduleDir = getFilesDir();
        File[] fileNames = scheduleDir.listFiles();
        for (int i = 0; i < fileNames.length; i++){
            if (FilenameUtils.isExtension(fileNames[i].getName(), "itsched")) {
                scheduleNames.add(fileNames[i].getName());
            }
        }
        Collections.sort(scheduleNames, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Load");
        builder.setView(loadLayout);
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dlgLoadDialog = builder.create();
        
        lvLoadScheduleNames.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, scheduleNames));
        lvLoadScheduleNames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                loadFileNameBuilder.append(scheduleNames.get(i));
                loadSchedule(loadFileNameBuilder.toString());
                dlgLoadDialog.dismiss();
            }
        });
        dlgLoadDialog.show();
    }

    private void deleteDlg(){
        //use dialog to get file names
        ArrayList<String> scheduleNames = new ArrayList<String>();
        File scheduleDir = getFilesDir();
        File[] fileNames = scheduleDir.listFiles();
        for (int i = 0; i < fileNames.length; i++){
            if (FilenameUtils.isExtension(fileNames[i].getName(), "itsched")) {
                scheduleNames.add(fileNames[i].getName());
            }
        }
        Collections.sort(scheduleNames, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        final String[] scheduleNamesAsArray = scheduleNames.toArray(new String[scheduleNames.size()]);
        boolean[] pickedNames = new boolean[scheduleNamesAsArray.length];
        final ArrayList<String> schedulesToDelete = new ArrayList<String>();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select schedules to Delete");
        builder.setPositiveButton("OK", null);
        builder.setMultiChoiceItems(scheduleNamesAsArray, pickedNames, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                if (b){
                    schedulesToDelete.add(scheduleNamesAsArray[i]);
                }
                else {
                    schedulesToDelete.remove(scheduleNamesAsArray[i]);
                }
            }
        });
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteSchedules(schedulesToDelete);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();

    }

    private void paAnnouncementTimesDlg(){
        String[] paAnnounceTimesStrings = getResources().getStringArray(R.array.pa_announcement_values);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select when to make Announcements");
        builder.setPositiveButton("OK", null);
        builder.setMultiChoiceItems(paAnnounceTimesStrings, paAnnounceTimes, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                getPreferences(Context.MODE_PRIVATE).edit().putBoolean(String.valueOf(i), b).commit();
            }
        });
        builder.create().show();
    }

    private void setUpListView() {
        lvSchedule = findViewById(R.id.lv_schedule);
        adpSchedule = new ScheduleAdapter(this, R.layout.row_schedule_listview, schedule.getPeriods());
        lvSchedule.setAdapter(adpSchedule);
    }

    private void setUpPeriodWidgets(){

        ArrayAdapter<CharSequence> adpPeriodLength = ArrayAdapter.createFromResource(this, R.array.period_length_values, android.R.layout.simple_spinner_item);
        adpPeriodLength.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spnPeriodLength1 = findViewById(R.id.spn_period_length_1);
        spnPeriodLength1.setAdapter(adpPeriodLength);
        Spinner spnPeriodLength2 = findViewById(R.id.spn_period_length_2);
        spnPeriodLength2.setAdapter(adpPeriodLength);
        Spinner spnPeriodLength3 = findViewById(R.id.spn_period_length_3);
        spnPeriodLength3.setAdapter(adpPeriodLength);
        Spinner spnPeriodLength4 = findViewById(R.id.spn_period_length_4);
        spnPeriodLength4.setAdapter(adpPeriodLength);
        Spinner spnPeriodLength5 = findViewById(R.id.spn_period_length_5);
        spnPeriodLength5.setAdapter(adpPeriodLength);
        Spinner spnPeriodLength6 = findViewById(R.id.spn_period_length_6);
        spnPeriodLength6.setAdapter(adpPeriodLength);

        //check prefs for previosly set times
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        int spnPeriod1Index = prefs.getInt("spinnerPeriod1Index", 0);
        int spnPeriod2Index = prefs.getInt("spinnerPeriod2Index", 0);
        int spnPeriod3Index = prefs.getInt("spinnerPeriod3Index", 0);
        int spnPeriod4Index = prefs.getInt("spinnerPeriod4Index", 0);
        int spnPeriod5Index = prefs.getInt("spinnerPeriod5Index", 0);
        int spnPeriod6Index =  prefs.getInt("spinnerPeriod6Index", 0);

        spnPeriodLength1.setSelection(spnPeriod1Index);
        spnPeriodLength2.setSelection(spnPeriod2Index);
        spnPeriodLength3.setSelection(spnPeriod3Index);
        spnPeriodLength4.setSelection(spnPeriod4Index);
        spnPeriodLength5.setSelection(spnPeriod5Index);
        spnPeriodLength6.setSelection(spnPeriod6Index);

        spnPeriodLength1.setOnItemSelectedListener(new PeriodSpinnerOnItemSelectedListener("spinnerPeriod1Index"));
        spnPeriodLength2.setOnItemSelectedListener(new PeriodSpinnerOnItemSelectedListener("spinnerPeriod2Index"));
        spnPeriodLength3.setOnItemSelectedListener(new PeriodSpinnerOnItemSelectedListener("spinnerPeriod3Index"));
        spnPeriodLength4.setOnItemSelectedListener(new PeriodSpinnerOnItemSelectedListener("spinnerPeriod4Index"));
        spnPeriodLength5.setOnItemSelectedListener(new PeriodSpinnerOnItemSelectedListener("spinnerPeriod5Index"));
        spnPeriodLength6.setOnItemSelectedListener(new PeriodSpinnerOnItemSelectedListener("spinnerPeriod6Index"));

        Button btn = findViewById(R.id.btn_add_period_1);
        btn.setOnClickListener(new AddPeriodButtonClickListener((Spinner) findViewById(R.id.spn_period_length_1)));
        btn = findViewById(R.id.btn_add_period_2);
        btn.setOnClickListener(new AddPeriodButtonClickListener((Spinner) findViewById(R.id.spn_period_length_2)));
        btn = findViewById(R.id.btn_add_period_3);
        btn.setOnClickListener(new AddPeriodButtonClickListener((Spinner) findViewById(R.id.spn_period_length_3)));
        btn = findViewById(R.id.btn_add_period_4);
        btn.setOnClickListener(new AddPeriodButtonClickListener((Spinner) findViewById(R.id.spn_period_length_4)));
        btn = findViewById(R.id.btn_add_period_5);
        btn.setOnClickListener(new AddPeriodButtonClickListener((Spinner) findViewById(R.id.spn_period_length_5)));
        btn = findViewById(R.id.btn_add_period_6);
        btn.setOnClickListener(new AddPeriodButtonClickListener((Spinner) findViewById(R.id.spn_period_length_6)));

    }



    private class ScheduleAdapter extends ArrayAdapter<Schedule.Period>{

        private ArrayList<Schedule.Period> periods;
        private int selectedIndex;
        private View cvLastView;



        public ScheduleAdapter(Context context, int layoutResourceId, ArrayList<Schedule.Period> periods){
            super(context, layoutResourceId, periods);
            this.periods = periods;
            selectedIndex = -1;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            //return super.getView(position, convertView, parent);

            View v = convertView;
            final int periodPosition = position;
            if (v == null){
                v = LayoutInflater.from(getContext()).inflate(R.layout.row_schedule_listview, parent, false);
            }

            int adjustedPeriod = includePeriodZero ? position : position + 1;
            TextView textViewPeriodLength = v.findViewById(R.id.text_period_info);
            String periodInfo = String.format("%d: %s", adjustedPeriod, periods.get(position).toString());
            textViewPeriodLength.setText(periodInfo);

            if (position == selectedIndex){
                v.setBackgroundResource(R.color.colorAccent);
                cvLastView = v;
            }
            else {
                v.setBackgroundColor(Color.TRANSPARENT);
            }

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (selectedIndex != -1) {
                        if (cvLastView != null)
                            cvLastView.setBackgroundColor(Color.TRANSPARENT);
                    }

                    if (selectedIndex != periodPosition) {
                        cvLastView = view;
                        selectedIndex = periodPosition;
                        view.setBackgroundResource(R.color.colorAccent);
                    }
                    else {
                        cvLastView = null;
                        selectedIndex = -1;
                        view.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            });

            Button btnUp = v.findViewById(R.id.btn_listview_up);
            Button btnDown = v.findViewById(R.id.btn_listview_down);
            Button btnDelete = v.findViewById(R.id.btn_listview_delete);

            btnUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (schedule.movePeriodUp(periodPosition)){
                        selectedIndex = periodPosition - 1;
                        adpSchedule.notifyDataSetChanged();
                    }
                }
            });

            btnDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (schedule.movePeriodDown(periodPosition)){
                        selectedIndex = periodPosition + 1;
                        adpSchedule.notifyDataSetChanged();
                    }
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    schedule.deletePeriod(periodPosition);
                    selectedIndex = periodPosition;
                    if (selectedIndex >= getCount()){
                        selectedIndex--;
                    }
                    adpSchedule.notifyDataSetChanged();
                    updateTitleBar();
                }
            });

            return v;
        }


        public void setSelectedIndex(int selectedIndex) {
            this.selectedIndex = selectedIndex;
        }

        public int getSelectedIndex() {
            return selectedIndex;
        }
    }

    private class AddPeriodButtonClickListener implements View.OnClickListener{

        private Spinner spnPeriodLength;

        public AddPeriodButtonClickListener(Spinner spinnerPeriodLength){
            this.spnPeriodLength = spinnerPeriodLength;
        }

        @Override
        public void onClick(View view) {
            int selectedIndex = adpSchedule.getSelectedIndex();
            if (selectedIndex == -1) {
                schedule.addPeriod((spnPeriodLength.getSelectedItemPosition() + 1) * 30);
                adpSchedule.setSelectedIndex(adpSchedule.getCount() - 1);
            }
            else {
                schedule.addPeriod(selectedIndex, (spnPeriodLength.getSelectedItemPosition() + 1) * 30);
                adpSchedule.setSelectedIndex(selectedIndex + 1);
            }
            adpSchedule.notifyDataSetChanged();
            lvSchedule.smoothScrollToPosition(adpSchedule.getSelectedIndex());
            updateTitleBar();
        }
    }

    private class PeriodSpinnerOnItemSelectedListener implements AdapterView.OnItemSelectedListener{

        String prefKey;

        public PeriodSpinnerOnItemSelectedListener(String prefKey){
            this.prefKey = prefKey;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            prefs.edit().putInt(prefKey, i).commit();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }

    }

}
