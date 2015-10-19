package practice.taoyulu.togglebutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.gesture.Gesture;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;

import hugo.weaving.DebugLog;


/**
 * TODO: document your custom view class.
 */
public class ToggleButton extends View implements View.OnTouchListener{
    private final String DEBUG_TAG = "ToggleButton";
    private int disabledColor ;
    private int enabledColor;
    private int openBg;
    private int animLayerColor;
    private int transparent;

    private float borderWidth;

    private RectF basicLayer;
    private RectF toggleAnimLayer;
    private RectF openBgLayer;
    private RectF handlerLayer;

    private Paint basicPaint;
    private Paint toggleAnimPaint;
    private Paint openBgPaint;
    private Paint handlerPaint;

    private float basicRadius;
    private float toggleAnimRadius;
    private float openBgRadius;
    private float handlerRadius;

    private float handlerMoveDistance;



    private boolean handlerHolded = false;

    private Point center;



    private float offsetX = 0;
//    private float offsetY = 0;

    private GestureDetector detector;

    private SpringSystem springSystem;
    private Spring springHandlerTouch ;
    private Spring springHandlerMove ;

    private boolean isInitial = true;


    private boolean toggleOn = false;


    public ToggleButton(Context context) {
        super(context);
        init(null, 0);
    }

    public ToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        springSystem = SpringSystem.create();
        springHandlerTouch = springSystem.createSpring();
        springHandlerTouch.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(50, 7));
        springHandlerMove = springSystem.createSpring();
        springHandlerMove.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(50, 7));

        detector = new GestureDetector(getContext(), new MyGestureDetector());
        setOnTouchListener(this);
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ToggleButton, defStyle, 0);
        int defaultDisabledColor = getContext().getResources().getColor(R.color.defaultDisabledColor);
        int defaultEnabledColor = getContext().getResources().getColor(R.color.defaultEnabledColor);
        int defaultBackgroundColor = getContext().getResources().getColor(R.color.defaultBackgroundColor);


        disabledColor = a.getColor(R.styleable.ToggleButton_disabledColor, defaultDisabledColor);
        enabledColor = a.getColor(R.styleable.ToggleButton_enabledColor, defaultEnabledColor);
        openBg = enabledColor;
        transparent = getContext().getResources().getColor(R.color.transparent);
        animLayerColor = getContext().getResources().getColor(R.color.toggleHandlerColor);
        int backgroundColor = a.getColor(R.styleable.ToggleButton_basicColor, defaultBackgroundColor);
        borderWidth = a.getDimension(R.styleable.ToggleButton_borderWidth, 0);
        int toggleState = a.getInt(R.styleable.ToggleButton_defaultState, 0);

        a.recycle();

        if(toggleState == 1){
            toggleOn = true;
        }else {
            toggleOn = false;
        }
        springHandlerMove.setCurrentValue(toggleState);
        springHandlerTouch.addListener(springListenerHandlerTouch);
        springHandlerMove.addListener(springListenerHandlerMove);

        basicPaint = new Paint();
        basicPaint.setAntiAlias(true);
        basicPaint.setColor(backgroundColor);

        openBgPaint = new Paint();
        openBgPaint.setAntiAlias(true);
        openBgPaint.setColor(openBg);
        if (!toggleOn) openBgPaint.setAlpha(0);

        toggleAnimPaint = new Paint();
        toggleAnimPaint.setAntiAlias(true);
        toggleAnimPaint.setColor(animLayerColor);

        handlerPaint = new Paint();
        handlerPaint.setAntiAlias(true);
        handlerPaint.setColor(getResources().getColor(R.color.toggleHandlerColor));
        handlerPaint.setStyle(Paint.Style.FILL);
        handlerPaint.setShadowLayer(borderWidth*2, 0, borderWidth, getResources().getColor(R.color.handlerShadowColor));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        springHandlerTouch.removeListener(springListenerHandlerTouch);
        springHandlerMove.removeListener(springListenerHandlerMove);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    SimpleSpringListener springListenerHandlerTouch = new SimpleSpringListener(){
//        @DebugLog
        @Override
        public void onSpringUpdate(Spring spring) {
            final double value = spring.getCurrentValue();
//            Log.d("rebound", "reboundTest:"+value);
            handlerScaleEffect(value);
            animLayerScale(1 - value);
        }
    };

    SimpleSpringListener springListenerHandlerMove = new SimpleSpringListener(){
        //        @DebugLog
        @Override
        public void onSpringUpdate(Spring spring) {
//            Log.d("rebound", "spring1:"+spring.getCurrentValue());
            handlerMoveEffect( spring.getCurrentValue());
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        if (isInitial) {
            int paddingLeft = getPaddingLeft() + (int)borderWidth*2;
            int paddingTop = getPaddingTop() + (int)borderWidth*2;
            int paddingRight = getPaddingRight() + (int)borderWidth*2;
            int paddingBottom = getPaddingBottom() + (int)borderWidth*2;

            //实际尺寸
            int backgroundWidth = getWidth() - paddingLeft - paddingRight;
            int backgroundHeight = getHeight() - paddingTop - paddingBottom;

            int topPosition = paddingTop;
            int leftPosition = paddingLeft;
            int bottomPosition = topPosition + backgroundHeight;
            int rightPosition = leftPosition + backgroundWidth;


            basicRadius = backgroundHeight/2;
            toggleAnimRadius = backgroundHeight/2 - borderWidth;
            openBgRadius = backgroundHeight/2;
            handlerRadius = backgroundHeight/2 - borderWidth;
            handlerMoveDistance = backgroundWidth - borderWidth*2 - handlerRadius*2;

            basicLayer = new RectF(leftPosition
                    , topPosition
                    , rightPosition
                    , bottomPosition);
            toggleAnimLayer = new RectF(leftPosition + borderWidth
                    , topPosition + borderWidth
                    , rightPosition - borderWidth
                    , bottomPosition - borderWidth);
            openBgLayer = new RectF(leftPosition
                    , topPosition
                    , rightPosition
                    , bottomPosition);

            if(toggleOn){
                handlerLayer = new RectF(rightPosition - backgroundHeight + borderWidth
                    , topPosition + borderWidth
                    , rightPosition - borderWidth
                    , bottomPosition - borderWidth);
            }else {
                handlerLayer = new RectF(leftPosition + borderWidth
                        , topPosition + borderWidth
                        , leftPosition + backgroundHeight - borderWidth
                        , bottomPosition - borderWidth);
            }

            isInitial = false;
        }


        canvas.drawRoundRect(basicLayer, basicRadius, basicRadius, basicPaint);
        canvas.drawRoundRect(toggleAnimLayer, toggleAnimRadius, toggleAnimRadius, toggleAnimPaint);
//        openBgPaint.setColor(openBg);
        canvas.drawRoundRect(openBgLayer, openBgRadius, openBgRadius, openBgPaint);
        canvas.drawRoundRect(handlerLayer, handlerRadius, handlerRadius, handlerPaint);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:{
                handlerHolded = handlerLayer.contains(event.getX(), event.getY());
                if(handlerHolded){
                    springHandlerTouch.setEndValue(1);
                }
                break;
            }

            case MotionEvent.ACTION_UP:{
//                Log.d(DEBUG_TAG, "action_up: " + event.toString());
                if(handlerHolded){
                    springHandlerTouch.setEndValue(0);
                }
                if(handlerLayer.contains(event.getX(), event.getY())){
                    toggle();
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:{
//                Log.d(DEBUG_TAG, "action_move: " + event.toString());
                return true;
            }

        }
        return true;

    }



    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
//    public void setExampleString(String exampleString) {
//        mExampleString = exampleString;
//        invalidateTextPaintAndMeasurements();
//    }


    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
//    public void setExampleColor(int exampleColor) {
//        mExampleColor = exampleColor;
//        invalidateTextPaintAndMeasurements();
//    }


    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
//    public Drawable getExampleDrawable() {
//        return mExampleDrawable;
//    }

//    /**
//     * Sets the view's example drawable attribute value. In the example view, this drawable is
//     * drawn above the text.
//     *
//     * @param exampleDrawable The example drawable attribute value to use.
//     */
//    public void setExampleDrawable(Drawable exampleDrawable) {
//        mExampleDrawable = exampleDrawable;
//    }

        private void animButtonScaling(float value){
            handlerLayer.right = handlerLayer.left + handlerRadius*2 + handlerRadius*0.5f*value;

            this.postInvalidate();
        }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener{

        @Override
        public boolean onDown(MotionEvent event) {
//            Log.d(DEBUG_TAG,"onDown: " + event.toString());
            handlerHolded = handlerLayer.contains(event.getX(), event.getY());
                if(handlerHolded){
                    springHandlerTouch.setEndValue(1);
                }
            return true;
        }

//        @Override
//        public boolean onFling(MotionEvent event1, MotionEvent event2,
//                               float velocityX, float velocityY) {
//            Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
//            return true;
//        }

//        @Override
//        public void onLongPress(MotionEvent event) {
//            Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
//        }

//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
//                                float distanceY) {
//            Log.d(DEBUG_TAG, "onScroll: " + e1.toString()+e2.toString());
//            return true;
//        }

//        @Override
//        public void onShowPress(MotionEvent event) {
//            Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
//        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
//            Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
//            toggle();
            return true;
        }
//
//        @Override
//        public boolean onDoubleTap(MotionEvent event) {
//            Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
//            return true;
//        }
//
//        @Override
//        public boolean onDoubleTapEvent(MotionEvent event) {
//            Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
//            return true;
//        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
//            Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
            return true;
        }
    }



    private void toggle(){
        if(toggleOn){
            springHandlerMove.setEndValue(0);
            toggleOn = false;
        }else {
            springHandlerMove.setEndValue(1);
            toggleOn = true;
        }
    }

    private void handlerScaleEffect(double value){

        if(toggleOn){
            handlerLayer.left = handlerLayer.right - handlerRadius*2 - handlerRadius*0.5f*(float)SpringUtil.clamp(value, 0 ,1);
        }else {
            handlerLayer.right = handlerLayer.left + handlerRadius*2 + handlerRadius*0.5f*(float)SpringUtil.clamp(value, 0 ,1);
        }

        this.postInvalidate();
    }

    private void handlerMoveEffect(double value){
//        if(toggleOn){
//            handlerLayer.left = basicLayer.right - handlerRadius*2 - borderWidth - handlerMoveDistance*value;
//            handlerLayer.right = basicLayer.right - borderWidth - handlerMoveDistance*value;
//        }else {
        //移动handler
            handlerLayer.left = basicLayer.left + borderWidth + handlerMoveDistance*(float)value;
            handlerLayer.right = basicLayer.left + handlerRadius*2 + borderWidth + handlerMoveDistance*(float)value;
//        }

        //下面这句话用来改变开启时颜色的透明度
        //SpringUtil.Clamp()的使用：value值小于0的时候取0，大于1的时候取1
        //SpringUtil.mapValueFromRangeToRange()value的变化本来是在范围0,1之间的，此方法可以得到其在范围0,255区间对应的值
        openBgPaint.setAlpha((int) SpringUtil.mapValueFromRangeToRange(SpringUtil.clamp(value, 0 ,1), 0, 1, 0, 255));

        postInvalidate();
    }

    private void animLayerScale(double value){
        double xOffset = SpringUtil.mapValueFromRangeToRange(value, 0 , 1, 0, handlerMoveDistance/2f + handlerRadius);
        double yOffset = SpringUtil.mapValueFromRangeToRange(value, 0 , 1, 0, handlerRadius);
        toggleAnimRadius = (float)yOffset;
        toggleAnimLayer.left = (float)getWidth()/2f - (float)xOffset;
        toggleAnimLayer.top = (float)getHeight()/2f - (float)yOffset;
        toggleAnimLayer.right = (float)getWidth()/2f + (float)xOffset;
        toggleAnimLayer.bottom = (float)getHeight()/2f + (float)yOffset;

    }

    private void animLayerScaleOut(double value){

    }


}
