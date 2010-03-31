/**
 * 
 */
package org.ejs.eulang.ast.impl;

import org.ejs.eulang.ast.IAstLitExpr;
import org.ejs.eulang.ast.IAstNode;
import org.ejs.eulang.ast.IAstSymbolExpr;
import org.ejs.eulang.ast.IAstType;
import org.ejs.eulang.ast.IAstTypedExpr;
import org.ejs.eulang.ast.TypeEngine;
import org.ejs.eulang.types.LLType;
import org.ejs.eulang.types.TypeException;


/**
 * @author ejs
 *
 */
public abstract class AstLitExpr extends AstTypedExpr implements
		IAstLitExpr, IAstTypedExpr {

	private String lit;
	public AstLitExpr(String lit, LLType type) {
		this.lit = lit;
		this.type = type;
		
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstLiteralExpression#getLiteral()
	 */
	@Override
	public String getLiteral() {
		return lit;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.tools.ast.expr.IAstExpression#equalValue(v9t9.tools.ast.expr.IAstExpression)
	 */
	@Override
	public boolean equalValue(IAstTypedExpr expr) {
		return expr instanceof IAstLitExpr && ((IAstLitExpr) expr).getLiteral().equals(lit);
	}

	/* (non-Javadoc)
	 * @see v9t9.tools.ast.expr.IAstNode#getChildren()
	 */
	@Override 
	public IAstNode[] getChildren() {
		return NO_CHILDREN;
	}
	
	@Override
	public void replaceChildren(IAstNode[] children) {
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstNode#replaceChildren(org.ejs.eulang.ast.IAstNode[])
	 */
	@Override
	public void replaceChild(IAstNode existing, IAstNode another) {
		throw new IllegalArgumentException();
	}

	/* (non-Javadoc)
	 * @see org.ejs.eulang.ast.IAstTypedNode#inferTypeFromChildren(org.ejs.eulang.ast.TypeEngine)
	 */
	@Override
	public boolean inferTypeFromChildren(TypeEngine typeEngine)
			throws TypeException {
		return false;
	}
	
}
