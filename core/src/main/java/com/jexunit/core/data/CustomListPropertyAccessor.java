package com.jexunit.core.data;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ognl.DynamicSubscript;
import ognl.ListPropertyAccessor;
import ognl.NoSuchPropertyException;
import ognl.Node;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlOps;
import ognl.OgnlRuntime;

/**
 * Implementation of PropertyAccessor that uses numbers and dynamic subscripts as properties to
 * index into Lists.
 *
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
public class CustomListPropertyAccessor extends ListPropertyAccessor {

	public Object getProperty(Map context, Object target, Object name) throws OgnlException {
		List<Object> list = (List<Object>) target;

		if (name instanceof String) {
			Object result = null;

			if (name.equals("size")) {
				result = new Integer(list.size());
			} else {
				if (name.equals("iterator")) {
					result = list.iterator();
				} else {
					if (name.equals("isEmpty") || name.equals("empty")) {
						result = list.isEmpty() ? Boolean.TRUE : Boolean.FALSE;
					} else {
						result = super.getProperty(context, target, name);
					}
				}
			}

			return result;
		}

		if (name instanceof Number) {
			int index = ((Number) name).intValue();
			if (index < list.size()) {
				return list.get(((Number) name).intValue());
			} else {
				// here we have to add a new element to the list!
				OgnlContext ctx = (OgnlContext) context;
				Node currentNode = ctx.getCurrentNode().jjtGetParent().jjtGetParent();
				Object instance = createNewInstance(ctx.getCurrentType(), currentNode
						.jjtGetChild(0).toString());
				list.add(instance);
				return instance;
			}
		}

		if (name instanceof DynamicSubscript) {
			int len = list.size();
			switch (((DynamicSubscript) name).getFlag()) {
			case DynamicSubscript.FIRST:
				return len > 0 ? list.get(0) : null;
			case DynamicSubscript.MID:
				return len > 0 ? list.get(len / 2) : null;
			case DynamicSubscript.LAST:
				return len > 0 ? list.get(len - 1) : null;
			case DynamicSubscript.ALL:
				return new ArrayList(list);
			}
		}

		throw new NoSuchPropertyException(target, name);
	}

	public void setProperty(Map context, Object target, Object name, Object value)
			throws OgnlException {
		if (name instanceof String && ((String) name).indexOf("$") < 0) {
			super.setProperty(context, target, name, value);
			return;
		}

		List list = (List) target;

		if (name instanceof Number) {
			list.set(((Number) name).intValue(), value);
			return;
		}

		if (name instanceof DynamicSubscript) {
			int len = list.size();
			switch (((DynamicSubscript) name).getFlag()) {
			case DynamicSubscript.FIRST:
				if (len > 0)
					list.set(0, value);
				return;
			case DynamicSubscript.MID:
				if (len > 0)
					list.set(len / 2, value);
				return;
			case DynamicSubscript.LAST:
				if (len > 0)
					list.set(len - 1, value);
				return;
			case DynamicSubscript.ALL: {
				if (!(value instanceof Collection))
					throw new OgnlException("Value must be a collection");
				list.clear();
				list.addAll((Collection) value);
				return;
			}
			}
		}

		throw new NoSuchPropertyException(target, name);
	}

	public Class getPropertyClass(OgnlContext context, Object target, Object index) {
		if (index instanceof String) {
			String key = ((String) index).replaceAll("\"", "");
			if (key.equals("size")) {
				return int.class;
			} else {
				if (key.equals("iterator")) {
					return Iterator.class;
				} else {
					if (key.equals("isEmpty") || key.equals("empty")) {
						return boolean.class;
					} else {
						return super.getPropertyClass(context, target, index);
					}
				}
			}
		}

		if (index instanceof Number)
			return Object.class;

		return null;
	}

	public String getSourceAccessor(OgnlContext context, Object target, Object index) {
		String indexStr = index.toString().replaceAll("\"", "");

		if (String.class.isInstance(index)) {
			if (indexStr.equals("size")) {
				context.setCurrentAccessor(List.class);
				context.setCurrentType(int.class);
				return ".size()";
			} else {
				if (indexStr.equals("iterator")) {
					context.setCurrentAccessor(List.class);
					context.setCurrentType(Iterator.class);
					return ".iterator()";
				} else {
					if (indexStr.equals("isEmpty") || indexStr.equals("empty")) {
						context.setCurrentAccessor(List.class);
						context.setCurrentType(boolean.class);
						return ".isEmpty()";
					}
				}
			}
		}

		// TODO: This feels really inefficient, must be some better way
		// check if the index string represents a method on a custom class implementing
		// java.util.List instead..

		if (context.getCurrentObject() != null
				&& !Number.class.isInstance(context.getCurrentObject())) {
			try {
				Method m = OgnlRuntime.getReadMethod(target.getClass(), indexStr);

				if (m != null)
					return super.getSourceAccessor(context, target, index);

			} catch (Throwable t) {
				throw OgnlOps.castToRuntime(t);
			}
		}

		context.setCurrentAccessor(List.class);

		// need to convert to primitive for list index access
		// System.out.println("Curent type: " + context.getCurrentType() + " current object type " +
		// context.getCurrentObject().getClass());

		if (!context.getCurrentType().isPrimitive()
				&& Number.class.isAssignableFrom(context.getCurrentType())) {
			indexStr += "." + OgnlRuntime.getNumericValueGetter(context.getCurrentType());
		} else if (context.getCurrentObject() != null
				&& Number.class.isAssignableFrom(context.getCurrentObject().getClass())
				&& !context.getCurrentType().isPrimitive()) {
			// means it needs to be cast first as well

			String toString = String.class.isInstance(index)
					&& context.getCurrentType() != Object.class ? "" : ".toString()";

			indexStr = "ognl.OgnlOps#getIntValue(" + indexStr + toString + ")";
		}

		context.setCurrentType(Object.class);

		return ".get(" + indexStr + ")";
	}

	public String getSourceSetter(OgnlContext context, Object target, Object index) {
		String indexStr = index.toString().replaceAll("\"", "");

		// TODO: This feels really inefficient, must be some better way
		// check if the index string represents a method on a custom class implementing
		// java.util.List instead..
		/*
		 * System.out.println("Listpropertyaccessor setter using index: " + index +
		 * " and current object: " + context.getCurrentObject() + " number is current object? " +
		 * Number.class.isInstance(context.getCurrentObject()));
		 */

		if (context.getCurrentObject() != null
				&& !Number.class.isInstance(context.getCurrentObject())) {
			try {
				Method m = OgnlRuntime.getWriteMethod(target.getClass(), indexStr);

				if (m != null || !context.getCurrentType().isPrimitive()) {
					System.out.println("super source setter returned: "
							+ super.getSourceSetter(context, target, index));
					return super.getSourceSetter(context, target, index);
				}

			} catch (Throwable t) {
				throw OgnlOps.castToRuntime(t);
			}
		}

		/*
		 * if (String.class.isInstance(index)) { context.setCurrentAccessor(List.class); return "";
		 * }
		 */

		context.setCurrentAccessor(List.class);

		// need to convert to primitive for list index access

		if (!context.getCurrentType().isPrimitive()
				&& Number.class.isAssignableFrom(context.getCurrentType())) {
			indexStr += "." + OgnlRuntime.getNumericValueGetter(context.getCurrentType());
		} else if (context.getCurrentObject() != null
				&& Number.class.isAssignableFrom(context.getCurrentObject().getClass())
				&& !context.getCurrentType().isPrimitive()) {
			// means it needs to be cast first as well

			String toString = String.class.isInstance(index)
					&& context.getCurrentType() != Object.class ? "" : ".toString()";

			indexStr = "ognl.OgnlOps#getIntValue(" + indexStr + toString + ")";
		}

		context.setCurrentType(Object.class);

		return ".set(" + indexStr + ", $3)";
	}

	private Object createNewInstance(Class<?> type, String fieldname) {
		try {
			Field listField = type.getDeclaredField(fieldname);
			ParameterizedType listType = (ParameterizedType) listField.getGenericType();
			Class<?> clazz = (Class<?>) listType.getActualTypeArguments()[0];
			return clazz.newInstance();
		} catch (NoSuchFieldException | SecurityException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
