/**
 * 
 */
package org.ejs.eulang.ast.impl;

import org.ejs.eulang.TypeEngine;
import org.ejs.eulang.ast.IAstDefineStmt;
import org.ejs.eulang.ast.IAstDerefExpr;
import org.ejs.eulang.ast.IAstFieldExpr;
import org.ejs.eulang.ast.IAstName;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstSymbolExpr;
import org.ejs.eulang.ast.IAstTypedExpr;
import org.ejs.eulang.types.BaseLLField;
import org.ejs.eulang.types.LLDataType;
import org.ejs.eulang.types.LLInstanceType;
import org.ejs.eulang.types.LLSymbolType;
import org.ejs.eulang.types.LLType;
import org.ejs.eulang.types.TypeException;

/**
 * @author ejs
 *
 */
public class AstFieldExpr extends AstTypedExpr implements IAstFieldExpr {

	private IAstName field;
	private IAstTypedExpr expr;

	/**
	 * @param expr
	 * @param field
	 */
	public AstFieldExpr(IAstTypedExpr expr, IAstName field) {
		setExpr(expr);
		setField(field);
	}

	 
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expr == null) ? 0 : expr.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AstFieldExpr other = (AstFieldExpr) obj;
		if (expr == null) {
			if (other.expr != null)
				return false;
		} else if (!expr.equals(other.expr))
			return false;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstTypedNode#toString()
	 */
	@Override
	public String toString() {
		return typedString("FIELD '" + field +"'");
	}
	
	/**
	 * @param field
	 */
	@Override
	public void setField(IAstName field) {
		this.field = reparent(this.field, field);
		field.setScope(null);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstFieldExpr#copy(org.ejs.eulang.ast.IAstNode)
	 */
	@Override
	public IAstFieldExpr copy(IAstNode parent) {
		return fixup(this, new AstFieldExpr(doCopy(expr, parent), doCopy(field, parent)));
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstFieldExpr#getExpr()
	 */
	@Override
	public IAstTypedExpr getExpr() {
		return expr;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstFieldExpr#getField()
	 */
	@Override
	public IAstName getField() {
		return field;
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstFieldExpr#setExpr(org.ejs.eulang.ast.IAstTypedExpr)
	 */
	@Override
	public void setExpr(IAstTypedExpr expr) {
		this.expr = reparent(this.expr, expr);
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#getChildren()
	 */
	@Override
	public IAstNode[] getChildren() {
		if (expr != null)
			return new IAstNode[] { expr, field };
		else
			return new IAstNode[] { field };
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstNode#getDumpChildren()
	 */
	@Override
	public IAstNode[] getDumpChildren() {
		if (expr != null)
			return new IAstNode[] { expr };
		else
			return NO_CHILDREN;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#replaceChild(org.ejs.eulang.ast.IAstNode, org.ejs.eulang.ast.IAstNode)
	 */
	@Override
	public void replaceChild(IAstNode existing, IAstNode another) {
		if (existing == expr)
			setExpr((IAstTypedExpr) another);
		else if (existing == field)
			setField((IAstName) another);
		else
			throw new IllegalArgumentException();
	}
	

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstTypedNode#inferTypeFromChildren(org.ejs.eulang.TypeEngine)
	 */
	@Override
	public boolean inferTypeFromChildren(TypeEngine typeEngine)
			throws TypeException {
		boolean changed = false;
		
		LLDataType dataType = null;
		LLType fieldType = type;
		
		if (canInferTypeFrom(expr)) {
			LLType exprType = getDataType(typeEngine);
			
			if (exprType == null || exprType instanceof LLInstanceType)
				return false;
			
			if (!(exprType instanceof LLDataType)) {
				throw new TypeException(expr, "can only field-dereference data");
			}
			dataType = (LLDataType) exprType;
			BaseLLField field = dataType.getField(this.field.getName());
			if (field == null)
				throw new TypeException(this.field, "no field '"+ this.field.getName() + "' in data '" + dataType.getName() + "'");
			
			int fieldIdx = dataType.getFieldIndex(field);
			fieldType = field.getType();
			
			//fieldType = fieldType.substitute(typeEngine, this, fieldType);
			
			changed |= updateType(this, fieldType);
			
			if (dataType != null && (fieldType == null || !fieldType.isComplete())) {
				if (canInferTypeFrom(this)) {
					LLType[] fieldTypes = dataType.getTypes();
					fieldTypes[fieldIdx] = getType();
					
					dataType = (LLDataType) dataType.updateTypes(typeEngine, fieldTypes);
					changed |= updateType(expr, dataType);
				}
			}
		}
		
    	if (dataType == null || !dataType.isComplete()) {
    		// cannot infer the expr type from the name (yet)
    	} 
		

		return changed;
	}

	/**
	 * @param type
	 * @return
	 */
	public LLType getDataType(TypeEngine typeEngine) {
		LLType exprType = expr.getType();
		if (exprType instanceof LLInstanceType) {
			//LLType realType = typeEngine.getInstanceToRealTypeMap().get(exprType);
			//if (realType != null)
			//	return realType;
			exprType = ((LLInstanceType) exprType).getSymbol().getType();
		}
		if (exprType instanceof LLSymbolType) {
			exprType = ((LLSymbolType) exprType).getRealType(typeEngine);
		}
		
		if (exprType instanceof LLDataType) {
			exprType = typeEngine.getDataType((LLDataType) exprType);
		}
		return exprType;
	}


	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.impl.AstNode#validateChildTypes(org.ejs.eulang.TypeEngine)
	 */
	@Override
	public void validateChildTypes(TypeEngine typeEngine) throws TypeException {
		if (expr == null)
			return;
		
		IAstTypedExpr theRef = expr;
		if (theRef instanceof IAstDerefExpr)
			theRef = ((IAstDerefExpr) theRef).getExpr();
		if (theRef instanceof IAstSymbolExpr) {
			IAstDefineStmt definition = ((IAstSymbolExpr) theRef).getDefinition();
			if (definition != null)
				throw new TypeException(theRef, "cannot reference fields in definitions (use an instance instead)");
		}
		LLType exprType = getDataType(typeEngine);
		if (!(exprType instanceof LLDataType)) {
			throw new TypeException(expr, "can only field-dereference data");
		}
		LLDataType dataType = (LLDataType) exprType;
		BaseLLField field = dataType.getField(this.field.getName());
		if (field == null)
			throw new TypeException(this.field, "no field '"+ this.field.getName() + "' in data '" + dataType.getName() + "'");
		
		LLType fieldType = field.getType();
		if (fieldType != null && fieldType.isComplete()) {
			// account for up-types
			if (!typeEngine.getBaseType(type).matchesExactly(typeEngine.getBaseType(fieldType))) {
				throw new TypeException(this.field, "field's type does not match parent");
			}
		}
	}

}
