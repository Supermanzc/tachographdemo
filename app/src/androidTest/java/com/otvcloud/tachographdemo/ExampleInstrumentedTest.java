package com.otvcloud.tachographdemo;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.otvcloud.tachographdemo.bean.Tachograph;
import com.otvcloud.tachographdemo.bean.dao.TachographDao;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.otvcloud.tachographdemo", appContext.getPackageName());
    }

    @Test
    public void findAll(){
        List<Tachograph> tachographList = TachographDao.getInstance().findAllTachograph(5,"desc");
        System.out.print("findAll:" + tachographList.size() + "");
    }
}
