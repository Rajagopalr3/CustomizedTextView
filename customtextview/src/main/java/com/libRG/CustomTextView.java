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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/*
 * Created by ${Raja} on 28-Jun-17.
 */

@SuppressLint("AppCompatCustomView")
public class CustomTextView extends TextView {

    private static final int TRIM_MODE_LINES = 0;
    private static final int TRIM_MODE_LENGTH = 1;
    private static final int DEFAULT_TRIM_LENGTH = 240;
    private static final int DEFAULT_TRIM_LINES = 2;
    private static final int INVALID_END_INDEX = -1;
    private boolean isReadMore = false;
    private boolean DEFAULT_SHOW_EXPANDED_TEXT = true;
    private static final String ELLIPSIZE = "... ";
    private CharSequence text;
    private BufferType bufferType;
    private boolean readMore = true;
    private int trimLength;
    private CharSequence trimCollapsedText;
    private CharSequence trimExpandedText;
    private ReadMoreClickableSpan viewMoreSpan;
    private int colorClickableText;
    private String fontName;
    private int trimMode;
    private int lineEndIndex;
    private int trimLines;
    private boolean isRounded = false;
    private int shape = 1;
    private float radius = 1;
    private float strokeWidth = 1;
    private String[] defValue = {"more", "less"};
    private int mBorderColor = Color.parseColor("#FF4081");
    private int mBackgroundColor = Color.TRANSPARENT;
    private List<OnExpandListener> onExpandListeners;
    private TimeInterpolator expandInterpolator;
    private TimeInterpolator collapseInterpolator;
    private long animationDuration = 300L;
    private boolean animating;
    private boolean expanded;
    private int collapsedHeight;

    public CustomTextView(Context context) {
        super(context);
        init(null);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    @SuppressLint("ResourceType")
    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, com.libRG.R.styleable.CustomTextView);
            isRounded = a.getBoolean(com.libRG.R.styleable.CustomTextView_lib_setRoundedView, isRounded);
            mBorderColor = a.getColor(com.libRG.R.styleable.CustomTextView_lib_setRoundedBorderColor, mBorderColor);

            //get padding values
            int padding = a.hasValue(0) ? a.getDimensionPixelSize(0, -1) : 0;
            int defaultPadding = 5;
            int paddingLeft = a.hasValue(1) ? a.getDimensionPixelSize(1, -1) : defaultPadding;
            int paddingTop = a.hasValue(2) ? a.getDimensionPixelSize(2, -1) : defaultPadding;
            int paddingRight = a.hasValue(3) ? a.getDimensionPixelSize(3, -1) : defaultPadding;
            int paddingBottom = a.hasValue(4) ? a.getDimensionPixelSize(4, -1) : defaultPadding;
            this.radius = a.getDimension(com.libRG.R.styleable.CustomTextView_lib_setRadius, radius);
            mBackgroundColor = a.getColor(com.libRG.R.styleable.CustomTextView_lib_setRoundedBGColor, mBackgroundColor);
            strokeWidth = a.getDimension(com.libRG.R.styleable.CustomTextView_lib_setStrokeWidth, strokeWidth);
            shape = a.getInt(com.libRG.R.styleable.CustomTextView_lib_setShape, shape);
            isReadMore = a.getBoolean(com.libRG.R.styleable.CustomTextView_lib_setExpandableText, isReadMore);
            DEFAULT_SHOW_EXPANDED_TEXT = a.getBoolean(com.libRG.R.styleable.CustomTextView_lib_setActionTextVisible, DEFAULT_SHOW_EXPANDED_TEXT);
            fontName = a.getString(com.libRG.R.styleable.CustomTextView_lib_setFont);
            trimLength = a.getInt(com.libRG.R.styleable.CustomTextView_lib_setTrimLength, DEFAULT_TRIM_LENGTH);
            trimCollapsedText = defValue[0];
            trimExpandedText = defValue[1];
            trimLines = a.getInt(com.libRG.R.styleable.CustomTextView_lib_setTrimLines, DEFAULT_TRIM_LINES);
            colorClickableText = a.getColor(com.libRG.R.styleable.CustomTextView_lib_setActionTextColor, Color.parseColor("#FF4081"));
            trimMode = a.getInt(com.libRG.R.styleable.CustomTextView_lib_setTrimMode, TRIM_MODE_LINES);

            // create bucket of OnExpandListener instances
            onExpandListeners = new ArrayList<>();
            // create default interpolators
            expandInterpolator = new AccelerateDecelerateInterpolator();
            collapseInterpolator = new AccelerateDecelerateInterpolator();

            //check rounded or not
            if (isRounded) {
                if (padding != 0) {
                    this.setPadding(padding, padding, padding, padding);
                } else {
                    this.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                }
                setBackgroundLayout(getShapeBackground(mBorderColor));
            }
            if (isReadMore) {
                viewMoreSpan = new ReadMoreClickableSpan();
                onGlobalLayoutLineEndIndex();
            }
            setFont();
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // if this TextView is collapsed and maxLines = 0,
        // than make its height equals to zero
        if (this.trimLines == 0 && !this.expanded && !this.animating) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    private void setText() {
        super.setText(getDisplayableText(), bufferType);
        setMovementMethod(LinkMovementMethod.getInstance());
        setHighlightColor(Color.TRANSPARENT);
    }

    private CharSequence getDisplayableText() {
        return getTrimmedText(text);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        this.text = text;
        bufferType = type;
        setText();
    }

    private CharSequence getTrimmedText(CharSequence text) {

        if (trimMode == TRIM_MODE_LENGTH) {
            if (text != null && text.length() > trimLength) {
                if (readMore) {
                    return updateCollapsedText();
                } else {
                    return updateExpandedText();
                }
            }
        } else if (trimMode == TRIM_MODE_LINES) {
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
        int trimEndIndex = text.length();
        switch (trimMode) {
            case TRIM_MODE_LINES:
                trimEndIndex = lineEndIndex - (ELLIPSIZE.length() + trimCollapsedText.length() + 1);
                if (trimEndIndex < 0) {
                    trimEndIndex = trimLength + 1;
                }
                break;
            case TRIM_MODE_LENGTH:
                trimEndIndex = trimLength + 1;
                break;
        }
        SpannableStringBuilder s = new SpannableStringBuilder(text, 0, trimEndIndex)
                .append(ELLIPSIZE)
                .append(trimCollapsedText);
        return addClickableSpan(s, trimCollapsedText);
    }

    private CharSequence updateExpandedText() {
        if (DEFAULT_SHOW_EXPANDED_TEXT) {
            SpannableStringBuilder s = new SpannableStringBuilder(text, 0, text.length()).append(" " + trimExpandedText);
            return addClickableSpan(s, trimExpandedText);
        }
        return text;
    }

    private CharSequence addClickableSpan(SpannableStringBuilder s, CharSequence trimText) {
        s.setSpan(viewMoreSpan, s.length() - trimText.length(), s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    public void setTrimLength(int trimLength) {
        this.trimLength = trimLength;
        setText();
    }

    public void setTrimMode(int trimMode) {
        this.trimMode = trimMode;
    }

    public void setTrimLines(int trimLines) {
        this.trimLines = trimLines;
    }


    void setBackgroundLayout(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.setBackground(drawable);
        } else {
            this.setBackgroundDrawable(drawable);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @SuppressLint("WrongConstant")
    private Drawable getShapeBackground(@ColorInt int color) {
        int radius = 0;
        if (this.shape == GradientDrawable.OVAL) {
            radius = ((this.getHeight() > this.getWidth()) ? this.getHeight() : this.getWidth()) / 2;
        } else if (this.shape == GradientDrawable.RECTANGLE) {
            radius = (int) this.radius;
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(this.shape);
        shape.setCornerRadius(radius);
        shape.setColor(mBackgroundColor);
        shape.setStroke((int) strokeWidth, color);
        return shape;
    }


    public void setFont() {
        Typeface typeface;
        if (fontName != null) {
            try {
                typeface = Typeface.createFromAsset(getContext().getAssets(), fontName);
                setTypeface(typeface);
            } catch (Exception ignored) {
            }
        } else {
            setTypeface(Typeface.DEFAULT);
        }
    }

    private class ReadMoreClickableSpan extends ClickableSpan {
        @Override
        public void onClick(View widget) {
            readMore = !readMore;
            toggle();
            setText();
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(colorClickableText);
        }
    }

    private void onGlobalLayoutLineEndIndex() {
        if (trimMode == TRIM_MODE_LINES) {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ViewTreeObserver obs = getViewTreeObserver();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        obs.removeOnGlobalLayoutListener(this);
                    } else {
                        obs.removeGlobalOnLayoutListener(this);
                    }
                    refreshLineEndIndex();
                    setText();
                }
            });
        }
    }

    private void refreshLineEndIndex() {
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
    }


    /**
     * Toggle the expanded state of this {@link CustomTextView}.
     *
     * @return true if toggled, false otherwise.
     */
    public boolean toggle() {
        return this.expanded ? this.collapse() : this.expand();
    }

    /**
     * Expand this {@link CustomTextView}.
     *
     * @return true if expanded, false otherwise.
     */
    public boolean expand() {
        if (!this.expanded && !this.animating && this.trimLines >= 0) {
            // notify listener
            this.notifyOnExpand();

            // measure collapsed height
            this.measure(MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            this.collapsedHeight = this.getMeasuredHeight();

            // indicate that we are now animating
            this.animating = true;

            // set maxLines to MAX Integer, so we can calculate the expanded height
            this.setMaxLines(Integer.MAX_VALUE);

            // measure expanded height
            this.measure(MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            final int expandedHeight = this.getMeasuredHeight();

            // animate from collapsed height to expanded height
            final ValueAnimator valueAnimator = ValueAnimator.ofInt(this.collapsedHeight, expandedHeight);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(final ValueAnimator animation) {
                    CustomTextView.this.setHeight((int) animation.getAnimatedValue());
                }
            });

            // wait for the animation to end
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    // reset min & max height (previously set with setHeight() method)
                    CustomTextView.this.setMaxHeight(Integer.MAX_VALUE);
                    CustomTextView.this.setMinHeight(0);

                    // if fully expanded, set height to WRAP_CONTENT, because when rotating the device
                    // the height calculated with this ValueAnimator isn't correct anymore
                    final ViewGroup.LayoutParams layoutParams = CustomTextView.this.getLayoutParams();
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    CustomTextView.this.setLayoutParams(layoutParams);

                    // keep track of current status
                    CustomTextView.this.expanded = true;
                    CustomTextView.this.animating = false;
                }
            });

            // set interpolator
            valueAnimator.setInterpolator(this.expandInterpolator);

            // start the animation
            valueAnimator
                    .setDuration(this.animationDuration)
                    .start();

            return true;
        }

        return false;
    }

    /**
     * Collapse this {@link TextView}.
     *
     * @return true if collapsed, false otherwise.
     */
    public boolean collapse() {
        if (this.expanded && !this.animating && this.trimLines >= 0) {
            // notify listener
            this.notifyOnCollapse();

            // measure expanded height
            final int expandedHeight = this.getMeasuredHeight();

            // indicate that we are now animating
            this.animating = true;

            // animate from expanded height to collapsed height
            final ValueAnimator valueAnimator = ValueAnimator.ofInt(expandedHeight, this.collapsedHeight);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(final ValueAnimator animation) {
                    CustomTextView.this.setHeight((int) animation.getAnimatedValue());
                }
            });

            // wait for the animation to end
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    // keep track of current status
                    CustomTextView.this.expanded = false;
                    CustomTextView.this.animating = false;

                    // set maxLines back to original value
                    CustomTextView.this.setMaxLines(CustomTextView.this.trimLines);

                    // if fully collapsed, set height back to WRAP_CONTENT, because when rotating the device
                    // the height previously calculated with this ValueAnimator isn't correct anymore
                    final ViewGroup.LayoutParams layoutParams = CustomTextView.this.getLayoutParams();
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    CustomTextView.this.setLayoutParams(layoutParams);
                }
            });

            // set interpolator
            valueAnimator.setInterpolator(this.collapseInterpolator);

            // start the animation
            valueAnimator
                    .setDuration(this.animationDuration)
                    .start();

            return true;
        }

        return false;
    }

    public void setAnimationDuration(final long animationDuration) {
        this.animationDuration = animationDuration;
    }

    public void addOnExpandListener(final OnExpandListener onExpandListener) {
        this.onExpandListeners.add(onExpandListener);
    }

    public void removeOnExpandListener(final OnExpandListener onExpandListener) {
        this.onExpandListeners.remove(onExpandListener);
    }

    public void setInterpolator(final TimeInterpolator interpolator) {
        this.expandInterpolator = interpolator;
        this.collapseInterpolator = interpolator;
    }

    public void setExpandInterpolator(final TimeInterpolator expandInterpolator) {
        this.expandInterpolator = expandInterpolator;
    }

    public TimeInterpolator getExpandInterpolator() {
        return this.expandInterpolator;
    }

    public void setCollapseInterpolator(final TimeInterpolator collapseInterpolator) {
        this.collapseInterpolator = collapseInterpolator;
    }

    public TimeInterpolator getCollapseInterpolator() {
        return this.collapseInterpolator;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    private void notifyOnCollapse() {
        for (final OnExpandListener onExpandListener : this.onExpandListeners) {
            onExpandListener.onCollapse(this);
        }
    }

    private void notifyOnExpand() {
        for (final OnExpandListener onExpandListener : this.onExpandListeners) {
            onExpandListener.onExpand(this);
        }
    }

    public interface OnExpandListener {
        void onExpand(@NonNull CustomTextView view);

        void onCollapse(@NonNull CustomTextView view);
    }

}
