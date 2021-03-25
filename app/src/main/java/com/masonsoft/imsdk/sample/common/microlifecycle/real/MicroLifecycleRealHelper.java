package com.masonsoft.imsdk.sample.common.microlifecycle.real;

import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManager;

import java.util.Collection;

public class MicroLifecycleRealHelper {

    private MicroLifecycleRealHelper() {
    }

    public static void requestPauseOthers(MicroLifecycleComponentManager.MicroLifecycleComponent self) {
        requestPauseOthers(self, null);
    }

    public static void requestPauseOthers(MicroLifecycleComponentManager.MicroLifecycleComponent self, Class type) {
        try {
            Collection<MicroLifecycleComponentManager.MicroLifecycleComponent> components = self.getMicroLifecycleComponentManager().copyComponents();
            for (MicroLifecycleComponentManager.MicroLifecycleComponent item : components) {
                if (type != null) {
                    if (!type.isInstance(item)) {
                        continue;
                    }
                }
                if (item == self) {
                    continue;
                }
                if (!(item instanceof RealHost)) {
                    SampleLog.v("ignore. item is not RealHost type %s", item);
                    continue;
                }
                Real real = ((RealHost) item).getReal();
                if (real != null) {
                    real.forcePause();
                }
            }
        } catch (Throwable e) {
            SampleLog.e(e);
            e.printStackTrace();
        }
    }

}
