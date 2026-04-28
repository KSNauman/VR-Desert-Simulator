package com.example.vrdesert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        TextView title = findViewById(R.id.titleText);
        TextView subtitle = findViewById(R.id.subtitleText);
        Button btnBegin = findViewById(R.id.btnBegin);

        // Fade-in animations
        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        subtitle.setAlpha(0f);
        subtitle.animate().alpha(1f).setStartDelay(400).setDuration(800).start();

        btnBegin.setAlpha(0f);
        btnBegin.animate().alpha(1f).setStartDelay(800).setDuration(800).start();

        btnBegin.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, StoryActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }
}
