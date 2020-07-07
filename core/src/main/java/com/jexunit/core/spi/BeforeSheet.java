package com.jexunit.core.spi;

/**
 * Implementations of this interface can be called before the execution of a Excel file.
 * It will get activated by {@link com.jexunit.core.JExUnitConfig.ConfigKey} jexunit.sheet.after
 */
public interface BeforeSheet {

    void run() throws Exception;
}
