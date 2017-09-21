package video.com.videoplayer.view;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Formatter;

import video.com.videoplayer.R;


/**
 * Created by zxs 2017/9/17
 */

public class VideoPlayerView extends FrameLayout implements MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback {
    private static final int UPDATE_PROGRESS = 1;
    private static final int AUTO_HIDE_CONTROL_BAR = 2;
    private static final int AUTO_HIDE_CONTROL_BAR_TIME_COUNT = 5000;
    private SeekBar mSeekBar;
    private ImageView mPlayOrPause;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private Context mContext;
    private SurfaceView mSurfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView mClose;
    private ImageView mPhotoAlbum;
    private RelativeLayout mRlControlBar;
    private LinearLayout mBottomBar;
    private MediaPlayer mMediaPlayer;
    private ImageView mFullScreenPlay;
    private View mRootView;
    private Uri mUri;
    private VideoPlayerListener mListener;
    private Handler handler = new TimerHandler(this);
    private boolean isShowBar = true;
    private boolean isDragging;
    private boolean isShowBtn;
    private OrientationEventListener mOrientationEventListener;

    public VideoPlayerView(Context context) {
        super(context);
        initView(context);
    }


    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        mContext = context;
        mRootView = View.inflate(mContext, R.layout.video_player_layout, this);
        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mCurrentTime = (TextView) findViewById(R.id.tv_begin_time);
        mTotalTime = (TextView) findViewById(R.id.tv_total_time);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv_video);
        mClose = (ImageView) findViewById(R.id.iv_close);
        mPhotoAlbum = (ImageView) findViewById(R.id.iv_photo_album);
        mPlayOrPause = (ImageView) findViewById(R.id.iv_play_or_pause);
        mRlControlBar = (RelativeLayout) findViewById(R.id.rl_control_bar);
        mFullScreenPlay = (ImageView) findViewById(R.id.iv_full_screen_play);
        mBottomBar = (LinearLayout) findViewById(R.id.ll_bottom);
        surfaceHolder = mSurfaceView.getHolder();
        mRootView.setOnTouchListener(mOnTouchListener);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        initClick();

    }

    private void initClick() {
        mClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).finish();
                }
            }
        });
        mPlayOrPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mPlayOrPause.setImageResource(R.mipmap.video_play_btn);
                    mFullScreenPlay.setVisibility(View.GONE);
                    handler.removeMessages(AUTO_HIDE_CONTROL_BAR);
                } else {
                    onPlay();
                }
            }
        });

        mFullScreenPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay();
            }
        });
        mPhotoAlbum.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPhotoAlbum();
            }
        });
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mMediaPlayer == null || !fromUser) {
                return;
            }
            updateSeek(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (mMediaPlayer == null) {
                return;
            }
            isDragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mMediaPlayer == null) {
                return;
            }
            isDragging = false;
            updateSeek(seekBar.getProgress());

        }
    };

    public void setVideoPlayerListener(VideoPlayerListener listener) {
        mListener = listener;
    }


    private void setProgress() {
        if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
            return;
        }
        int position = mMediaPlayer.getCurrentPosition();
        int duration = mMediaPlayer.getDuration();
        mSeekBar.setProgress(position);
        mSeekBar.setMax(duration);
        mCurrentTime.setText(formatTime(position));
    }

    /**
     * 拖动进度条时更新
     *
     * @param progress
     */
    private void updateSeek(int progress) {
        int duration = mMediaPlayer.getDuration();
        mCurrentTime.setText(formatTime(progress));
        mMediaPlayer.seekTo(progress);
        if (progress < duration) {
            onPlay();
        }
    }


    private MediaPlayer.OnPreparedListener mOnPrepared = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mTotalTime.setText(formatTime(mMediaPlayer.getDuration()));
            onPlay();
        }
    };
    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mPlayOrPause.setImageResource(R.mipmap.video_play_btn);
            mSeekBar.setProgress(mSeekBar.getMax());
            mMediaPlayer.seekTo(0);
            mFullScreenPlay.setVisibility(View.VISIBLE);
        }
    };

    private void onPlay() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            mPlayOrPause.setImageResource(R.mipmap.video_pause_btn);
            mFullScreenPlay.setVisibility(View.GONE);
            handler.sendEmptyMessage(UPDATE_PROGRESS);
            handler.removeMessages(AUTO_HIDE_CONTROL_BAR);
            if (!isDragging) {
                handler.sendEmptyMessageDelayed(AUTO_HIDE_CONTROL_BAR, AUTO_HIDE_CONTROL_BAR_TIME_COUNT);
            }
        }

    }

    private void hideBar() {
        isShowBar = true;
        mRlControlBar.setVisibility(View.GONE);
        mBottomBar.setVisibility(View.GONE);
    }

    private void showBar() {
        handler.removeMessages(AUTO_HIDE_CONTROL_BAR);
        isShowBar = false;
        mRlControlBar.setVisibility(View.VISIBLE);
        mBottomBar.setVisibility(View.VISIBLE);
        if (mMediaPlayer.isPlaying() && !isDragging) {
            handler.sendEmptyMessageDelayed(AUTO_HIDE_CONTROL_BAR, AUTO_HIDE_CONTROL_BAR_TIME_COUNT);
        }
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (isShowBtn) {
                        if (isShowBar) {
                            showBar();
                        } else {
                            hideBar();
                        }
                    } else {
                        if (mMediaPlayer != null) {
                            if (mMediaPlayer.isPlaying()) {
                                mMediaPlayer.pause();
                                mFullScreenPlay.setVisibility(View.VISIBLE);
                            } else {
                                mMediaPlayer.start();
                                mFullScreenPlay.setVisibility(View.GONE);
                            }
                        }
                    }

                    break;
            }
            return true;
        }
    };


    /**
     * 视频按等比例计算
     *
     * @param width
     * @param height
     */
    private void updateVideoSize(int width, int height) {
        int targetWidth = 0;
        int targetHeight = 0;
        if (width * getHeight() > height * getWidth()) {
            targetWidth = getWidth();
            targetHeight = (int) (1f * height * getWidth() / width);
        } else if (width * getHeight() < height * getWidth()) {
            targetHeight = getHeight();
            targetWidth = (int) (1f * width * getHeight() / height);
        }
        if (targetWidth != 0 && targetHeight != 0) {
            LayoutParams videoViewParam = new LayoutParams(targetWidth, targetHeight);
            videoViewParam.gravity = Gravity.CENTER;
            mSurfaceView.setLayoutParams(videoViewParam);
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        updateVideoSize(width, height);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mMediaPlayer = new MediaPlayer();
        if (mUri == null || surfaceHolder == null) {
            return;
        }
        try {
            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.setDisplay(surfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(mOnPrepared);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mOrientationEventListener = new OrientationEventListener(mContext) {
                @Override
                public void onOrientationChanged(int orientation) {
                    updateVideoSize(mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
                }
            };
            mOrientationEventListener.enable();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        release();

    }

    private static class TimerHandler extends Handler {
        private WeakReference<VideoPlayerView> viewWeakReference;

        TimerHandler(VideoPlayerView videoPlayerView) {
            viewWeakReference = new WeakReference<>(videoPlayerView);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoPlayerView videoPlayerView = viewWeakReference.get();

            if (videoPlayerView != null) {
                switch (msg.what) {
                    case UPDATE_PROGRESS://进度条及时间
                        videoPlayerView.setProgress();
                        videoPlayerView.handler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 100);
                        break;
                    case AUTO_HIDE_CONTROL_BAR://是否隐藏控制条
                        videoPlayerView.hideBar();
                        break;
                }
            }
            super.handleMessage(msg);
        }
    }


    /**
     * @param url       要播放的url路径
     * @param isShowBtn 是否展示头部与底部状态栏
     */
    public void setVideo(String url, boolean isShowBtn) {
        mUri = Uri.parse(url);
        this.isShowBtn = isShowBtn;
        surfaceHolder.addCallback(this);
    }

    private void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
        }
    }

    public String formatTime(long timeMs) {
        if (timeMs <= 0) {
            return "00:00";
        }
        long totalSeconds = timeMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = totalSeconds / 60 % 60;
        long hours = totalSeconds / 3600;

        Formatter formatter = new Formatter();
        return hours > 0
                ? formatter.format("%d:%02d:%02d", new Object[]{hours, minutes, seconds}).toString()
                : formatter.format("%02d:%02d", new Object[]{minutes, seconds}).toString();
    }

    public interface VideoPlayerListener {

        void onPhotoAlbum();
    }


}
