package com.libRG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Checkable;
import android.widget.TextView;


/*
 * Created by ${Raja} on 28-Jun-17.
 */

public class CustomTextView extends android.support.v7.widget.AppCompatTextView implements Checkable {

    private static final int COLOR_ACCENT_INDEX = 2;
    private static final int INVALID_END_INDEX = -1;
    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private static final int LEFT = 0, RIGHT = 1;
    private int actionTextColor, mBackgroundColor, mBorderColor, checkMarkDrawableTint;
    private int padding, paddingLeft, paddingTop, paddingRight, paddingBottom;
    private int lineEndIndex, trimLines, shape;
    private int collapsedHeight, checkBoxPosition, expandedHeight = 0;
    private int checkBoxDrawablePadding, baseCheckBoxPadding, checkBoxWidth;
    private long animationDuration = 300L;
    private boolean isExpandableText, expanded, needTint, mChecked, isStrikeText;
    private boolean readMore = true, isClick = false;
    private boolean DEFAULT_SHOW_EXPANDED_TEXT, isUnderLine, isBorderView, isCheckedText;
    private float radius;
    private float strokeWidth;
    private String fontName;
    private String[] defValue = {"more", "less"};
    private CharSequence text;
    private Drawable checkBoxDrawable, checked, unChecked;
    private TimeInterpolator expandInterpolator, collapseInterpolator;
    private BufferType bufferType;
    private ReadMoreClickableSpan viewMoreSpan;
    private OnCheckedChangeListener listener;


    public CustomTextView(Context context) {
        this(context, null);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }


    @SuppressLint("ResourceType")
    private void init(AttributeSet attrs) {

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CustomTextView);
        isBorderView = a.getBoolean(R.styleable.CustomTextView_lib_setRoundedView, false);
        mBorderColor = a.getColor(R.styleable.CustomTextView_lib_setRoundedBorderColor, Color.parseColor("#B6B6B6"));
        padding = a.getDimensionPixelSize(R.styleable.CustomTextView_android_padding, -1);
        paddingLeft = a.getDimensionPixelSize(R.styleable.CustomTextView_android_paddingLeft, 5);
        paddingTop = a.getDimensionPixelSize(R.styleable.CustomTextView_android_paddingTop, 5);
        paddingRight = a.getDimensionPixelSize(R.styleable.CustomTextView_android_paddingRight, 5);
        paddingBottom = a.getDimensionPixelSize(R.styleable.CustomTextView_android_paddingBottom, 5);
        isClick = a.hasValue(R.styleable.CustomTextView_android_onClick);
        radius = a.getDimension(R.styleable.CustomTextView_lib_setRadius, 1);
        mBackgroundColor = a.getColor(R.styleable.CustomTextView_lib_setRoundedBGColor, Color.TRANSPARENT);
        strokeWidth = a.getDimension(R.styleable.CustomTextView_lib_setStrokeWidth, 1);
        shape = a.getInt(R.styleable.CustomTextView_lib_setShape, 0);
        isExpandableText = a.getBoolean(R.styleable.CustomTextView_lib_setExpandableText, false);
        isUnderLine = a.getBoolean(R.styleable.CustomTextView_lib_setUnderLineText, false);
        isStrikeText = a.getBoolean(R.styleable.CustomTextView_lib_setStrikeText, false);
        DEFAULT_SHOW_EXPANDED_TEXT = a.getBoolean(R.styleable.CustomTextView_lib_setActionTextVisible, true);
        fontName = a.getString(R.styleable.CustomTextView_lib_setFont);
        if (a.hasValue(R.styleable.CustomTextView_lib_setExpandHint))
            defValue[0] = a.getString(R.styleable.CustomTextView_lib_setExpandHint);
        if (a.hasValue(R.styleable.CustomTextView_lib_setCollapseHint))
            defValue[1] = a.getString(R.styleable.CustomTextView_lib_setCollapseHint);
        trimLines = a.getInt(R.styleable.CustomTextView_lib_setTrimLines, 2);
        actionTextColor = a.getColor(R.styleable.CustomTextView_lib_setActionTextColor, 0);
        expandInterpolator = new AccelerateDecelerateInterpolator();
        collapseInterpolator = new AccelerateDecelerateInterpolator();
        isCheckedText = a.getBoolean(R.styleable.CustomTextView_lib_setCheckedText, false);
        mChecked = a.getBoolean(R.styleable.CustomTextView_lib_setChecked, false);
        checkBoxPosition = a.getInteger(R.styleable.CustomTextView_lib_checkedIconPosition, LEFT);
        checkBoxDrawablePadding = a.getDimensionPixelSize(R.styleable.CustomTextView_lib_checkedDrawablePadding, 5);
        checked = a.getDrawable(R.styleable.CustomTextView_lib_checkedDrawable);
        unChecked = a.getDrawable(R.styleable.CustomTextView_lib_unCheckedDrawable);
        checkMarkDrawableTint = a.getColor(R.styleable.CustomTextView_lib_checkMarkTint, 0);
        needTint = checked != null && unChecked != null;
        checked = checked == null ? getResources().getDrawable(android.R.drawable.checkbox_on_background) : checked;
        unChecked = unChecked == null ? getResources().getDrawable(android.R.drawable.checkbox_off_background) : unChecked;
        refresh();
        a.recycle();
    }


    private void setText() {
        super.setText(getTrimmedText(text), bufferType);
        setMovementMethod(LinkMovementMethod.getInstance());
        setHighlightColor(Color.TRANSPARENT);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        this.text = text;
        bufferType = type;
        setText();
    }

    private CharSequence getTrimmedText(CharSequence text) {
        if (isExpandableText) {
            if (text != null && lineEndIndex > 0) {
                if (readMore) {
                    return updateCollapsedText();
                } else {
                    return updateExpandedText();
                }
            }
        }
        return text;
    }

    private CharSequence updateCollapsedText() {
        String ELLIPSIZE = "...";
        int trimEndIndex = lineEndIndex - (ELLIPSIZE.length() + defValue[0].length() + 1);
        SpannableStringBuilder s = new SpannableStringBuilder(text, 0, trimEndIndex)
                .append(ELLIPSIZE)
                .append(defValue[0]);
        return addClickableSpan(s, defValue[0]);
    }

    private CharSequence updateExpandedText() {
        if (DEFAULT_SHOW_EXPANDED_TEXT) {
            SpannableStringBuilder s = new SpannableStringBuilder(text, 0, text.length()).append(" ").append(String.valueOf(defValue[1]));
            return addClickableSpan(s, defValue[1]);
        }
        return text;
    }

    private CharSequence addClickableSpan(SpannableStringBuilder s, CharSequence trimText) {
        s.setSpan(viewMoreSpan, s.length() - trimText.length(), s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private void setBackgroundLayout(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.setBackground(drawable);
        } else {
            this.setBackgroundDrawable(drawable);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isCheckedText) {
            final Drawable checkMarkDrawable = mChecked ? checked : unChecked;
            if (checkMarkDrawable != null) {
                final int height = checkMarkDrawable.getIntrinsicHeight();
                int y = (getHeight() - height) / 2;
                switch (checkBoxPosition) {
                    case LEFT:
                        checkMarkDrawable.setBounds(baseCheckBoxPadding, y, baseCheckBoxPadding
                                + checkBoxWidth, y + height);
                        checkMarkDrawable.draw(canvas);
                        break;
                    case RIGHT:
                        int right = getWidth();
                        checkMarkDrawable.setBounds(right - checkBoxWidth - baseCheckBoxPadding,
                                y, right - baseCheckBoxPadding, y + height);
                        checkMarkDrawable.draw(canvas);
                        break;
                }
            }
        }
    }

    private void refresh() {
        //check checkedText is active or not... if active change view as checkedText
        if (isCheckedText) {
            if (checkMarkDrawableTint != 0 && needTint) {
                checked.setColorFilter(new PorterDuffColorFilter(checkMarkDrawableTint, PorterDuff.Mode.SRC_IN));
                unChecked.setColorFilter(new PorterDuffColorFilter(checkMarkDrawableTint, PorterDuff.Mode.SRC_IN));
            }
            setGravity(Gravity.CENTER_VERTICAL);
            setCheckMarkDrawable(mChecked ? checked : unChecked);
            setChecked(mChecked);
        }
        //check borderView is active or not... if active set border view
        if (isBorderView) {
            if (padding != -1) {
                setPadding(padding, padding, padding, padding);
            } else {
                setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            }
            setBackgroundLayout(getShapeBackground(mBorderColor));
        }
        //check ExpandableText is active or not... if active set view in expandable mode
        if (isExpandableText) {
            setMaxLines(trimLines);
            viewMoreSpan = new ReadMoreClickableSpan();
            onGlobalLayoutLineEndIndex();
        }
        setUnderLineText(isUnderLine);
        setStrikeText(isStrikeText);
        setFont();
    }


    @SuppressLint("WrongConstant")
    private Drawable getShapeBackground(@ColorInt int color) {
        int radius;
        if (this.shape == GradientDrawable.OVAL) {
            radius = ((this.getHeight() > this.getWidth()) ? this.getHeight() : this.getWidth()) / 2;
        } else {
            radius = (int) this.radius;
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(this.shape);
        shape.setCornerRadius(radius);
        shape.setColor(mBackgroundColor);
        shape.setStroke((int) strokeWidth, color);
        return shape;
    }

    private void setFont() {
        if (fontName != null) {
            try {
                setTypeface(Typefaces.get(getContext(), fontName));
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            int actionX, actionY;
            invalidate();
            if (isClick || isExpandableText)
                return super.onTouchEvent(event);

            if (isEnabled()) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    actionX = (int) event.getX();
                    actionY = (int) event.getY();
                    if (isCheckedText) {
                        if (checkBoxDrawable != null && checkBoxDrawable.getBounds().contains(actionX, actionY)) {
                            if (listener != null)
                                listener.onCheckedChanged(!isChecked());
                            setChecked(!isChecked());
                            return super.onTouchEvent(event);
                        }
                    } else {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        try {
            isClick = true;
            if (isCheckedText) {
                setChecked(!isChecked());
                if (listener != null)
                    listener.onCheckedChanged(isChecked());
            }
            super.setOnClickListener(l);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.trimLines == 0 && !this.expanded)
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    private class ReadMoreClickableSpan extends ClickableSpan {
        @Override
        public void onClick(View widget) {
            CustomTextView.this.setClickable(false);
            CustomTextView.this.setFocusable(false);
            CustomTextView.this.setFocusableInTouchMode(false);
            readMore = !readMore;
            toggleView();
            setText();
            enableClick();
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(actionTextColor != 0 ? actionTextColor : getThemeColor(COLOR_ACCENT_INDEX));
        }
    }

    private void enableClick() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CustomTextView.this.setClickable(true);
                CustomTextView.this.setFocusable(true);
                CustomTextView.this.setFocusableInTouchMode(true);
            }
        }, 100L);
    }

    private void onGlobalLayoutLineEndIndex() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver obs = getViewTreeObserver();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }
                try {
                    if (trimLines == 0) {
                        lineEndIndex = getLayout().getLineEnd(0);
                    } else if (trimLines > 0 && getLineCount() >= trimLines) {
                        lineEndIndex = getLayout().getLineEnd(trimLines - 1);
                    } else {
                        lineEndIndex = INVALID_END_INDEX;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setText();
            }
        });
        invalidate();
    }


    private void toggleView() {
        if (this.trimLines >= 0) {
            if (expanded) {
                expandedHeight = this.getMeasuredHeight();
                animateView(false);
            } else {
                this.measure(MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                this.collapsedHeight = this.getMeasuredHeight();
                this.setMaxLines(Integer.MAX_VALUE);
                if (this.expandedHeight == 0) {
                    this.expandedHeight = getMeasuredHeightOfTextView();
                }
                animateView(true);
            }
        }
    }

    private void animateView(final boolean expanded) {
        ValueAnimator valueAnimator;
        if (expanded) {
            valueAnimator = ValueAnimator.ofInt(this.collapsedHeight, expandedHeight);
        } else {
            valueAnimator = ValueAnimator.ofInt(this.expandedHeight, this.collapsedHeight);
        }

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                CustomTextView.this.setHeight((int) animation.getAnimatedValue());
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                if (expanded) {
                    CustomTextView.this.setMaxHeight(Integer.MAX_VALUE);
                    final ViewGroup.LayoutParams layoutParams = CustomTextView.this.getLayoutParams();
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    CustomTextView.this.setLayoutParams(layoutParams);
                    CustomTextView.this.expanded = true;
                } else {
                    final ViewGroup.LayoutParams layoutParams = CustomTextView.this.getLayoutParams();
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    CustomTextView.this.setLayoutParams(layoutParams);
                    CustomTextView.this.expanded = false;
                }
            }
        });
        valueAnimator.setInterpolator(expanded ? this.expandInterpolator : this.collapseInterpolator);
        valueAnimator.setDuration(this.animationDuration).start();
    }

    private int getMeasuredHeightOfTextView() {
        TextView textView = new TextView(this.getContext());
        textView.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        textView.setTypeface(getTypeface());
        textView.setText(text, BufferType.SPANNABLE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) getTextSize());
        textView.measure(MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return textView.getMeasuredHeight();
    }

    private int getThemeColor(int index) {
        int[] attribute = new int[]{R.attr.colorPrimary, R.attr.colorPrimaryDark, R.attr.colorAccent};
        TypedArray array = getContext().getTheme().obtainStyledAttributes(attribute);
        int color = array.getColor(index, Color.parseColor("#FF4081"));
        array.recycle();
        return color;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener l) {
        this.listener = l;
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean isChecked);
    }

    public void toggle() {
        if (isCheckedText)
            setChecked(!mChecked);
    }

    public boolean isChecked() {
        return mChecked;
    }

    /**
     * <p>Changes the checked state of this text view.</p>
     *
     * @param checked true to check the text, false to uncheck it
     */
    public void setChecked(boolean checked) {
        if (isCheckedText)
            if (mChecked != checked) {
                mChecked = checked;
                invalidate();
            }
    }

    /**
     * Set the checkmark to a given Drawable. This will be drawn when {@link #isChecked()} is true.
     *
     * @param d The Drawable to use for the checkmark.
     */
    private void setCheckMarkDrawable(Drawable d) {
        if (isCheckedText) {
            if (checkBoxDrawable != null) {
                checkBoxDrawable.setCallback(null);
                unscheduleDrawable(checkBoxDrawable);
            }
            if (d != null) {
                d.setCallback(this);
                d.setVisible(getVisibility() == VISIBLE, false);
                d.setState(CHECKED_STATE_SET);
                setMinHeight(d.getIntrinsicHeight());
                checkBoxWidth = d.getIntrinsicWidth();
                checkBoxDrawable = d;
                if (!isBorderView)
                    super.setPadding(paddingLeft + checkBoxWidth + checkBoxDrawablePadding, paddingTop, paddingRight, paddingBottom);
                d.setState(getDrawableState());
            }
            requestLayout();
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (checkBoxDrawable != null) {
            int[] myDrawableState = getDrawableState();
            checkBoxDrawable.setState(myDrawableState);
            invalidate();
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        boolean populated = super.dispatchPopulateAccessibilityEvent(event);
        if (!populated) {
            event.setChecked(mChecked);
        }
        return populated;
    }

    /**
     * Set the checkMark to a given Drawable, identified by its resourece id. This will be drawn
     * when it is true.
     *
     * @param resId The Drawable to use for the checkmark.
     */
    public void setCheckedDrawable(int resId) {
        if (resId != 0 && !isCheckedText) {
            return;
        }
        Drawable d = getResources().getDrawable(resId);
        checked = d;
        setCheckMarkDrawable(d);
    }

    /**
     * Set the unCheckMark to a given Drawable, identified by its resourece id. This will be drawn
     * when it is true.
     *
     * @param resId The Drawable to use for the checkmark.
     */
    public void setUnCheckedDrawable(int resId) {
        if (resId != 0 && !isCheckedText) {
            return;
        }
        Drawable d = getResources().getDrawable(resId);
        unChecked = d;
        setCheckMarkDrawable(d);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (checkBoxDrawable != null && isCheckedText) {
            switch (checkBoxPosition) {
                case LEFT:
                    baseCheckBoxPadding = left;
                    super.setPadding(left + checkBoxWidth + checkBoxDrawablePadding, top, right, bottom);
                    break;
                case RIGHT:
                    baseCheckBoxPadding = right;
                    super.setPadding(left, top, right + checkBoxWidth + checkBoxDrawablePadding, bottom);
                    break;
            }
        } else {
            super.setPadding(left, top, right, bottom);
        }
    }

    public void setUnderLineText(boolean isUnderLine) {
        this.setPaintFlags(isUnderLine ? this.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG : this.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
    }

    public void setStrikeText(boolean isStrikeText) {
        this.setPaintFlags(isStrikeText ? this.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : this.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
        refresh();
    }

    public void setTrimLines(int trimLines) {
        this.trimLines = trimLines;
        refresh();
    }

    public void setShape(int shape) {
        this.shape = shape;
        refresh();
    }

    public void setActionTextColor(int actionTextColor) {
        this.actionTextColor = actionTextColor;
        refresh();
    }

    public void setExpandableText(boolean isExpandable) {
        isExpandableText = isExpandable;
        refresh();

    }

    public void setBorderView(boolean isBorderNeed) {
        isBorderView = isBorderNeed;
        refresh();
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        refresh();
    }

    public void setBackgroundColor(int mBackgroundColor) {
        this.mBackgroundColor = mBackgroundColor;
        refresh();
    }

    public void setBorderColor(int mBorderColor) {
        this.mBorderColor = mBorderColor;
        refresh();
    }

    public void setRadius(float radius) {
        this.radius = radius;
        refresh();
    }


    public void setCheckedText(boolean checkedText) {
        isCheckedText = checkedText;
        refresh();
    }


    public void setAnimationDuration(final long animationDuration) {
        this.animationDuration = animationDuration;
    }

    public void setInterpolator(final TimeInterpolator interpolator) {
        this.expandInterpolator = interpolator;
        this.collapseInterpolator = interpolator;
    }

}