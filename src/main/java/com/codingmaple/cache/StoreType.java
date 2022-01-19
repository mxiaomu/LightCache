package com.codingmaple.cache;


public enum StoreType {

    VALUE(0, "对象"),
    SET(1, "集合"),
    LIST(2, "列表"),
    BYTE_ARRAY(4, "字节数组");


    private final Integer type;
    private final String description;

    StoreType(final Integer type, final String description){
        this.type = type;
        this.description = description;
    }

    public Integer getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
