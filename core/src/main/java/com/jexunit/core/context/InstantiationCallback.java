package com.jexunit.core.context;

/**
 * Callback interface. Implementations which are set into the {@link TestContextManager} will be called right after
 * creation of {@link com.jexunit.core.commands.annotation.TestCommand} annotated classes.
 */
public interface InstantiationCallback {
    /**
     * Method which will be executed
     */
    <T> void perform(Class<T> type, T instance) throws Exception;
}
