package com.fulcrumy.pdfeditor.db;

import androidx.room.RoomDatabase;

import com.fulcrumy.pdfeditor.dao.ConfigDao;

public abstract class AppDatabase extends RoomDatabase {
    public abstract ConfigDao configDao();
}
