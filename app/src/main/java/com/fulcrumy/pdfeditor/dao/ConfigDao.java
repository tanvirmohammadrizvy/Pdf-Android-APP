package com.fulcrumy.pdfeditor.dao;

import com.fulcrumy.pdfeditor.models.ConfigModel;

public interface ConfigDao {
    ConfigModel getConfigBykey(int i);

    void incrementConfigValue(int i);

    void insert(ConfigModel configModel);

    void setRewarded(int i, int i2);
}
