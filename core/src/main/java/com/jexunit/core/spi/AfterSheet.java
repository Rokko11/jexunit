package com.jexunit.core.spi;

/**
 * Implementations of this interface can be called after the execution of a Excel file.
 * It will get activated by {@link com.jexunit.core.JExUnitConfig.ConfigKey} jexunit.sheet.after
 */
public interface AfterSheet {

    void run() throws Exception;
}
