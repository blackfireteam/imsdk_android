package com.masonsoft.imsdk.sample.uniontype;

import io.github.idonans.uniontype.UnionTypeViewHolder;

public interface UnionTypeViewHolderListeners {

    interface OnItemClickListener {
        void onItemClick(UnionTypeViewHolder viewHolder);
    }

    interface OnItemLongClickListener {
        void onItemLongClick(UnionTypeViewHolder viewHolder);
    }

}
