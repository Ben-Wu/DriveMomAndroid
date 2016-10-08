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

public class MediumFontTextView extends TextView {

    private Context mContext;

    public MediumFontTextView(Context context) {
        super(context, null);
        mContext = context;
        init();
    }

    public MediumFontTextView(Context context, AttributeSet attr) {
        super(context, attr, 0);
        mContext = context;
        init();
    }

    public MediumFontTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        init();
    }

    private void init() {
        setTypeface(Typeface.createFromAsset(mContext.getAssets(), "fonts/Metropolis-Medium.otf"));
        setTextColor(mContext.getResources().getColor(R.color.white));
    }
}
