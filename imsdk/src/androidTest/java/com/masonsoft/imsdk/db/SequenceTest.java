package com.masonsoft.imsdk.db;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.db.Sequence;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SequenceTest {

    @Test
    public void testSequence() {
        for (int i = 0; i < 10; i++) {
            final long microSeconds = SignGenerator.nextMicroSeconds();
            final long seq = Sequence.create(microSeconds);
            System.out.println("===========================================");
            System.out.println("microSeconds:" + Long.toBinaryString(microSeconds));
            System.out.println("seq:" + Long.toBinaryString(seq));
        }
    }

}