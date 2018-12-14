package test.com.livetest;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.github.faucamp.simplertmp.RtmpHandler;
import com.seu.magicfilter.utils.MagicFilterType;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

import io.vov.vitamio.utils.Log;

/**
 * Created by Sikang on 2017/5/2.
 */

public class CameraActivity extends Activity implements SrsEncodeHandler.SrsEncodeListener, RtmpHandler.RtmpListener, SrsRecordHandler.SrsRecordListener, View.OnClickListener, MediaRecorder.OnInfoListener {
    private static final String TAG = "CameraActivity";

    private Button mPublishBtn;
    private Button mRecordBtn;
    private Button mCameraSwitchBtn;
    private Button mEncoderBtn;
    private EditText mRempUrlEt;
    private SrsCameraView mCameraView;
    private SrsPublisher mPublisher;
    private String rtmpUrl;
    private String recPath = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private final int UPLOAD_FILE = 100002;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message paramMessage) {
            switch (paramMessage.what) {
                case UPLOAD_FILE:
                    Toast.makeText(getApplicationContext(), "Upload file now", Toast.LENGTH_SHORT).show();
                    new Thread(runnableInit).start();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        mPublishBtn = (Button) findViewById(R.id.publish);
        mRecordBtn = (Button) findViewById(R.id.record);
        mCameraSwitchBtn = (Button) findViewById(R.id.swCam);
        mEncoderBtn = (Button) findViewById(R.id.swEnc);
        mRempUrlEt = (EditText) findViewById(R.id.url);
        mPublishBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mCameraSwitchBtn.setOnClickListener(this);
        mEncoderBtn.setOnClickListener(this);

        mCameraView = (SrsCameraView) findViewById(R.id.glsurfaceview_camera);
        mPublisher = new SrsPublisher(mCameraView);
        //编码状态回调
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        //rtmp推流状态回调
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        //预览分辨率
        mPublisher.setPreviewResolution(1280, 720);
        //推流分辨率
        mPublisher.setOutputResolution(1280, 720);
        //传输率
//        mPublisher.setVideoHDMode();
        mPublisher.setVideoSmoothMode();
        //开启美颜（其他滤镜效果在MagicFilterType中查看）
//        mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
        //打开摄像头，开始预览（未推流）
//        mPublisher.startCamera();
        mCameraView.setPreviewOrientation(0);
        mPublisher.startCamera();

        mCamera = mPublisher.getCamera();
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedVideoSizes();
        for (int i = 0; i < sizeList.size(); i++) {
            System.out.println("Supported video sizes: width is " + sizeList.get(i).width + ", height is " + sizeList.get(i).height);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //开始/停止推流
            case R.id.publish:
                if (mPublishBtn.getText().toString().contentEquals("开始")) {
                    rtmpUrl = mRempUrlEt.getText().toString();
                    if (TextUtils.isEmpty(rtmpUrl)) {
                        Toast.makeText(getApplicationContext(), "地址不能为空！", Toast.LENGTH_SHORT).show();
                    }
                    mPublisher.startPublish(rtmpUrl);
//                    mPublisher.startCamera();

                    if (mEncoderBtn.getText().toString().contentEquals("软编码")) {
                        Toast.makeText(getApplicationContext(), "当前使用硬编码", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "当前使用软编码", Toast.LENGTH_SHORT).show();
                    }
                    mPublishBtn.setText("停止");
                    mEncoderBtn.setEnabled(false);
                } else if (mPublishBtn.getText().toString().contentEquals("停止")) {
                    mPublisher.stopPublish();
                    mPublisher.stopRecord();
                    mPublishBtn.setText("开始");
                    mEncoderBtn.setEnabled(true);
                }
                break;
            //录制/结束
            case R.id.record:
                if (mRecordBtn.getText().toString().contentEquals("录制")) {
                    startRecordVideo();
                    mRecordBtn.setText("结束");
                } else if (mRecordBtn.getText().toString().contentEquals("结束")) {
                    stopRecordVideo();
                    mRecordBtn.setText("录制");
                }
                break;
            //切换摄像头
            case R.id.swCam:
                mPublisher.switchCameraFace((mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());
                break;
            //切换编码方式
            case R.id.swEnc:
                if (mEncoderBtn.getText().toString().contentEquals("软编码")) {
                    mPublisher.switchToSoftEncoder();
                    mEncoderBtn.setText("硬编码");
                } else if (mEncoderBtn.getText().toString().contentEquals("硬编码")) {
                    mPublisher.switchToHardEncoder();
                    mEncoderBtn.setText("软编码");
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPublisher.resumeRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPublisher.pauseRecord();
        if (mRecordBtn.getText().toString().contentEquals("结束")) {
            stopRecordVideo();
            mRecordBtn.setText("录制");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPublisher.stopPublish();
        mPublisher.stopRecord();
        stopRecordVideo();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPublisher.stopEncode();
        mPublisher.stopRecord();
        mPublisher.setScreenOrientation(newConfig.orientation);
        if (mPublishBtn.getText().toString().contentEquals("停止")) {
            mPublisher.startEncode();
        }
        mPublisher.startCamera();
    }

    @Override
    public void onNetworkWeak() {
        Toast.makeText(getApplicationContext(), "网络型号弱", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNetworkResume() {

    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    private void handleException(Exception e) {
        try {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            mPublishBtn.setText("开始");
        } catch (Exception e1) {
            //
        }
    }

    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoStreaming() {

    }

    @Override
    public void onRtmpAudioStreaming() {

    }

    @Override
    public void onRtmpStopped() {
        Toast.makeText(getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "未连接服务器", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {

    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {

    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {

    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    @Override
    public void onRecordPause() {
        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Toast.makeText(getApplicationContext(), "Maximum Filesize Reached", Toast.LENGTH_SHORT).show();
            stopRecordVideo();
        }
    }

    private void startRecordVideo() {
        mCamera = mPublisher.getCamera();
        configureMediaRecorder();
        Toast.makeText(getApplicationContext(), "Recording file", Toast.LENGTH_SHORT).show();
    }

    private void stopRecordVideo() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            Toast.makeText(getApplicationContext(), "MP4 file saved", Toast.LENGTH_SHORT).show();
            mHandler.sendEmptyMessageDelayed(UPLOAD_FILE, 1000);
        }
    }

    private void configureMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setOnInfoListener(this);
        try {

            // Step 1: Unlock and set camera to MediaRecorder
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);

            // Step 2: Set Sources
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a Camera Parameters
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            /* Fixed video Size: 640 * 480 */
            mMediaRecorder.setVideoSize(640, 480);
            /* Video Frame Rate: 30*/
            mMediaRecorder.setVideoFrameRate(30);
            /* Encoding bit rate: 1 * 1024 * 1024 */
            mMediaRecorder.setVideoEncodingBitRate(500 * 1024);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            // Step 4: Set output file
            mMediaRecorder.setOutputFile(recPath);

            // Set MediaRecorder Max Duration (ms)
            mMediaRecorder.setMaxDuration(60 * 1000);
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Runnable runnableInit = new Runnable() {
        @Override
        public void run() {
            OSSInit();
        }
    };

    private void OSSInit() {
        String endPoint = "http://oss-cn-shenzhen.aliyuncs.com";
        String accessKeyId = "LTAIvIhIJ3JNzkRl";
        String accessKeySecret = "7aZBMS42QqguHTF5cq5uPD7tle8dK3";

        OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000);   // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000);   // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5);    // 最大并发请求数，默认5个
        conf.setMaxErrorRetry(2);   // 失败后最大重试次数，默认2次
        OSS oss = new OSSClient(getApplicationContext(), endPoint, credentialProvider, conf);
        System.out.println("Upload File");
        uploadFile(oss);
    }

    private void uploadFile(OSS oss) {
        // 上传文件
        Log.d("Upload", "Start");
        PutObjectRequest put = new PutObjectRequest("gadsp", "datas/soft/file", recPath);
        try {
            PutObjectResult putObjectResult = oss.putObject(put);
            Log.d("PutObject", "Upload Success");
            Log.d("ETag", putObjectResult.getETag());
            Log.d("RequestId", putObjectResult.getRequestId());
        } catch (ClientException e) {
            // 本地异常如网络异常等
            e.printStackTrace();
        } catch (ServiceException e) {
            // 服务异常
            Log.e("RequestId", e.getRequestId());
            Log.e("ErrorCode", e.getErrorCode());
            Log.e("HostId", e.getHostId());
            Log.e("RawMessage", e.getRawMessage());
        }
    }
}
