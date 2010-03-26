/**
 * 
 */
package v9t9.tools.ast.expr;


/**
 * A scope defining names.  These are nested hierarchically and 
 * searched from inside out.
 * 
 * @author eswartz
 *
 */
public interface IScope {
    /** Get the owner of the scope (or null); this is, e.g.,
     * an IAstEnumDeclaration, IAstCompositeTypeSpecifier, etc. */
    public IAstNode getOwner();
    
    /** Set the owner of the scope (or null); this is, e.g.,
     * an IAstEnumDeclaration, IAstCompositeTypeSpecifier, etc. */
    public void setOwner(IAstNode owner);
    
    /** Get the name of the scope (or null) */
    public IAstName getScopeName();
    
    /** Set the name of the scope (or null) */
    public void setScopeName(IAstName name);
    
    /** Get the parent scope (or null) */
    public IScope getParent();
    
    /** Set the parent scope (or null) */
    public void setParent(IScope parent);
    
    /** Look up a name in this scope */
    public IAstName find(String name);

    /**
     * Look up a name in any scope visible from this scope,
     * starting from this one and going up the parent chain
     */
    public IAstName search(String name);
    
    /** Look up a named node by name in this scope */
    public IAstNode find(IAstName name);
    
    /**
     * Look up a named node in any scope visible from this scope,
     * starting from this one and going up the parent chain
     */
    public IAstNode search(IAstName name);

    /** Add a name to the scope.  Sets name's scope to this
     * and the name's parent to node.
     * @param name the name to add.  Current scope must be null.  
     * @param node the node associated with the name
     * */
    public void add(IAstName name, IAstNode node);

    /** Remove a name to the scope.  Sets name's scope to null.
     * @param name the name to remove.  Current scope must be this.  */
    public void remove(IAstName name);

	public IAstName[] getNames();
	public IAstNode[] getNodes();
}
