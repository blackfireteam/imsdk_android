package com.masonsoft.imsdk.sample.common;

import com.idonans.uniontype.UnionTypeAdapter;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;

public class ItemClickUnionTypeAdapter extends UnionTypeAdapter {

    private UnionTypeViewHolderListeners.OnItemClickListener mOnItemClickListener;
    private UnionTypeViewHolderListeners.OnItemLongClickListener mOnItemLongClickListener;

    public void setOnItemClickListener(UnionTypeViewHolderListeners.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public UnionTypeViewHolderListeners.OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemLongClickListener(UnionTypeViewHolderListeners.OnItemLongClickListener onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    public UnionTypeViewHolderListeners.OnItemLongClickListener getOnItemLongClickListener() {
        return mOnItemLongClickListener;
    }

}
