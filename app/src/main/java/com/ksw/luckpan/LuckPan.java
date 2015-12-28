package com.ksw.luckpan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * SurfaceView的通用使用步骤
 * Created by Windows User on 2015/12/27.
 */
public class LuckPan extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private Thread t;//用于绘制的线程
    private boolean isRunning;//线程控制开关

    //奖项的名称
    private String[] mStrs = new String[]{"单反相机", "Ipad",
            "恭喜发财", "Iphone", "服装一套", "恭喜发财"};
    //奖项的图片
    private int[] mImgs = new int[]{R.drawable.danfan, R.drawable.ipad, R.drawable.f040,
            R.drawable.iphone, R.drawable.meizi, R.drawable.f040};
    //盘块的背景颜色
    private int[] mColor = new int[]{0xFFFFC300, 0xFFF17E01, 0xFFFFC300, 0xFFF17E01
            , 0xFFFFC300, 0xFFF17E01};
    //与图片对应的bitmap数组
    private Bitmap[] mImagesBitmap;

    private int mItemCount = 6;//盘块数量
    //整个盘快的直径
    private int mRadius;
    //整个盘快的范围
    private RectF mRange = new RectF();
    //绘制盘快的画笔
    private Paint mArcPaint;
    //绘制文字的画笔
    private Paint mTextPaint;
    private double mSpeed ;//转盘转动速度
    private volatile int mStartAngle = 0;//volatile可保证线程间的可见性
    private boolean isShouldEnd;//是否点击可停止按钮
    private int mCenter;//转盘的中心位置
    private int mPandding;//以paddlingLeft为准
    private Bitmap mBgBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg2);
    private float mTextSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20,
                    getResources().getDisplayMetrics());


    public LuckPan(Context context) {
        this(context, null);
    }

    public LuckPan(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
        //可获得焦点
        setFocusable(true);
        setFocusable(true);
        //设置常亮
        setKeepScreenOn(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = Math.min(getMeasuredWidth(),getMeasuredHeight());
        mPandding = getPaddingLeft();
        //直径
        mRadius = width - mPandding * 2;
        //中心点
        mCenter = width/2;
        setMeasuredDimension(width, width);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //初始化绘制盘块的画笔
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setDither(true);
        //初始化绘制文本的画笔
        mTextPaint = new Paint();
        mTextPaint.setColor(0xffffffff);
        mTextPaint.setTextSize(mTextSize);
        //初始化盘快范围
        mRange = new RectF(mPandding,mPandding,mPandding+mRadius,mPandding+mRadius);

        //初始化图片
        mImagesBitmap = new Bitmap[mItemCount];
        for(int i = 0;i < mItemCount ; i++){
            mImagesBitmap[i] = BitmapFactory.decodeResource(getResources(),mImgs[i]);
        }
        isRunning = true;
        t = new Thread(this);
        t.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        isRunning = false;
    }

    @Override
    public void run() {
        while (isRunning) {
            long start = System.currentTimeMillis();
            draw();
            long end = System.currentTimeMillis();
            if(end - start<50){
                try {
                    Thread.sleep(50 - (end - start));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void draw() {
        try {
            mCanvas = mHolder.lockCanvas();
            if (mCanvas != null) {
                //draw something
                //绘制背景
                drawBg();
                //绘制盘块
                float tmpAngle = mStartAngle;
                float sweepAngle = 360/mItemCount;
                for(int i = 0; i < mItemCount ; i++){
                    mArcPaint.setColor(mColor[i]);
                    //绘制盘快
                    mCanvas.drawArc(mRange,tmpAngle,sweepAngle,true,mArcPaint);
                    //绘制文本
                    drawText(tmpAngle,sweepAngle,mStrs[i]);
                    //绘制图标
                    drawIcon(tmpAngle,mImagesBitmap[i]);
                    tmpAngle += sweepAngle;
                }
                mStartAngle += mSpeed;
                //如果点击了停止按钮
                if(isShouldEnd){
                    mSpeed -= 1;
                }
                if(mSpeed <= 0){
                    mSpeed = 0;
                    isShouldEnd = false;
                }
            }
        } catch (Exception e) {
        } finally {
            if (mCanvas != null) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }

    }
    //点击启动按钮
    public void luckStart(){
        mSpeed = 50;
        isShouldEnd = false;
    }
    //点击停止按钮
    public void luckEnd(){
//        mSpeed = 50;

        isShouldEnd = true;

    }
    //转盘是否还在旋转
    public boolean isStart(){
        return mSpeed != 0;
    }

    public boolean isShouldEnd(){
        return isShouldEnd;
    }
    private void drawIcon(float tmpAngle, Bitmap bitmap) {
        //设置图片的宽度为半径的1/2
        int imgWidth = mRadius/8;
        float angle = (float) ((tmpAngle + 360/mItemCount/2)*Math.PI/180);
        int x = (int) (mCenter+mRadius/2/2*Math.cos(angle));
        int y = (int) (mCenter+mRadius/2/2*Math.sin(angle));
        //确定图片位置
        Rect rect = new Rect(x-imgWidth/2,y-imgWidth/2,x+imgWidth/2,y+imgWidth/2);
        mCanvas.drawBitmap(bitmap,null,rect,null);
    }


    //绘制没饿过盘快的文本
    private void drawText(float tmpAngle, float sweepAngle, String mStr) {
        Path path = new Path();
        path.addArc(mRange,tmpAngle,sweepAngle);
       //利用水平偏移量让文字居中
        float textWidth = mTextPaint.measureText(mStr);
        int hOffset = (int) (mRadius * Math.PI/mItemCount/2 - textWidth/2);//水平偏移量
        int wOffset = mRadius/2/6;//垂直偏移量
        mCanvas.drawTextOnPath(mStr,path,hOffset,wOffset,mTextPaint);
    }

    //绘制转盘
    private void drawBg() {
        mCanvas.drawColor(0xffffffff);
        mCanvas.drawBitmap(mBgBitmap,null,new Rect(mPandding/2,mPandding/2,
                getMeasuredWidth()-mPandding/2,getMeasuredHeight()- mPandding/2),null);
    }
}
