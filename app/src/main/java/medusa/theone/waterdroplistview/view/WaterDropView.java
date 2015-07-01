package medusa.theone.waterdroplistview.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import medusa.theone.waterdroplistview.R;
import medusa.theone.waterdroplistview.entity.Circle;
import medusa.theone.waterdroplistview.utils.Utils;

/**
 * Created by xiayong on 2015/6/23.
 * 下拉头中间的“水滴”
 */
public class WaterDropView extends View {

    private Circle topCircle;
    private Circle bottomCircle;

    private Paint mPaint;
    private Path mPath;
    private float mMaxCircleRadius;//圆半径最大值
    private float mMinCircleRaidus;//圆半径最小值
    private Bitmap arrowBitmap;//箭头
    private final static int BACK_ANIM_DURATION = 180;
    private final static float STROKE_WIDTH = 2;//边线宽度

    public WaterDropView(Context context) {
        super(context);
        init(context, null);
    }

    public WaterDropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WaterDropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void parseAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaterDropView, 0, 0);
            try {
                /*if (a.hasValue(R.styleable.WaterDropView_topcircle_x)) {
                    topCircle.setX(a.getDimensionPixelSize(R.styleable.WaterDropView_topcircle_x, 0));
                }
                if (a.hasValue(R.styleable.WaterDropView_topcircle_y)) {
                    topCircle.setY(a.getDimensionPixelSize(R.styleable.WaterDropView_topcircle_y, 0));
                }
                if (a.hasValue(R.styleable.WaterDropView_bottomcircle_x)) {
                    bottomCircle.setX(a.getDimensionPixelSize(R.styleable.WaterDropView_topcircle_x, 0));
                }
                if (a.hasValue(R.styleable.WaterDropView_bottomcircle_y)) {
                    bottomCircle.setY(a.getDimensionPixelSize(R.styleable.WaterDropView_topcircle_y, 0));
                }*/
                if(a.hasValue(R.styleable.WaterDropView_waterdrop_color)){
                   int waterDropColor =  a.getColor(R.styleable.WaterDropView_waterdrop_color, Color.GRAY);
                    mPaint.setColor(waterDropColor);
                }
                if (a.hasValue(R.styleable.WaterDropView_max_circle_radius)) {
                    mMaxCircleRadius = a.getDimensionPixelSize(R.styleable.WaterDropView_max_circle_radius, 0);

                    topCircle.setRadius(mMaxCircleRadius);
                    bottomCircle.setRadius(mMaxCircleRadius);

                    topCircle.setX(STROKE_WIDTH + mMaxCircleRadius);
                    topCircle.setY(STROKE_WIDTH + mMaxCircleRadius);

                    bottomCircle.setX(STROKE_WIDTH + mMaxCircleRadius);
                    bottomCircle.setY(STROKE_WIDTH + mMaxCircleRadius);
                }
                if (a.hasValue(R.styleable.WaterDropView_min_circle_radius)) {
                    mMinCircleRaidus = a.getDimensionPixelSize(R.styleable.WaterDropView_min_circle_radius, 0);
                    if (mMinCircleRaidus > mMaxCircleRadius) {
                        throw new IllegalStateException("Circle's MinRaidus should be equal or lesser than the MaxRadius");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                a.recycle();
            }
        }
    }

    private void init(Context context, AttributeSet attrs) {
        topCircle = new Circle();
        bottomCircle = new Circle();
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setColor(Color.GRAY);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        Drawable drawable = getResources().getDrawable(R.drawable.refresh_arrow);
        arrowBitmap = Utils.drawableToBitmap(drawable);
        parseAttrs(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //宽度：上圆和下圆的最大直径
        int width = (int) ((mMaxCircleRadius + STROKE_WIDTH) * 2);
        //高度：上圆半径 + 圆心距 + 下圆半径
        int height = (int) Math.ceil(bottomCircle.getY()+bottomCircle.getRadius() + STROKE_WIDTH * 2);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        makeBezierPath();
//        mPaint.setColor(Color.RED);
//        mPaint.setAlpha(200);
        canvas.drawPath(mPath, mPaint);
//        mPaint.setColor(Color.GRAY);
//        mPaint.setAlpha(50);
        canvas.drawCircle(topCircle.getX(), topCircle.getY(), topCircle.getRadius(), mPaint);
        canvas.drawCircle(bottomCircle.getX(), bottomCircle.getY(), bottomCircle.getRadius(), mPaint);
//        canvas.drawBitmap(arrowBitmap, topCircle.getX() - topCircle.getRadius(), topCircle.getY() - topCircle.getRadius(), mPaint);
        RectF bitmapArea = new RectF(topCircle.getX()-0.5f*topCircle.getRadius(),topCircle.getY()-0.5f*topCircle.getRadius(),topCircle.getX()+ 0.5f*topCircle.getRadius(),topCircle.getY()+0.5f*topCircle.getRadius());
        canvas.drawBitmap(arrowBitmap,null,bitmapArea,mPaint);
        super.onDraw(canvas);
    }


    private void makeBezierPath() {
        mPath.reset();
        //获取两圆的两个切线形成的四个切点
        double angle = getAngle();
        float top_x1 = (float) (topCircle.getX() - topCircle.getRadius() * Math.cos(angle));
        float top_y1 = (float) (topCircle.getY() + topCircle.getRadius() * Math.sin(angle));

        float top_x2 = (float) (topCircle.getX() + topCircle.getRadius() * Math.cos(angle));
        float top_y2 = top_y1;

        float bottom_x1 = (float) (bottomCircle.getX() - bottomCircle.getRadius() * Math.cos(angle));
        float bottom_y1 = (float) (bottomCircle.getY() + bottomCircle.getRadius() * Math.sin(angle));

        float bottom_x2 = (float) (bottomCircle.getX() + bottomCircle.getRadius() * Math.cos(angle));
        float bottom_y2 = bottom_y1;

        mPath.moveTo(topCircle.getX(), topCircle.getY());

        mPath.lineTo(top_x1, top_y1);

        mPath.quadTo((bottomCircle.getX() - bottomCircle.getRadius()),
                (bottomCircle.getY() + topCircle.getY()) / 2,

                bottom_x1,
                bottom_y1);
        mPath.lineTo(bottom_x2, bottom_y2);

        mPath.quadTo((bottomCircle.getX() + bottomCircle.getRadius()),
                (bottomCircle.getY() + top_y2) / 2,
                top_x2,
                top_y2);

        mPath.close();
    }

    /**
     * 获得两个圆切线与圆心连线的夹角
     *
     * @return
     */
    private double getAngle() {
        if (bottomCircle.getRadius() > topCircle.getRadius()) {
            throw new IllegalStateException("bottomCircle's radius must be less than the topCircle's");
        }
        return Math.asin((topCircle.getRadius() - bottomCircle.getRadius()) / (bottomCircle.getY() - topCircle.getY()));
    }

    /**
     * 创建回弹动画
     * 上圆半径减速恢复至最大半径
     * 下圆半径减速恢复至最大半径
     * 圆心距减速从最大值减到0(下圆Y从当前位置移动到上圆Y)。
     *
     * @return
     */
    public Animator createAnimator() {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1, 0).setDuration(BACK_ANIM_DURATION);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                updateComleteState((float) valueAnimator.getAnimatedValue());
            }
        });
        return valueAnimator;
    }

    /**
     * 完成的百分比
     *
     * @param percent between[0,1]
     */
    public void updateComleteState(float percent) {
        if (percent < 0 || percent > 1) {
            throw new IllegalStateException("completion percent should between 0 and 1!");
        }
        float top_r = (float) (mMaxCircleRadius - 0.25 * percent * mMaxCircleRadius);
        float bottom_r = (mMinCircleRaidus - mMaxCircleRadius) * percent + mMaxCircleRadius;
        float bottomCricleOffset = 2 * percent * mMaxCircleRadius;
        topCircle.setRadius(top_r);
        bottomCircle.setRadius(bottom_r);
        bottomCircle.setY(topCircle.getY() + bottomCricleOffset);
        requestLayout();
        postInvalidate();
    }

    public Circle getTopCircle() {
        return topCircle;
    }

    public Circle getBottomCircle() {
        return bottomCircle;
    }

    public void setIndicatorColor(int color) {
        mPaint.setColor(color);
    }

    public int getIndicatorColor() {
        return mPaint.getColor();
    }
}
