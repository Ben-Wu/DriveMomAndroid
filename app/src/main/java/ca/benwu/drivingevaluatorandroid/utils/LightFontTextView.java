package ca.benwu.drivingevaluatorandroid.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import ca.benwu.drivingevaluatorandroid.R;

/**
 * Created by Ben Wu on 2016-10-08.
 */

public class LightFontTextView extends TextView {

    private Context mContext;

    public LightFontTextView(Context context) {
        super(context, null);
        mContext = context;
        init();
    }

    public LightFontTextView(Context context, AttributeSet attr) {
        super(context, attr, 0);
        mContext = context;
        init();
    }

    public LightFontTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        init();
    }

    private void init() {
        setTypeface(Typeface.createFromAsset(mContext.getAssets(), "fonts/Metropolis-Light.otf"));
        setTextColor(mContext.getResources().getColor(R.color.white));
    }
}
