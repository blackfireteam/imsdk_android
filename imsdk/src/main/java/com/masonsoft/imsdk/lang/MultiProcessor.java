package com.masonsoft.imsdk.lang;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MultiProcessor<T> implements Processor<T> {

    private final List<Processor<T>> mProcessorArray = new ArrayList<>();

    public void addLastProcessor(Processor<T> processor) {
        synchronized (mProcessorArray) {
            mProcessorArray.add(processor);
        }
    }

    public void addFirstProcessor(Processor<T> processor) {
        synchronized (mProcessorArray) {
            mProcessorArray.add(0, processor);
        }
    }

    @Override
    public boolean doProcess(@Nullable T target) {
        synchronized (mProcessorArray) {
            for (Processor<T> processor : mProcessorArray) {
                if (processor != null) {
                    if (processor.doProcess(target)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
