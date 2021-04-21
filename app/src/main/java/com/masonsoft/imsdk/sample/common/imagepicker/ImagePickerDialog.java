package com.masonsoft.imsdk.sample.common.imagepicker;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.ItemClickUnionTypeAdapter;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleCommonImagePickerDialogBinding;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleCommonImagePickerDialogBucketViewBinding;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleCommonImagePickerDialogPagerViewBinding;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.widget.GridItemDecoration;
import com.masonsoft.imsdk.util.Preconditions;

import java.util.List;

import io.github.idonans.backstack.ViewBackLayer;
import io.github.idonans.backstack.dialog.ViewDialog;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.DimenUtil;
import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeItemObject;

public class ImagePickerDialog implements ImageData.ImageLoaderCallback, ViewBackLayer.OnBackPressedListener {

    private static final boolean DEBUG = true;

    private final Activity mActivity;
    private final LayoutInflater mInflater;
    private final ImsdkSampleCommonImagePickerDialogBinding mBinding;
    private ViewDialog mViewDialog;
    public GridView mGridView;
    private BucketView mBucketView;
    private PagerView mPagerView;

    @Nullable
    private UnionTypeImageData mUnionTypeImageData;
    private ImageData.ImageLoader mImageLoader;

    public ImagePickerDialog(Activity activity, ViewGroup parentView) {
        mActivity = activity;
        mInflater = activity.getLayoutInflater();
        mViewDialog = new ViewDialog.Builder(activity)
                .setContentView(R.layout.imsdk_sample_common_image_picker_dialog)
                .defaultAnimation()
                .setOnBackPressedListener(this)
                .setParentView(parentView)
                .dimBackground(true)
                .create();
        //noinspection ConstantConditions,ConstantConditions
        mBinding = ImsdkSampleCommonImagePickerDialogBinding.bind(mViewDialog.getContentView());

        mGridView = new GridView(mBinding);
        mBucketView = new BucketView(mBinding);
        mPagerView = new PagerView(mBinding);

        mImageLoader = new ImageData.ImageLoader(this, mInnerImageSelector);
        mImageLoader.start();
    }

    private final ImageSelector mInnerImageSelector = new ImageSelector.SimpleImageSelector() {
        @Override
        public boolean accept(@NonNull ImageData.ImageInfo info) {
            if (mOutImageSelector != null) {
                if (!mOutImageSelector.accept(info)) {
                    return false;
                }
            }
            return super.accept(info);
        }

        @Override
        public boolean canSelect(@NonNull List<ImageData.ImageInfo> imageInfoListSelected, @NonNull ImageData.ImageInfo info) {
            if (mOutImageSelector != null) {
                if (!mOutImageSelector.canSelect(imageInfoListSelected, info)) {
                    return false;
                }
            }
            return super.canSelect(imageInfoListSelected, info);
        }

        @Override
        public boolean canDeselect(@NonNull List<ImageData.ImageInfo> imageInfoListSelected, int currentSelectedIndex, @NonNull ImageData.ImageInfo info) {
            if (mOutImageSelector != null) {
                if (!mOutImageSelector.canDeselect(imageInfoListSelected, currentSelectedIndex, info)) {
                    return false;
                }
            }
            return super.canDeselect(imageInfoListSelected, currentSelectedIndex, info);
        }

        @Override
        public boolean canFinishSelect(@NonNull List<ImageData.ImageInfo> imageInfoListSelected) {
            if (mOutImageSelector != null) {
                if (!mOutImageSelector.canFinishSelect(imageInfoListSelected)) {
                    return false;
                }
            }
            return super.canFinishSelect(imageInfoListSelected);
        }
    };

    @Nullable
    private ImageSelector mOutImageSelector;

    public void setImageSelector(@Nullable ImageSelector imageSelector) {
        mOutImageSelector = imageSelector;
    }

    private void notifyImageDataChanged() {
        if (mUnionTypeImageData == null) {
            SampleLog.e("notifyImageDataChanged mUnionTypeImageData is null");
            return;
        }
        if (mUnionTypeImageData.imageData.bucketSelected != null) {
            List<UnionTypeItemObject> gridItems = mUnionTypeImageData.unionTypeGridItemsMap.get(mUnionTypeImageData.imageData.bucketSelected);
            mGridView.mDataAdapter.setGroupItems(0, gridItems);

            List<UnionTypeItemObject> pagerItems = mUnionTypeImageData.unionTypePagerItemsMap.get(mUnionTypeImageData.imageData.bucketSelected);
            mPagerView.mDataAdapter.setGroupItems(0, pagerItems);
            //noinspection ConstantConditions
            mPagerView.mRecyclerView.getLayoutManager().scrollToPosition(mUnionTypeImageData.pagerPendingIndex);
        }
        mBucketView.mDataAdapter.setGroupItems(0, mUnionTypeImageData.unionTypeBucketItems);

        String bucketSelectedName = I18nResources.getString(R.string.imsdk_sample_custom_soft_keyboard_item_image);
        if (mUnionTypeImageData.imageData.bucketSelected != null
                && !mUnionTypeImageData.imageData.bucketSelected.allImageInfo) {
            bucketSelectedName = mUnionTypeImageData.imageData.bucketSelected.bucketDisplayName;
        }
        mGridView.mGridTopBarTitle.setText(bucketSelectedName);
        mGridView.updateConfirmNextStatus();
    }

    class GridView {
        private final View mGridTopBarClose;
        private final TextView mGridTopBarTitle;
        private final RecyclerView mRecyclerView;
        private final TextView mActionSubmit;

        private final ItemClickUnionTypeAdapter mDataAdapter;

        private GridView(ImsdkSampleCommonImagePickerDialogBinding parentBinding) {
            mGridTopBarClose = parentBinding.gridTopBarClose;
            mGridTopBarTitle = parentBinding.gridTopBarTitle;
            mRecyclerView = parentBinding.gridRecyclerView;
            mActionSubmit = parentBinding.actionSubmit;

            mRecyclerView.setLayoutManager(
                    new GridLayoutManager(mRecyclerView.getContext(), 3));
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.addItemDecoration(new GridItemDecoration(3, DimenUtil.dp2px(2), false));
            mDataAdapter = new ItemClickUnionTypeAdapter();
            mDataAdapter.setHost(Host.Factory.create(mActivity, mRecyclerView, mDataAdapter));
            mDataAdapter.setUnionTypeMapper(new UnionTypeMapperImpl());
            mDataAdapter.setOnItemClickListener(viewHolder -> {
                Preconditions.checkNotNull(mUnionTypeImageData);
                final int position = viewHolder.getAdapterPosition();
                mUnionTypeImageData.pagerPendingIndex = Math.max(position, 0);
                notifyImageDataChanged();
                mPagerView.show();
            });

            mRecyclerView.setAdapter(mDataAdapter);

            ViewUtil.onClick(mGridTopBarClose, v -> {
                if (ImagePickerDialog.this.onBackPressed()) {
                    return;
                }
                ImagePickerDialog.this.hide();
            });
            ViewUtil.onClick(mGridTopBarTitle, v -> {
                if (mBucketView.onBackPressed()) {
                    return;
                }
                mBucketView.show();
            });
            ViewUtil.onClick(mActionSubmit, v -> {
                if (mUnionTypeImageData == null) {
                    SampleLog.e("mUnionTypeImageData is null");
                    return;
                }
                if (mUnionTypeImageData.imageData.imageInfoListSelected.isEmpty()) {
                    SampleLog.e("mUnionTypeImageData.imageData.imageInfoListSelected.isEmpty()");
                    return;
                }

                if (mOnImagePickListener == null) {
                    SampleLog.v("ignore. mOnImagePickListener is null.");
                    return;
                }
                if (mOnImagePickListener.onImagePick(mUnionTypeImageData.imageData.imageInfoListSelected)) {
                    ImagePickerDialog.this.hide();
                }
            });
        }

        public void updateConfirmNextStatus() {
            boolean enable;
            int count;
            if (mUnionTypeImageData == null) {
                SampleLog.e("mUnionTypeImageData is null");
                count = 0;
                enable = false;
            } else if (mInnerImageSelector.canFinishSelect(mUnionTypeImageData.imageData.imageInfoListSelected)) {
                count = mUnionTypeImageData.imageData.imageInfoListSelected.size();
                enable = true;
            } else {
                count = mUnionTypeImageData.imageData.imageInfoListSelected.size();
                enable = false;
            }
            mActionSubmit.setText(I18nResources.getString(R.string.imsdk_sample_custom_soft_keyboard_item_image_picker_submit_format, count));
            mActionSubmit.setEnabled(enable);
        }
    }

    private class BucketView {
        private final ViewDialog mBucketViewDialog;
        private final RecyclerView mRecyclerView;
        private final ItemClickUnionTypeAdapter mDataAdapter;

        private BucketView(ImsdkSampleCommonImagePickerDialogBinding parentBinding) {
            final ViewGroup parentView = parentBinding.bucketOverlayContainer;
            mBucketViewDialog = new ViewDialog.Builder(mActivity)
                    .setParentView(parentView)
                    .setContentView(R.layout.imsdk_sample_common_image_picker_dialog_bucket_view)
                    .setContentViewShowAnimation(R.anim.backstack_slide_in_from_top)
                    .setContentViewHideAnimation(R.anim.backstack_slide_out_to_top)
                    .dimBackground(true)
                    .create();
            //noinspection ConstantConditions
            final ImsdkSampleCommonImagePickerDialogBucketViewBinding binding =
                    ImsdkSampleCommonImagePickerDialogBucketViewBinding.bind(mBucketViewDialog.getContentView());
            mRecyclerView = binding.recyclerView;

            mRecyclerView.setLayoutManager(
                    new LinearLayoutManager(mRecyclerView.getContext()));
            mRecyclerView.setHasFixedSize(false);
            mDataAdapter = new ItemClickUnionTypeAdapter();
            mDataAdapter.setHost(Host.Factory.create(mActivity, mRecyclerView, mDataAdapter));
            mDataAdapter.setUnionTypeMapper(new UnionTypeMapperImpl());
            mDataAdapter.setOnItemClickListener(viewHolder -> {
                Preconditions.checkNotNull(mUnionTypeImageData);
                int size = mUnionTypeImageData.imageData.allSubBuckets.size();
                final int position = viewHolder.getAdapterPosition();
                if ((position < 0 || position >= size)) {
                    SampleLog.e("BucketView onItemClick invalid position: %s, size:%s", position, size);
                    BucketView.this.hide();
                    return;
                }

                ImageData.ImageBucket imageBucket = mUnionTypeImageData.imageData.allSubBuckets.get(position);
                if (ObjectsCompat.equals(mUnionTypeImageData.imageData.bucketSelected, imageBucket)) {
                    if (DEBUG) {
                        SampleLog.v("BucketView onItemClick ignore. same as last bucket selected");
                    }
                    BucketView.this.hide();
                    return;
                }

                BucketView.this.hide();
                mUnionTypeImageData.imageData.bucketSelected = imageBucket;
                notifyImageDataChanged();
            });

            mRecyclerView.setAdapter(mDataAdapter);
        }

        public void show() {
            mBucketViewDialog.show();
        }

        public void hide() {
            mBucketViewDialog.hide(false);
        }

        public boolean onBackPressed() {
            if (mBucketViewDialog.isShown()) {
                mBucketViewDialog.hide(false);
                return true;
            }
            return false;
        }
    }

    private class PagerView {

        private final ViewDialog mPagerViewDialog;
        @SuppressWarnings("FieldCanBeLocal")
        private final ImsdkSampleCommonImagePickerDialogPagerViewBinding mBinding;
        private final RecyclerView mRecyclerView;
        private final ItemClickUnionTypeAdapter mDataAdapter;

        private PagerView(ImsdkSampleCommonImagePickerDialogBinding parentBinding) {
            final ViewGroup parentView = parentBinding.pagerOverlayContainer;
            mPagerViewDialog = new ViewDialog.Builder(mActivity)
                    .setParentView(parentView)
                    .setContentView(R.layout.imsdk_sample_common_image_picker_dialog_pager_view)
                    .create();
            //noinspection ConstantConditions
            mBinding = ImsdkSampleCommonImagePickerDialogPagerViewBinding.bind(mPagerViewDialog.getContentView());
            mRecyclerView = mBinding.recyclerView;
            mRecyclerView.setLayoutManager(
                    new LinearLayoutManager(mRecyclerView.getContext(), RecyclerView.HORIZONTAL, false));
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
            PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
            pagerSnapHelper.attachToRecyclerView(mRecyclerView);
            mDataAdapter = new ItemClickUnionTypeAdapter();
            mDataAdapter.setHost(Host.Factory.create(mActivity, mRecyclerView, mDataAdapter));
            mDataAdapter.setUnionTypeMapper(new UnionTypeMapperImpl());
            mDataAdapter.setOnItemClickListener(viewHolder -> PagerView.this.hide());
            mRecyclerView.setAdapter(mDataAdapter);
        }

        public void show() {
            mPagerViewDialog.show();
        }

        public void hide() {
            mPagerViewDialog.hide(false);
        }

        public boolean onBackPressed() {
            if (mPagerViewDialog.isShown()) {
                mPagerViewDialog.hide(false);
                return true;
            }
            return false;
        }
    }

    public void show() {
        mViewDialog.show();
    }

    public void hide() {
        mViewDialog.hide(false);
        mImageLoader.close();
    }

    @Override
    public void onLoadFinish(@NonNull ImageData imageData) {
        if (DEBUG) {
            SampleLog.v("onLoadFinish buckets:%s, images:%s", imageData.allSubBuckets.size(), imageData.allImageInfoListMap.size());
        }
        imageData.bucketSelected = imageData.allImageInfoListBucket;
        UnionTypeImageData unionTypeImageData = new UnionTypeImageData(this, imageData);
        Threads.runOnUi(() -> {
            mUnionTypeImageData = unionTypeImageData;
            notifyImageDataChanged();
        });
    }

    @Override
    public boolean onBackPressed() {
        if (mBucketView.onBackPressed()) {
            return true;
        }

        if (mPagerView.onBackPressed()) {
            return true;
        }

        return false;
    }

    public interface OnImagePickListener {
        /**
         * 关闭 ImagePicker，返回 true.
         */
        boolean onImagePick(@NonNull List<ImageData.ImageInfo> imageInfoList);
    }

    private OnImagePickListener mOnImagePickListener;

    public void setOnImagePickListener(OnImagePickListener onImagePickListener) {
        mOnImagePickListener = onImagePickListener;
    }

}
