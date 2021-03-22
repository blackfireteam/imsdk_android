package com.masonsoft.imsdk.sample.app.conversation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.masonsoft.imsdk.sample.databinding.ImsdkSampleConversationFragmentBinding;

/**
 * 会话
 */
public class ConversationFragment extends Fragment {

    public static ConversationFragment newInstance() {
        Bundle args = new Bundle();
        ConversationFragment fragment = new ConversationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return ImsdkSampleConversationFragmentBinding.inflate(inflater, container, false).getRoot();
    }

}
