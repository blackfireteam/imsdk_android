package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji.widget.EmojiTextView;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.idonans.core.util.DimenUtil;
import com.idonans.lang.util.ViewUtil;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleWidgetCustomSoftKeyboardBinding;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleWidgetCustomSoftKeyboardLayerEmojiViewHolderBinding;

public class CustomSoftKeyboard extends FrameLayout {

    public CustomSoftKeyboard(@NonNull Context context) {
        super(context);
        initFromAttributes(context, null, 0, 0);
    }

    public CustomSoftKeyboard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs, 0, 0);
    }

    public CustomSoftKeyboard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public CustomSoftKeyboard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private ImsdkSampleWidgetCustomSoftKeyboardBinding mBinding;

    private void initFromAttributes(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {

        mBinding = ImsdkSampleWidgetCustomSoftKeyboardBinding.inflate(
                LayoutInflater.from(context),
                this,
                true);

        initLayerEmoji();

        showLayerEmoji();
    }

    public void showLayerEmoji() {
        ViewUtil.setVisibilityIfChanged(mBinding.layerEmoji, View.VISIBLE);
        ViewUtil.setVisibilityIfChanged(mBinding.layerMore, View.GONE);
    }

    public void showLayerMore() {
        ViewUtil.setVisibilityIfChanged(mBinding.layerEmoji, View.GONE);
        ViewUtil.setVisibilityIfChanged(mBinding.layerMore, View.VISIBLE);
    }

    /**
     * 硬编码自定义键盘上的数据
     */
    private final static class CustomKeyboardDataBuiltin {

        // 1F600-1F64F
        static final int EMOJI_START = 0x1F600;
        static final int EMOJI_END = 0x1F64F;
        static final int EMOJI_COUNT = EMOJI_END - EMOJI_START + 1;
        static final String[] EMOJI = new String[EMOJI_COUNT];

        static {
            for (int i = 0; i < EMOJI_COUNT; i++) {
                EMOJI[i] = new String(Character.toChars(i + EMOJI_START));
            }
        }
    }

    /**
     * 自定义键盘：表情
     */
    private void initLayerEmoji() {
        mBinding.layerEmojiPager.setAdapter(new LayerEmojiPagerAdapter());
    }

    public interface OnInputListener {
        void onInputText(CharSequence text);

        void onDeleteOne();

        void onVoiceClick();
    }

    private OnInputListener mOnInputListener;

    public void setOnInputListener(OnInputListener onInputListener) {
        mOnInputListener = onInputListener;
    }

    private class LayerEmojiPagerAdapter extends RecyclerView.Adapter<LayerEmojiViewHolder> {

        @NonNull
        @Override
        public LayerEmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            return new LayerEmojiViewHolder(inflater.inflate(R.layout.imsdk_sample_widget_custom_soft_keyboard_layer_emoji_view_holder, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull LayerEmojiViewHolder holder, int position) {
        }

        @Override
        public int getItemViewType(int position) {
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    private class LayerEmojiViewHolder extends RecyclerView.ViewHolder {

        private final ImsdkSampleWidgetCustomSoftKeyboardLayerEmojiViewHolderBinding mBinding;

        public LayerEmojiViewHolder(@NonNull View itemView) {
            super(itemView);
            mBinding = ImsdkSampleWidgetCustomSoftKeyboardLayerEmojiViewHolderBinding.bind(itemView);
            final Context context = getContext();
            final String[] allEmoji = CustomKeyboardDataBuiltin.EMOJI;
            final int columns = context.getResources().getInteger(R.integer.imsdk_sample_widget_custom_soft_keyboard_emoji_columns);
            final int size = allEmoji.length;
            final int viewPadding = DimenUtil.dp2px(15);
            final int itemViewWidth = DimenUtil.dp2px(30);
            final int itemViewHeight = DimenUtil.dp2px(30);
            mBinding.gridLayout.setPadding(viewPadding, viewPadding, viewPadding, viewPadding);
            mBinding.gridLayout.setColumnCount(columns);
            mBinding.gridLayout.setUseDefaultMargins(true);

            for (String emoji : allEmoji) {
                inflateEmojiItemView(context, itemViewWidth, itemViewHeight, emoji);
            }
        }

        private void inflateEmojiItemView(Context context, int itemViewWidth, int itemViewHeight, final String emojiText) {
            final EmojiTextView itemView = new EmojiTextView(context);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = itemViewWidth;
            lp.height = itemViewHeight;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1.0f);
            itemView.setLayoutParams(lp);

            itemView.setIncludeFontPadding(false);
            itemView.setGravity(Gravity.CENTER);
            itemView.setText(emojiText);
            itemView.setTextColor(0xFF333333);
            itemView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);

            mBinding.gridLayout.addView(itemView);

            ViewUtil.onClick(itemView, 100, v -> {
                if (mOnInputListener != null) {
                    mOnInputListener.onInputText(emojiText);
                }
            });
        }

    }


}
