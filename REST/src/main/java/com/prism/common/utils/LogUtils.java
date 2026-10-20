package com.prism.common.utils;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.prism.common.exceptions.CoreException;
import com.prism.common.logging.LogCategory;

import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
//import org.apache.log4j.LogManager;
//import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This is a utility class for use with Log4J to help with tracing 
 * messages. This class is also intended to provide a way to convert 
 * objects into an array of String objects that describe the
 * object's properties. This will be used to give insight into an 
 * object's state at run time.
 */
@SuppressWarnings({"rawtypes"})
public class LogUtils {
    /**
     * The log4j logger.
     */
    private static final Logger log = LogCategory.CORE.logger(LogUtils.class);

    /**
     * Log the exception. If the exception is of type ITInterfaceException, 
     * walk the chain of exceptions and log each one until you get to the 
     * end of the chain.
     * 
     * @param ex
     *            The exception to log.
     * @return The value of the StackTraceAsString property.
     */
    public static String getStackTraceAsString(java.lang.Throwable ex) {
        // Retrieve stack trace from exception.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(stream);

        ex.printStackTrace(printStream);
        return stream.toString();
    }

    /**
     * Writes the details of the specified object to System.out.
     * @param obj Object
     * @param prefix String
     * @param showTypes boolean
     * @throws CoreException
     */
    public static void println(Object obj, String prefix, boolean showTypes) throws CoreException {
        String[] lines = dataToStrings(obj, showTypes);
        for (int i=0; i <= lines.length-1; i++) {
            System.out.println(prefix + lines[i]);
        }
    }

    /**
     * Write the details of the specified object using info level.
     * @param logger Logger
     * @param obj Object
     * @throws CoreException
     */
    public static void debug(Logger logger, Object obj) throws CoreException {
        String[] lines = dataToStrings(obj);
        for (int i=0; i <= lines.length-1; i++) {
            logger.debug("   " + lines[i]);
        }
    }
    
    /**
     * Write the details of the specified object using info level.
     * @param logger Logger
     * @param obj Object
     * @throws CoreException
     */
    public static void debug(Logger logger, Object obj, String prefix, boolean showTypes) throws CoreException {
        String[] lines = dataToStrings(obj, showTypes);
        for (int i=0; i <= lines.length-1; i++) {
            logger.debug(prefix + lines[i]);
        }
    }
    
    /**
     * Write the details of the specified object using info level.
     * @param logger Logger
     * @param obj Object
     * @throws CoreException
     */
    public static void info(Logger logger, Object obj) throws CoreException {
        if (obj == null)
            logger.error("LogUtils.info()  'obj' is NULL.");
        String[] lines = dataToStrings(obj);
        if (lines == null)
            logger.error("LogUtils.info()  'lines' is NULL.");
        for (int i=0; i <= lines.length-1; i++) {
            logger.info("   " + lines[i]);
        }
    }
    
    /**
     * Write the details of the specified object using info level.
     * @param logger Logger
     * @param obj Object
     * @throws CoreException
     */
    public static void info(Logger logger, Object obj, String prefix, boolean showTypes) throws CoreException {
        if (obj == null)
            logger.error("LogUtils.info()  'obj' is NULL.");
        String[] lines = dataToStrings(obj, showTypes);
        if (lines == null)
            logger.error("LogUtils.info()  'lines' is NULL.");
        for (int i=0; i <= lines.length-1; i++) {
            logger.info(prefix + lines[i]);
        }
    }
    
    /**
     * Convert a bean to an array of String objects that contain the properties of the bean. This is
     * used to log out the details of the bean.
     * 
     * @param bean
     *            Object
     * @return String[]
     */
    public static String[] dataToStrings(Object bean) throws CoreException { 
        return dataToStrings(bean, true);       
    }
    
    /**
     * Convert a bean to an array of String objects that contain the properties of the bean. This is
     * used to log out the details of the bean.
     * 
     * @param bean
     *            Object
     * @return String[]
     */
    public static String[] dataToStrings(Object bean, boolean showTypes) throws CoreException {
        ArrayList<String> lines = new ArrayList<String>();
        String[] result = null;

        if (bean == null) {
            result = new String[1];
            result[0] = "null";
            return result;
        }    
        
        //log.debug("Extracting bean properties...   " + bean.getClass().getName());
        //String fieldName = null;

        try {
            processBean(bean, 0, lines, showTypes);

            result = new String[lines.size()];
            // Convert Vector to array.
            for (int F = 0; F <= lines.size() - 1; F++) {
                result[F] = lines.get(F).toString();
            }

            return result;
        } catch (Exception e) {
            CoreException ex = new CoreException("Error loading bean (" + bean.getClass().getName() + ").", e);
            log.warn("Error writing to log file.", ex);
            throw ex;
        }
    }

    private static ArrayList<String> processBean(Object bean, int level, ArrayList<String> lines, boolean showTypes)
                                    throws CoreException {

        if (bean == null) {
            lines.add(indent(level) + "null");
            return lines;
        }    
        try {
            Object[] args = new Object[0];
            String fieldName = null;
            PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(bean.getClass());

            if (pds == null || pds.length == 0) {
                return lines;
            }

            StringBuffer line = null;

            if (bean instanceof Map) {
                Map map = (Map)bean;
                Iterator keys = map.keySet().iterator();
                if (! keys.hasNext())
                    lines.add(indent(level) + "Map is empty.");
                while (keys.hasNext()) {
                    String key = keys.next().toString();
                    Object val = map.get(key);
                    if (val instanceof Integer 
                     || val instanceof Long
                     || val instanceof Double
                     || val instanceof Boolean
                     || val instanceof BigDecimal
                     || val instanceof BigInteger
                     || val instanceof java.util.Date
                     || val instanceof Float
                     || val instanceof String) {
                        line = new StringBuffer();
                        line.append(indent(level));
                        line.append(key);
                        line.append(" = ");
                        line.append(val.toString());
                        lines.add(line.toString());
                    } else {
                        line = new StringBuffer();
                        line.append(indent(level));
                        line.append(key);
                        line.append(" = ");
                        if (val == null)
                            line.append("null");
                        else
                            line.append(val.getClass().getName());
                        lines.add(line.toString());
                        processBean(val, level + 1, lines, showTypes);
                    }
                }
            } else if (bean instanceof Document) {
                Document doc = (Document)bean;
                
                lines.add(indent(level) + "XML Document:");
                String name = null;
                String value = null;
/*                
                String name = doc.getNodeName();
                String value = doc.getNodeValue();
                if (value == null)
                    lines.add(indent(level+1) + name);
                else
                    lines.add(indent(level+1) + name + " = " + value);
*/                
                NamedNodeMap attributes = doc.getAttributes();
                if (attributes != null) {
                    for (int i=0; i <= attributes.getLength()-1; i++) {
                        Node attr = attributes.item(i);
                        name = attr.getNodeName();
                        value = attr.getNodeValue();
                        if (value == null)
                            lines.add(indent(level+2) + "Attr: " + name);
                        else
                            lines.add(indent(level+2) + "Attr: " + name + " = " + value);
                    }    
                }
                
                Node element = doc.getFirstChild();
                if (element != null) {
                    name = element.getNodeName();
                    if (name == null || ! name.equals("#text")) {
                        processBean(element, level + 1, lines, showTypes);
                    }
                    element = element.getNextSibling();
                    while (element != null) {
                        name = element.getNodeName();
                        if (name == null || ! name.equals("#text")) {
                            processBean(element, level + 1, lines, showTypes);
                        }    
                        element = element.getNextSibling();
                    }
                }
            } else if (bean instanceof Node) {
                Node node = (Node)bean;
                
                String name = node.getNodeName();
                String value = node.getNodeValue();

                Node element = node.getFirstChild();
                if (element != null) {
                    String childname = element.getNodeName();
                    if (childname != null && childname.equals("#text")) {
                        value = element.getNodeValue();        
                    }
                }     
                
                if (value == null || value.trim().equals(""))
                    lines.add(indent(level) + name);
                else
                    lines.add(indent(level) + name + " = " + value);

                NamedNodeMap attributes = node.getAttributes();
                if (attributes != null) {
                    for (int i=0; i <= attributes.getLength()-1; i++) {
                        Node attr = attributes.item(i);
                        name = attr.getNodeName();
                        value = attr.getNodeValue();
                        if (value == null)
                            lines.add(indent(level+1) + "Attr: " + name);
                        else
                            lines.add(indent(level+1) + "Attr: " + name + " = " + value);
                    }    
                }
                
                if (element != null) {
                    name = element.getNodeName();
                    if (name == null || ! name.equals("#text")) {
                        processBean(element, level + 1, lines, showTypes);
                    }
                    element = element.getNextSibling();
                    while (element != null) {
                        name = element.getNodeName();
                        if (name == null || ! name.equals("#text")) {
                            processBean(element, level + 1, lines, showTypes);
                        }    
                        element = element.getNextSibling();
                    }
                }
                
            } else if (bean instanceof Collection) {
                // Process the collection.
                Collection coll = (Collection) bean;
                Object[] list = coll.toArray();
                //log.debug(indent(level) + "Bean is a collection...   count=" + list.length);

                if (list.length == 0)
                   lines.add(indent(level) + "List is empty.");
                
                for (int k = 0; k < list.length; k++) {
                    StringBuffer nextItem = new StringBuffer();

                    // Add indentation to simulate object hierarchy.
                    nextItem.append(indent(level));
                    if (list[k] instanceof String)
                        nextItem.append("Item #" + k + ": " + list[k]);
                    else if (showTypes)
                        nextItem.append("Item #" + k + ": " + list[k].getClass().getName());
                    else
                        nextItem.append("Item #" + k + ": ");
                        
                    lines.add(nextItem.toString());

                    // Process the next bean.
                    if (! (list[k] instanceof String))
                        processBean(list[k], level + 1, lines, showTypes);
                }
            } else {
                // Loop through the properties of the bean.
                //log.debug(indent(level) + "Extracting bean properties...   count=" + pds.length);
                for (int i = 0; i < pds.length; i++) {
                    fieldName = pds[i].getName();
                    
                    try {
                        if (! (fieldName.equalsIgnoreCase("class") ||
                               fieldName.equalsIgnoreCase("parent") ||
                               fieldName.equalsIgnoreCase("declaringclass"))) {
                            //log.debug(indent(level) + "   " + fieldName);

                            // Get the getter method for this property.
                            Method method = pds[i].getReadMethod();

                            if (method != null) {
                                Object value = method.invoke(bean, args);

                                if (value == null)
                                    value = "null";

                                // Add indentation to simulate object hierarchy.
                                line = new StringBuffer();
                                line.append(indent(level));
                                line.append(fieldName);
                                line.append(" = ");
                                if (fieldName.endsWith("Class") || fieldName.endsWith("class")) {
                                    line.append("SKIPPING");
                                } else {
                                    if (value instanceof Map) {
                                        if (showTypes)
                                            line.append(value.getClass().getName());
                                        else
                                            line.append("Map");

                                        lines.add(line.toString());
                                        //log.debug(indent(level) + "    Property is a map.  Calling processBean()...");
                                        processBean(value, level + 1, lines, showTypes);
                                    }
                                    else if (value instanceof Collection) {
                                        // Process the collection.
                                        if (showTypes)
                                            line.append(value.getClass().getName());
                                        else
                                            line.append("List");

                                        lines.add(line.toString());
                                        //log.debug(indent(level) + "    Property is a collection.  Calling processBean()...");
                                        processBean(value, level + 1, lines, showTypes);
                                    } else if (value instanceof Integer
                                            || value instanceof Long
                                            || value instanceof Double
                                            || value instanceof Boolean
                                            || value instanceof BigDecimal
                                            || value instanceof BigInteger
                                            || value instanceof java.util.Date
                                            || value instanceof Float
                                            || value instanceof String) {
                                        // Append the actual property value converted to a String.
                                        line.append(value.toString());
                                        lines.add(line.toString());
                                    } else {
                                        // Append the actual property value converted to a String.
                                        line.append(value.getClass().getName());
                                        lines.add(line.toString());

                                        // Process the next bean.
                                        processBean(value, level + 1, lines, showTypes);
                                    }
                                }

                            } else {
                                line = new StringBuffer();
                                line.append("   ");
                                line.append(fieldName);
                                line.append(" = NO GET METHOD FOUND.");
                            }
                        }
                    } catch (Exception e) {
                        line = new StringBuffer();
                        line.append(indent(level));
                        line.append(fieldName);
                        line.append(" = ERROR: " + e.getMessage());
                        lines.add(line.toString());
                    }
                }
            }

            return lines;
        } catch (Exception e) {
            String className = null;
            if (bean != null)
                className = bean.getClass().getName();
            throw new CoreException("Error processing bean to log data: "
                                               + className + ".", e);
        }
    }

    /**
     * Indent to the desired level (3 spaces per level).
     * @param level int
     * @return String
     */
    public static String indent(int level) {
        StringBuffer buffer = new StringBuffer();
        for (int m = 0; m <= level-1; m++)
            buffer.append("   ");
        return buffer.toString();
    }
    
    
    /**
     * Convert milliseconds to String representation of duration (HH:MM:SS).
     * 
     * @param value
     *            long
     * @return String
     */
    public static String longToDuration(long value) {
        StringBuffer result = new StringBuffer();
        String seconds = null;
        String minutes = null;
        String hours = null;

        // Seconds
        int milliseconds = (int)(value % 1000);
        long newValue = value / 1000;
        long remainder = newValue % 60;

        if (remainder < 10)
            seconds = "0" + remainder;
        else
            seconds = "" + remainder;

        // Minutes
        newValue = newValue / 60;
        remainder = newValue % 60;
        if (remainder < 10)
            minutes = "0" + remainder;
        else
            minutes = "" + remainder;

        // Hours
        remainder = newValue / 60;
        if (remainder < 10)
            hours = "0" + remainder;
        else
            minutes = "" + remainder;

        result.append(hours);
        result.append(':');
        result.append(minutes);
        result.append(':');
        result.append(seconds);
        result.append('.');
        result.append(StringUtils.leadingZeros(milliseconds, 3));
        return result.toString();
    }
       
}
