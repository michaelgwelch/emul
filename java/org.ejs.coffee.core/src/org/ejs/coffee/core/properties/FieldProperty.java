/**
 * 
 */
package org.ejs.coffee.core.properties;

import java.lang.reflect.Field;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * @author ejs
 *
 */
public class FieldProperty extends AbstractProperty {

	
	protected final String fieldName;
	protected final Object obj;
	protected Field field;

	public FieldProperty(IClassPropertyFactory factory, Object obj, String fieldName, String name, IPropertyEditorProvider editorProvider) {
		super(factory, null, name, editorProvider != null ? editorProvider : new FieldPropertyEditorProvider());
		this.obj = obj;
		this.fieldName = fieldName;
		Class<?> klass = obj.getClass();
		while (klass != null) {
			try {
				field = klass.getDeclaredField(fieldName);
				field.setAccessible(true);
				setType(field.getType());
				return;
			} catch (NoSuchFieldException e) {
				klass = klass.getSuperclass();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		throw new IllegalArgumentException(fieldName);
	}

	

	public FieldProperty(IClassPropertyFactory factory, Object obj, String fieldName) {
		this(factory, obj, fieldName, fieldName);
	}
	
	/**
	 * @param name
	 */
	public FieldProperty(Object obj, String fieldName, String name, IPropertyEditorProvider editor) {
		this(null, obj, fieldName, name, editor);
	}
	public FieldProperty(Object obj, String fieldName, String name) {
		this(obj, fieldName, name, null);
	}	
	public FieldProperty(Object obj, String fieldName) {
		this(obj, fieldName, fieldName, null);
	}	
	
	public FieldProperty(IClassPropertyFactory factory, Object obj, String fieldName, String name) {
		this(factory, obj, fieldName, name, null);
		
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result
				+ ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result + ((obj == null) ? 0 : obj.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FieldProperty other = (FieldProperty) obj;
		if (field == null) {
			if (other.field != null) {
				return false;
			}
		} else if (!field.equals(other.field)) {
			return false;
		}
		if (fieldName == null) {
			if (other.fieldName != null) {
				return false;
			}
		} else if (!fieldName.equals(other.fieldName)) {
			return false;
		}
		if (this.obj == null) {
			if (other.obj != null) {
				return false;
			}
		} else if (!this.obj.equals(other.obj)) {
			return false;
		}
		return true;
	}



	/* (non-Javadoc)
	 * @see org.ejs.chiprocksynth.model.ICellEditorProvider#createCellEditor(org.eclipse.swt.widgets.Composite)
	 */
	public CellEditor createCellEditor(Composite composite) {
		return new TextCellEditor(composite);
	}
	
	protected FieldProperty getProperty() { return this; }
	
	/* (non-Javadoc)
	 * @see org.ejs.chiprocksynth.editor.model.AbstractProperty#getPersistedName()
	 */
	@Override
	public String getName() {
		return fieldName;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.chiprocksynth.editor.model.IProperty#getValue()
	 */
	public Object getValue() {
		return FieldUtils.getValue(field, obj);
	}


	/* (non-Javadoc)
	 * @see org.ejs.chiprocksynth.editor.model.IProperty#setValue(java.lang.Object)
	 */
	public void setValue(Object value) {
		FieldUtils.setValue(field, obj, value);
		firePropertyChange();
	}
	
	/**
	 * @param txt
	 */
	public void setValueFromString(String txt) {
		FieldUtils.setValueFromString(field, obj, txt);		
		firePropertyChange();
	}



	/**
	 * @return
	 */
	public Field getField() {
		return field;
	}
	/**
	 * @return
	 */
	public Object getObject() {
		return obj;
	}


}
