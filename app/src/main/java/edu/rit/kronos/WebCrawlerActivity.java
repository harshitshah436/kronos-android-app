package edu.rit.kronos;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

public class WebCrawlerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_crawler);

        Log.d("WebCrawler", "On Create    ");

        String result =  getTimeCard();
        TextView textView = new TextView(this);
        textView.setTextSize(12);
        textView.setText(result);
        ViewGroup layout = (ViewGroup) findViewById(R.id.activity_web_crawler);
        layout.addView(textView);
    }

    private String getTimeCard() {
        StringBuilder sb = new StringBuilder();
        sb.append("hello");
        return sb.toString();
    }
}
