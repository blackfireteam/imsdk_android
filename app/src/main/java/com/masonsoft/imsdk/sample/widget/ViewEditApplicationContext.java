package com.masonsoft.imsdk.sample.widget;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import com.google.common.base.Preconditions;

/**
 * View 编辑器预览模式下设置全局 Context
 * <pre>
 * class View {
 *     ...
 *     if (isInEditMode()) {
 *         AppContext.setContext(new ViewEditApplicationContext(this));
 *     }
 *     int size = DimenUtil.dp2px(10);
 *     ...
 * }
 * </pre>
 *
 * @since 1.0
 */
public class ViewEditApplicationContext extends ContextWrapper {

    private final ApplicationMock mApplication;

    public ViewEditApplicationContext(View view) {
        super(null);
        Preconditions.checkArgument(view.isInEditMode());
        mApplication = new ApplicationMock(view.getContext());
    }

    @Override
    public Context getApplicationContext() {
        return mApplication;
    }

    private static class ApplicationMock extends Application {

        public ApplicationMock(Context context) {
            attachBaseContext(context);
        }

    }

}
