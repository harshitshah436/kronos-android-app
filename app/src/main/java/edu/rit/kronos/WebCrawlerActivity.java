package edu.rit.kronos;

import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

public class WebCrawlerActivity extends AppCompatActivity {

    public String kronos_url, kronos_current_timeperiod_url, username, password;
    public SimpleDateFormat sdf1 = new SimpleDateFormat("hh:mm a");
    double today_time = 0;
    long in_time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_crawler);

        Log.d("WebCrawler", "On Create");

        String result = getTimeCard();
        TextView textView = (TextView) findViewById(R.id.text_view1);
        textView.setText(result);
    }

    public void hoursToCompleteToday(View view) {
        EditText editText = (EditText) findViewById(R.id.edit_text);
        String message = editText.getText().toString();
        Log.d("WebCrawler/numHours", message);

        if (message.isEmpty()) {
            // Exit application and back to home screen
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }

        double input = Integer.parseInt(message) * 3600;

        if (input > today_time) {
            message = "\n" + "You should clock out at: " + sdf1.format(new Date((long) (in_time + (input - today_time) * 1000)));
        } else {
            message = "\n" + "You have already completed entered number of hours.";
        }

        TextView textView = (TextView) findViewById(R.id.text_view2);
        textView.setTextSize(12);
        textView.setText(message);
    }

    private String getTimeCard() {
        StringBuilder sb = new StringBuilder();

        try {
            getPropValues();
        } catch (IOException e) {
            e.printStackTrace();
        }

        kronos_current_timeperiod_url = getKronosCurrentTimeCardURL(kronos_current_timeperiod_url);

        ConnectKronos ck = new ConnectKronos();
        String json = null;
        try {
            json = ck.execute(kronos_url, kronos_current_timeperiod_url, username, password).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        Log.d("WebCrawler/getTimeCard", json);

        JSONParser parser = new JSONParser();

        try {
            JSONObject obj = (JSONObject) parser.parse(json);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");

            Date start_date = new Date((long) obj.get("start_date"));
            Date end_date = new Date((long) obj.get("end_date"));

            JSONArray array = (JSONArray) obj.get("summaries");
            double wages = 0, total_time = 0;
            for (Object array1 : array) {
                JSONObject obj1 = (JSONObject) array1;
                wages += Double.parseDouble(obj1.get("wageamount").toString());
                if (obj1.get("name").toString().equalsIgnoreCase("Holiday RIT") && Double.parseDouble(obj1.get("duration").toString()) > 0 && Double.parseDouble(obj1.get("wageamount").toString()) > 0) {
                    total_time += Double.parseDouble(obj1.get("duration").toString());
                }
            }

            sb.append("\n");
            sb.append(sdf.format(start_date)).append(" to ").append(sdf.format(end_date)).append("\t Wages: $").append(String.format("%.2f", wages)).append("\n");
            sb.append("\n");

            if (total_time > 0) {
                sb.append("RIT Holiday hours: ").append(String.format("%6s", formatSeconds((int) total_time))).append("\n");
                sb.append("\n");
            }
            sb.append("=================================================\n");
            sb.append("PUNCHES\n");
            sb.append("=================================================\n");
            sb.append(String.format("%18s", "Date"));
            sb.append(String.format("%12s", "In Punch"));
            sb.append(String.format("%12s", "Out Punch"));
            sb.append(String.format("%10s", "Shift"));
            sb.append(String.format("%10s", "Total")).append("\n");

            boolean shift_over = false, new_shift = false;
            array = (JSONArray) obj.get("punchlist");
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj2 = (JSONObject) array.get(i);

                String startreason = (String) obj2.get("startreason");

                in_time = (long) obj2.get("in_datetime");

                if (startreason.equalsIgnoreCase("newShift")) {
                    sb.append("-------------------------------------------------------------------------------------\n");
                    today_time = 0;
                    new_shift = true;
                } else {
                    new_shift = false;
                }

                sb.append(String.format("%12s", obj2.get("applydate")));

                Date in_datetime = new Date(in_time);
                sb.append(String.format("%12s", sdf1.format(in_datetime)));

                long out_time = (long) obj2.get("out_datetime");
                Date out_datetime = new Date(out_time);
                double duration = (double) obj2.get("duration");
                if (obj2.get("endreason").equals("out")) {
                    sb.append(String.format("%12s", sdf1.format(out_datetime)));
                    sb.append(String.format("%10s", formatSeconds((int) duration)));
                    today_time += duration;
                    shift_over = true;
                } else {
                    sb.append(String.format("%12s", ""));
                    sb.append(String.format("%10s", ""));
                    shift_over = false;
                }

                total_time += duration;
                sb.append(String.format("%10s", formatSeconds((int) total_time))).append("\n");
            }
            sb.append("-------------------------------------------------------------------------------------\n");

            Log.d("WebCrawler", sb.toString());

            double time_40 = total_time > 144000 ? total_time - 144000 : total_time;
            sb.append("\n" + "Time to 40 hours: ").append(formatSeconds((int) (144000 - time_40))).append("\t\t");
            sb.append("Time to 80 hours: ").append(formatSeconds((int) (288000 - total_time))).append("\t").append("\n");
            if (144000 - time_40 <= 21600) {
                sb.append("\n" + "You should clock out at: ").append(sdf1.format(new Date((long) (in_time + (144000 - time_40) * 1000)))).append("\n");
            } else if (288000 - total_time <= 21600) {
                sb.append("\n" + "You should clock out at: ").append(sdf1.format(new Date((long) (in_time + (288000 - total_time) * 1000)))).append("\n");
            }

            if (!shift_over) {
                sb.append("\n" + "Total time worked today as of now (hours and minutes): ").append(formatSeconds((int) (today_time + (System.currentTimeMillis() - in_time) / 1000))).append("\n");
                if (new_shift) {
                    sb.append("\n" + "You should take a break before: ").append(sdf1.format(new Date(in_time + (21600) * 1000))).append("\n");
                }
            }

            sb.append("\n");

            Log.d("WebCrawler", sb.toString());

        } catch (ParseException pe) {

            System.err.println("position: " + pe.getPosition());
            System.err.println(pe);
            //pe.printStackTrace();
        }
        return sb.toString();
    }

    public String formatSeconds(int timeInSeconds) {
        int hours = timeInSeconds / 3600;
        int secondsLeft = timeInSeconds - hours * 3600;
        int minutes = secondsLeft / 60;
        //int seconds = secondsLeft - minutes * 60;

        String formattedTime = "";
        if (hours < 10) {
            formattedTime += "0";
        }
        formattedTime += hours + ":";

        if (minutes < 10) {
            formattedTime += "0";
        }
        formattedTime += minutes;// + ":";

        return formattedTime;
    }

    protected void getPropValues() throws IOException {
        InputStream inputStream;
        try {
            Properties prop = new Properties();
            AssetManager assetManager = getBaseContext().getAssets();

            String propFileName = "config.properties";

            inputStream = assetManager.open(propFileName);

            prop.load(inputStream);

            kronos_url = prop.getProperty("kronos_url");
            kronos_current_timeperiod_url = prop.getProperty("kronos_current_timeperiod_url");
            username = prop.getProperty("username");
            password = prop.getProperty("password");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String getKronosCurrentTimeCardURL(String kronos_current_timeperiod_url) {
        String last_day_date = kronos_current_timeperiod_url.substring(kronos_current_timeperiod_url.lastIndexOf("/") + 1);

        kronos_current_timeperiod_url = kronos_current_timeperiod_url.substring(0, kronos_current_timeperiod_url.lastIndexOf("/"));
        kronos_current_timeperiod_url = kronos_current_timeperiod_url.substring(0, kronos_current_timeperiod_url.lastIndexOf("/"));

        Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
        int CurrentDayOfYear = localCalendar.get(Calendar.DAY_OF_YEAR);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            localCalendar.setTime(sdf.parse(last_day_date));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        int lastDayOf14DayPayPeriod = localCalendar.get(Calendar.DAY_OF_YEAR);
        while (lastDayOf14DayPayPeriod < CurrentDayOfYear) {
            lastDayOf14DayPayPeriod += 14;
        }

        localCalendar.set(Calendar.DAY_OF_YEAR, lastDayOf14DayPayPeriod - 13);
        kronos_current_timeperiod_url += "/" + sdf.format(localCalendar.getTime());

        localCalendar.set(Calendar.DAY_OF_YEAR, lastDayOf14DayPayPeriod);
        kronos_current_timeperiod_url += "/" + sdf.format(localCalendar.getTime());

        return kronos_current_timeperiod_url;
    }

}
