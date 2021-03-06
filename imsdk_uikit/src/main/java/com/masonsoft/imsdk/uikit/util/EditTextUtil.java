package com.masonsoft.imsdk.uikit.util;

import android.widget.EditText;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.uikit.IMUIKitLog;

/**
 * EditText 相关辅助类
 */
public class EditTextUtil {

    private EditTextUtil() {
    }

    /**
     * 根据光标位置的不同插入数据
     */
    public static void insertText(@NonNull EditText view, CharSequence text) {
        int selectionStart = view.getSelectionStart();
        if (selectionStart >= 0) {
            int selectionEnd = view.getSelectionEnd();
            if (selectionEnd >= 0 && selectionEnd > selectionStart) {
                view.getText().replace(selectionStart, selectionEnd, text);
            } else {
                view.getText().insert(selectionStart, text);
            }
        } else {
            IMUIKitLog.e("insertText invalid selectionStart:%s", selectionStart);
        }
    }

    /**
     * 根据光标位置的不同删除数据
     */
    public static void deleteOne(@NonNull EditText view) {
        int selectionStart = view.getSelectionStart();
        if (selectionStart >= 0) {
            int selectionEnd = view.getSelectionEnd();
            if (selectionEnd >= 0 && selectionEnd > selectionStart) {
                view.getText().delete(selectionStart, selectionEnd);
            } else if (selectionStart > 0) {
                // 如果是 emoji, 需要删除紧挨着的两个 char
                if (selectionStart > 1) {
                    char char1 = view.getText().charAt(selectionStart - 2);
                    char char2 = view.getText().charAt(selectionStart - 1);
                    if (Character.isSurrogatePair(char1, char2)) {
                        view.getText().delete(selectionStart - 2, selectionStart);
                        return;
                    }
                }
                view.getText().delete(selectionStart - 1, selectionStart);
            }
        } else {
            IMUIKitLog.e("deleteOne invalid selectionStart:%s", selectionStart);
        }
    }

}
