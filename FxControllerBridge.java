package pro.carl.fxcontrollerbridge;

import javafx.fxml.FXML;
import javafx.util.Pair;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;


/**
 * JavaFX Controller Bridge
 * Smart helper to deal with JavaFX sub-controllers.
 *
 * By @imifos / https://github.com/imifos/FxControllerBridge
 *
 * License:
 * The author accepts no liability for damage caused by this code. If you do
 * not accept this condition then you are prohibited from using this tool.
 *
 * In all other respects the Apache License version 2 applies.
 * *
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the Apache License version 2 as
 * published by the Apache Foundation.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
public class FxControllerBridge {

    public static boolean debug=true;

    // The BridgedFXML annotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface BridgedFXML {
    }


    // Caches @FXML annotated member <names,value> (values = reference of the FX control)
    private final Map<String,Object> fxmlMembers=new HashMap<>();

    // Caches the @BridgeFXML annotated event handler names, and the controller where found
    private final List<Pair<String,Object>> fxmlMethods=new ArrayList<>();


    /**
     * Does the magic...
     */
    public void interlace(Object fxController) {

        if (FxControllerBridge.debug) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            System.out.println("FxControllerBridge: interlacing in "+stackTrace[2]);
        }

        scanForFXMLMembers(fxController);
        injectIntoBridgedFXMLMembers(fxController);
        scanForBridgeFXMLEventHandlers(fxController);
    }



    /**
     * Must be called when the original FXML event handler is invoked.
     * Searches all registered bridge-event handlers and invokes all sub-controller handlers based on the method name.
     */
    public void event() {

        boolean found=false;

        try {
            // Obtain the name of the caller which should be the original event handler
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callerMethod = stackTrace[2].getMethodName();

            // Invoke all known instances, annotated with @BridgedFXML
            for (Pair<String, Object> eventHandler : fxmlMethods) {

                if (eventHandler.getKey().equals(callerMethod)) {
                    Method method=eventHandler.getValue().getClass().getDeclaredMethod(eventHandler.getKey());
                    method.setAccessible(true);

                    if (FxControllerBridge.debug)
                        System.out.println("FxControllerBridge: Bridge event handler "+method.getName()+"() invoked in "+eventHandler.getValue().getClass()+", called from "+stackTrace[2]);

                    method.invoke(eventHandler.getValue());
                    found=true;
                }
            }

            if (!found && FxControllerBridge.debug)
                System.out.println("FxControllerBridge: Forward to bridge event handlers requested, but no handler found. Origin: "+stackTrace[2]);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * Loads all class meta information of event handlers annotated with 'BridgeFXML' into the internal map.
     */
    private void scanForBridgeFXMLEventHandlers(Object fxController) {

        try {
            for (Method method : fxController.getClass().getDeclaredMethods()) {

                if (method.getAnnotation(BridgedFXML.class)!=null) {
                    if (FxControllerBridge.debug)
                        System.out.println("FxControllerBridge: Found bridge event handler in "+fxController.getClass().getSimpleName()+".java: "+method.getName()+"()");
                    fxmlMethods.add(new Pair(method.getName(),fxController));
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * Loads all values of class members annotated with 'FXML' injection into the internal map.
     */
    private void scanForFXMLMembers(Object fxController) {

        try {
            for (Field field : fxController.getClass().getDeclaredFields()) {

                field.setAccessible(true); // overwrite 'privacy'...

                if (fxmlMembers.containsKey(field.getName()) && field.getAnnotation(BridgedFXML.class)==null) {
                    // Most common reason is a @FXML tag in a sub-controller instead the @BridgedFXML annotation
                    if (FxControllerBridge.debug)
                        System.out.println("FxControllerBridge: Conflict in ("+fxController.getClass().getSimpleName()+".java:1): Field ["+field.getName()+"] already loaded from another controller\n");
                }

                Object value = field.get(fxController);
                if (value!=null && field.getAnnotation(FXML.class)!=null) {

                    if (FxControllerBridge.debug)
                        System.out.println("FxControllerBridge: Load ["+field.getName()+"] being "+value);

                    fxmlMembers.put(field.getName(), value);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * Injects FXML member values into 'interlaced' class members that are annotated with 'BridgedFXML'.
     */
    private void injectIntoBridgedFXMLMembers(Object fxController) {

        try {
            for (Field field : fxController.getClass().getDeclaredFields()) {

                Object value=fxmlMembers.get(field.getName());
                if (value!=null) {

                    if (field.getAnnotation(BridgedFXML.class)!=null) {
                        if (FxControllerBridge.debug)
                            System.out.println("FxControllerBridge: Inject into ("+fxController.getClass().getSimpleName()+".java:1) [" + field.getName() + "] being " + value);
                        field.setAccessible(true);
                        field.set(fxController, value);
                    }
                    else {
                        if (field.getAnnotation(FXML.class)==null && FxControllerBridge.debug)
                            System.out.println("FxControllerBridge: Warning! "+fxController.getClass().getSimpleName()+".java: Identified possible injection point without @BridgedFXML annotation [" + field.getName()+"]");
                    }

                } // value!=null
            } // for field

        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
