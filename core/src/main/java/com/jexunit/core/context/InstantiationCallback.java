package com.jexunit.core.context;

public interface InstantiationCallback {
    <T> void perform(Class<T> type, T instance) throws Exception;
}
