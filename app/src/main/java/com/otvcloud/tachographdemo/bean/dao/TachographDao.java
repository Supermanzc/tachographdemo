package com.otvcloud.tachographdemo.bean.dao;

import com.otvcloud.tachographdemo.TachographManager;
import com.otvcloud.tachographdemo.bean.Tachograph;
import com.otvcloud.tachographdemo.util.FileSizeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.orm.SugarRecord.find;

/**
 * Created by android_jy on 2017/10/23.
 */

public class TachographDao {

    private static TachographDao tachograph;

    private TachographDao() {
    }

    public static final TachographDao getInstance() {
        if (tachograph == null) {
            tachograph = new TachographDao();
        }
        return tachograph;
    }

    /**
     * 保存数据信息
     *
     * @param filePath
     * @param createTime
     */
    public void save(String filePath, String createTime) {
        Tachograph tachograph = new Tachograph();
        tachograph.setFilePath(filePath);
        tachograph.setCreateTime(createTime);
        tachograph.save();
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(int id) {
        Tachograph book = Tachograph.findById(Tachograph.class, id);
        book.delete();
    }

    /**
     * 发现可用的本地视频(进行视频合并处理)
     *
     * @param pageSize 获取的大小
     * @param orderBy  当前的排序
     * @return
     */
    public List<Tachograph> findAllTachograph(int pageSize, String orderBy) {
        return Tachograph.find(Tachograph.class, null, null, null, "id " + orderBy, "" + pageSize);
    }
}
