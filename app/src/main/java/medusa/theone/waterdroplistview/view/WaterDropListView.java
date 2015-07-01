/**
 * @file XListView.java
 * @package me.maxwin.view
 * @create Mar 18, 2012 6:28:41 PM
 * @author Maxwin
 * @description An ListView support (a) Pull down to refresh, (b) Pull up to load more.
 * 		Implement IWaterDropListViewListener, and see stopRefresh() / stopLoadMore().
 */
package medusa.theone.waterdroplistview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;

public class WaterDropListView extends ListView implements OnScrollListener,WaterDropListViewHeader.IStateChangedListener {

	private float mLastY = -1; // save event y
	private Scroller mScroller; // used for scroll back
	private OnScrollListener mScrollListener; // user's scroll listener

	// the interface to trigger refresh and load more.
	private IWaterDropListViewListener mListViewListener;

	// -- header view
	private WaterDropListViewHeader mHeaderView;
	// header view content, use it to calculate the Header's height. And hide it
	// when disable pull refresh.
//	private RelativeLayout mHeaderViewContent;
	private boolean mEnablePullRefresh = true;
//	private boolean mPullRefreshing = false; // is refreashing.

	// -- footer view
	private WaterDropListViewFooter mFooterView;
	private boolean mEnablePullLoad;
	private boolean mPullLoading;
	private boolean mIsFooterReady = false;

	// total list items, used to detect is at the bottom of listview.
	private int mTotalItemCount;

	// for mScroller, scroll back from header or footer.
	private ScrollBack mScrollBack;
	private boolean isTouchingScreen = false;//手指是否触摸屏幕

//	private int mStretchHeight; // view开始变形的高度
//	private int mReadyHeight; // view由stretch变成ready的高度
	private final static int SCROLL_DURATION = 400; // scroll back duration
	private final static int PULL_LOAD_MORE_DELTA = 50; // when pull up >= 50px
														// at bottom, trigger
														// load more.
	private final static float OFFSET_RADIO = 1.8f; // support iOS like pull

	private enum ScrollBack{
		header,
		footer
	}												// feature.

	/**
	 * @param context
	 */
	public WaterDropListView(Context context) {
		super(context);
		initWithContext(context);
	}

	public WaterDropListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initWithContext(context);
	}

	public WaterDropListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initWithContext(context);
	}

	private void initWithContext(Context context) {
		mScroller = new Scroller(context, new DecelerateInterpolator());
		// XListView need the scroll event, and it will dispatch the event to
		// user's listener (as a proxy).
		super.setOnScrollListener(this);

		// init header view
		mHeaderView = new WaterDropListViewHeader(context);
		mHeaderView.setStateChangedListener(this);
		addHeaderView(mHeaderView);

		// init footer view
		mFooterView = new WaterDropListViewFooter(context);
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		// make sure XListViewFooter is the last footer view, and only add once.
		if (mIsFooterReady == false) {
			mIsFooterReady = true;
			addFooterView(mFooterView);
		}
		super.setAdapter(adapter);
	}

	/**
	 * enable or disable pull down refresh feature.
	 * 
	 * @param enable
	 */
	/*public void setPullRefreshEnable(boolean enable) {
		mEnablePullRefresh = enable;
		if (!mEnablePullRefresh) { // disable, hide the content
			mHeaderViewContent.setVisibility(View.INVISIBLE);
		} else {
			mHeaderViewContent.setVisibility(View.VISIBLE);
		}
	}*/

	/**
	 * enable or disable pull up load more feature.
	 *
	 * @param enable
	 */
	public void setPullLoadEnable(boolean enable) {
		mEnablePullLoad = enable;
		if (!mEnablePullLoad) {
			mFooterView.hide();
			mFooterView.setOnClickListener(null);
		} else {
			mPullLoading = false;
			mFooterView.show();
			mFooterView.setState(WaterDropListViewFooter.STATE.normal);
			// both "pull up" and "click" will invoke load more.
			mFooterView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mFooterView.setEnabled(false);
					startLoadMore();
				}
			});
		}
	}

	/**
	 * stop refresh, reset header view.
	 */
	public void stopRefresh() {
		if (mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.refreshing) {
			mHeaderView.updateState(WaterDropListViewHeader.STATE.end);
			if(!isTouchingScreen){
				resetHeaderHeight();
			}
		}else{
			throw  new IllegalStateException("can not stop refresh while it is not refreshing!");
		}
	}

	/**
	 * stop load more, reset footer view.
	 */
	public void stopLoadMore() {
		if (mPullLoading == true) {
			mPullLoading = false;
			mFooterView.setState(WaterDropListViewFooter.STATE.normal);
		}
		mFooterView.setEnabled(true);
	}


	private void invokeOnScrolling() {
		if (mScrollListener instanceof OnXScrollListener) {
			OnXScrollListener l = (OnXScrollListener) mScrollListener;
			l.onXScrolling(this);
		}
	}

	private void updateHeaderHeight(int height){
		if (mEnablePullRefresh) {
			if (mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.normal && height >= mHeaderView.getStretchHeight()) {
				//由normal变成stretch的逻辑：1、当前状态是normal；2、下拉头达到了stretchheight的高度
				mHeaderView.updateState(WaterDropListViewHeader.STATE.stretch);
			} else if(mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.stretch && height >= mHeaderView.getReadyHeight()){
				//由stretch变成ready的逻辑：1、当前状态是stretch；2、下拉头达到了readyheight的高度
				mHeaderView.updateState(WaterDropListViewHeader.STATE.ready);
			}else if (mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.stretch && height < mHeaderView.getStretchHeight()){
				// 由stretch变成normal的逻辑：1、当前状态是stretch；2、下拉头高度小于stretchheight的高度
				mHeaderView.updateState(WaterDropListViewHeader.STATE.normal);
			}else if(mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.end && height < 2){
				//由end变成normal的逻辑：1、当前状态是end；2、下拉头高度小于一个极小值
				mHeaderView.updateState(WaterDropListViewHeader.STATE.normal);
			}
			/*else{
				throw new IllegalStateException("WaterDropListView's state is illegal!");
			}*/
		}
		mHeaderView.setVisiableHeight(height);//动态设置HeaderView的高度
	}

	private void updateHeaderHeight(float delta) {
		int newHeight = (int) delta + mHeaderView.getVisiableHeight();
		updateHeaderHeight(newHeight);
	}

	/**
	 * reset header view's height.
	 * 重置headerheight的高度
	 * 逻辑：1、如果状态处于非refreshing，则回滚到height=0状态2；2、如果状态处于refreshing，则回滚到stretchheight高度
	 */
	private void resetHeaderHeight() {
		int height = mHeaderView.getVisiableHeight();
		if (height == 0) {
		// not visible.
			return;
		}
		// refreshing and header isn't shown fully. do nothing.
		if (mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.refreshing && height <= mHeaderView.getStretchHeight()) {
			return;
		}
		int finalHeight = 0; // default: scroll back to dismiss header.
		// is refreshing, just scroll back to show all the header.
		if ((mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.ready ||mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.refreshing )&& height > mHeaderView.getStretchHeight()) {
			finalHeight = mHeaderView.getStretchHeight();
		}

		mScrollBack = ScrollBack.header;
		mScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);
		// trigger computeScroll
		invalidate();
	}

	private void updateFooterHeight(float delta) {
		int height = mFooterView.getBottomMargin() + (int) delta;
		if (mEnablePullLoad && !mPullLoading) {
			if (height > PULL_LOAD_MORE_DELTA) { // height enough to invoke load
													// more.
				mFooterView.setState(WaterDropListViewFooter.STATE.ready);
			} else {
				mFooterView.setState(WaterDropListViewFooter.STATE.normal);
			}
		}
		mFooterView.setBottomMargin(height);

		// setSelection(mTotalItemCount - 1); // scroll to bottom
	}


	private void resetFooterHeight() {
		int bottomMargin = mFooterView.getBottomMargin();
		if (bottomMargin > 0) {
			mScrollBack = ScrollBack.footer;
			mScroller.startScroll(0, bottomMargin, 0, -bottomMargin, SCROLL_DURATION);
			invalidate();
		}
	}

	private void startLoadMore() {
		mPullLoading = true;
		mFooterView.setState(WaterDropListViewFooter.STATE.loading);
		if (mListViewListener != null) {
			mListViewListener.onLoadMore();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mLastY == -1) {
			mLastY = ev.getRawY();
		}

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastY = ev.getRawY();
			isTouchingScreen = true;
			break;
		case MotionEvent.ACTION_MOVE:
			final float deltaY = ev.getRawY() - mLastY;
			mLastY = ev.getRawY();
			if (getFirstVisiblePosition() == 0 && (mHeaderView.getVisiableHeight() > 0 || deltaY > 0)) {
				// the first item is showing, header has shown or pull down.
				updateHeaderHeight(deltaY / OFFSET_RADIO);
				invokeOnScrolling();
			} else if (getLastVisiblePosition() == mTotalItemCount - 1 && (mFooterView.getBottomMargin() > 0 || deltaY < 0)) {
				// last item, already pulled up or want to pull up.
				updateFooterHeight(-deltaY / OFFSET_RADIO);
			}
			break;
		default:
			mLastY = -1; // reset
			isTouchingScreen = false;
			//TODO 存在bug：当两个if的条件都满足的时候，只能滚动一个，所以在reSetHeader的时候就不起作用了，一般就只会reSetFooter
			if (getFirstVisiblePosition() == 0) {
				resetHeaderHeight();
			}
			if (getLastVisiblePosition() == mTotalItemCount - 1) {
				// invoke load more.
				if (mEnablePullLoad && mFooterView.getBottomMargin() > PULL_LOAD_MORE_DELTA) {
					startLoadMore();
				}
				resetFooterHeight();
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			if (mScrollBack == ScrollBack.header) {
				updateHeaderHeight(mScroller.getCurrY());
				if(mScroller.getCurrY() < 2 && mHeaderView.getCurrentState() == WaterDropListViewHeader.STATE.end){
					//停止滚动了
					//逻辑：如果header范围进入了一个极小值内，且当前的状态是end，就把状态置成normal
					mHeaderView.updateState(WaterDropListViewHeader.STATE.normal);
				}
			} else {
				mFooterView.setBottomMargin(mScroller.getCurrY());
			}
			postInvalidate();
			invokeOnScrolling();
		}
		super.computeScroll();
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		mScrollListener = l;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mScrollListener != null) {
			mScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// send to user's listener
		mTotalItemCount = totalItemCount;
		if (mScrollListener != null) {
			mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}
	}
	@Override
	public void notifyStateChanged(WaterDropListViewHeader.STATE oldState, WaterDropListViewHeader.STATE newState) {
		if(newState == WaterDropListViewHeader.STATE.refreshing){
			if(mListViewListener != null){
				mListViewListener.onRefresh();
			}
		}
	}
	public void setWaterDropListViewListener(IWaterDropListViewListener l) {
		mListViewListener = l;
	}

	/**
	 * you can listen ListView.OnScrollListener or this one. it will invoke onXScrolling when header/footer scroll back.
	 */
	public interface OnXScrollListener extends OnScrollListener {
		public void onXScrolling(View view);
	}

	/**
	 * implements this interface to get refresh/load more event.
	 */
	public interface IWaterDropListViewListener {
		public void onRefresh();

		public void onLoadMore();
	}
}
