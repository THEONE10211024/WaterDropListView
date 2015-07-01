/**
 * @file XListViewHeader.java
 * @create Apr 18, 2012 5:22:27 PM
 * @author Maxwin
 * @description XListView's header
 */
package medusa.theone.waterdroplistview.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import medusa.theone.waterdroplistview.R;
import medusa.theone.waterdroplistview.utils.Utils;

public class WaterDropListViewHeader extends FrameLayout {
    private LinearLayout mContainer;
    private ProgressBar mProgressBar;
    private WaterDropView mWaterDropView;
    private STATE mState = STATE.normal;
    private IStateChangedListener mStateChangedListener;

    private int stretchHeight;
    private  int readyHeight;
    private static final int DISTANCE_BETWEEN_STRETCH_READY = 250;

    public enum STATE {
        normal,//正常
        stretch,//准备进行拉伸
        ready,//拉伸到最大位置
        refreshing,//刷新
        end//刷新结束，回滚
    }

    public WaterDropListViewHeader(Context context) {
        super(context);
        initView(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public WaterDropListViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mContainer = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.waterdroplistview_header, null);
        mProgressBar = (ProgressBar) mContainer.findViewById(R.id.waterdroplist_header_progressbar);
        mWaterDropView = (WaterDropView) mContainer.findViewById(R.id.waterdroplist_waterdrop);
        // 初始情况，设置下拉刷新view高度为0
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, 0);
        addView(mContainer, lp);
        initHeight();
    }

    private void initHeight(){

        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
                stretchHeight =  mWaterDropView.getHeight();
                readyHeight = stretchHeight + DISTANCE_BETWEEN_STRETCH_READY;
				getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});

    }
    /**
     * 修改状态。注：状态的改变与前一个状态以及下拉头高度有关
     *
     * @param state
     */
    public void updateState(STATE state) {
        if (state == mState) return;
        STATE oldState = mState;
        mState = state;
        if (mStateChangedListener != null) {
            mStateChangedListener.notifyStateChanged(oldState, mState);
        }

        switch (mState) {
            case normal:
                handleStateNormal();
                break;
            case stretch:
                handleStateStretch();
                break;
            case ready:
                handleStateReady();
                break;
            case refreshing:
                handleStateRefreshing();
                break;
            case end:
                handleStateEnd();
                break;
            default:
        }
    }

    /**
     * 处理处于normal状态的值
     */
    private void handleStateNormal() {
        mWaterDropView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mContainer.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    }

    /**
     * 处理水滴拉伸状态
     */
    private void handleStateStretch() {
        mWaterDropView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mContainer.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    }

    /**
     * 处理水滴ready状态，回弹效果
     */
    private void handleStateReady() {
        mWaterDropView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        Animator shrinkAnimator = mWaterDropView.createAnimator();
        shrinkAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //回弹结束后即进入refreshing状态
                updateState(STATE.refreshing);
            }
        });
        shrinkAnimator.start();//开始回弹
    }

    /**
     * 处理正在进行刷新状态
     */
    private void  handleStateRefreshing() {
        mWaterDropView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * 处理刷新完毕状态
     */
    private void handleStateEnd() {
        mWaterDropView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
    }

    public void setVisiableHeight(int height) {
        if (height < 0)
            height = 0;
        LayoutParams lp = (LayoutParams) mContainer
                .getLayoutParams();
        lp.height = height;
        mContainer.setLayoutParams(lp);
        //通知水滴进行更新
        if(mState == STATE.stretch){
            float pullOffset = (float) Utils.mapValueFromRangeToRange(height, stretchHeight, readyHeight, 0, 1);
            if(pullOffset < 0 || pullOffset >1){
                throw new IllegalArgumentException("pullOffset should between 0 and 1!"+mState+" "+height);
            }
            Log.e("pullOffset", "pullOffset:" + pullOffset);
            mWaterDropView.updateComleteState(pullOffset);
        }

    }


    public int getVisiableHeight() {
        return mContainer.getHeight();
    }

    public STATE getCurrentState() {
        return mState;
    }

    public int getStretchHeight() {
        return stretchHeight;
    }

    public int getReadyHeight() {
        return readyHeight;
    }

    public void setStateChangedListener(IStateChangedListener l) {
        mStateChangedListener = l;
    }

    public interface IStateChangedListener {
        public void notifyStateChanged(STATE oldState, STATE newState);
    }
}
