package test.com.livetest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.widget.VideoView;

/**
 * Created by Sikang on 2017/5/2.
 */

public class PlayerActivity extends Activity implements View.OnClickListener {
    private String path = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";
    private VideoView mVideoView;
    private EditText mEditText;
    private Button mStartBtn;
    private Button mStopBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        if (!LibsChecker.checkVitamioLibs(this))
            return;
        Bundle bundle = getIntent().getExtras();
        String playerType = bundle.getString("type");
        if (playerType.equals("playLive")) {
            path = Environment.getExternalStorageDirectory() + "/Android/data/test.com.livetest/test.mp4";
        }
        mEditText = (EditText) findViewById(R.id.url);
        mVideoView = (VideoView) findViewById(R.id.surface_view);
        mStartBtn = (Button) findViewById(R.id.start);
        mStopBtn = (Button) findViewById(R.id.stop);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                mEditText.setText(path);
                if (!TextUtils.isEmpty(path)) {
                    mVideoView.setVideoPath(path);
                }
                break;
            case R.id.stop:
                mVideoView.stopPlayback();
                break;
        }
    }


}
