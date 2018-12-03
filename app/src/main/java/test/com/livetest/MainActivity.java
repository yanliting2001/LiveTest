package test.com.livetest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mPushBtn;
    private Button mPlayBtn;
    private Button mLocalBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPushBtn = (Button) findViewById(R.id.push_stream_btn);
        mPlayBtn = (Button) findViewById(R.id.play_stream_btn);
        mLocalBtn = (Button) findViewById(R.id.play_video_btn);
        mPushBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mLocalBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();
        switch (v.getId()) {
            case R.id.push_stream_btn:
                bundle.putString("type", "camera");
                Intent intent1 = new Intent(this,CameraActivity.class);
                intent1.putExtras(bundle);
                startActivity(intent1);
                break;
            case R.id.play_stream_btn:
                bundle.putString("type", "playLive");
                Intent intent2 = new Intent(this,PlayerActivity.class);
                intent2.putExtras(bundle);
                startActivity(intent2);
                break;
            case R.id.play_video_btn:
                bundle.putString("type", "playLocal");
                Intent intent3 = new Intent(this,PlayerActivity.class);
                intent3.putExtras(bundle);
                startActivity(intent3);
                break;
        }
    }
}
