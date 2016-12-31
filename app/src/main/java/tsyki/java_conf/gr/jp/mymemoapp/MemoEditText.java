package tsyki.java_conf.gr.jp.mymemoapp;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.EditText;

public class MemoEditText extends EditText {
    private static final int SOLID = 1;
    private static final int DASH = 2;
    private static final int NORMAL = 4;
    private static final int BOLD= 8;

    /** Viewの横幅 */
    private int measuredWidth;
    /** 1行の高さ*/
    private int lineHeight;
    /** 画面上に表示可能な行数 */
    private int displayLineCount;

    // NOTE ローカル変数でもできるがonDrawの度に生成するのはコストが高いためフィールドにする
    private Path path;
    private Paint paint;

    public   MemoEditText(Context context){
        this(context,null);
    }

    public MemoEditText(Context context, AttributeSet attrs){
        super(context,attrs);
        init(context, attrs);
    }

    public MemoEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        path = new Path();
        paint = new Paint();

        //輪郭線を描画
        paint.setStyle( Paint.Style.STROKE);
        if(attrs != null && !isInEditMode()){
            int lineEffectBit;
            int lineColor;

            Resources resources = context.getResources();
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MemoEditText);
            try{
                lineEffectBit = typedArray.getInteger(R.styleable.MemoEditText_lineEffect, SOLID);
                lineColor = typedArray.getColor(R.styleable.MemoEditText_lineColor, Color.GRAY);
            }finally {
                typedArray.recycle();
            }

            // 罫線のエフェクトを設定
            if((lineEffectBit & DASH) == DASH){
                DashPathEffect effect = new DashPathEffect(new float[]{
                    resources.getDimension(R.dimen.text_rule_interval_on),
                    resources.getDimension(R.dimen.text_rule_interval_off),
                },0f);
                paint.setPathEffect(effect);
            }

            float strokeWidth;
            if((lineEffectBit & BOLD) == BOLD){
                strokeWidth = resources.getDimension(R.dimen.text_rule_width_bold);
            }
            else{
                strokeWidth = resources.getDimension(R.dimen.text_rule_width_normal);
            }
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(lineColor);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.measuredWidth = getMeasuredWidth();
        this.displayLineCount = getMeasuredHeight() / getLineHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int paddingTop = getExtendedPaddingTop();
        int scrollY = getScrollY();
        int firstVisibleLine = getLayout().getLineForVertical(scrollY);
        int lastVisibleLine = firstVisibleLine + this.displayLineCount;

        this.path.reset();
        for(int i =firstVisibleLine;i <= lastVisibleLine ; i++){
            this.path.moveTo(0, 1*lineHeight+paddingTop);
            this.path.lineTo(this.measuredWidth, 1*lineHeight + paddingTop);
        }
        canvas.drawPath(path,paint);

        super.onDraw(canvas);
    }
}
