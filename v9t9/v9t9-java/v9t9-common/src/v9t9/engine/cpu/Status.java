package v9t9.engine.cpu;

public interface Status {

	String toString();

	void copyTo(Status copy);

	short flatten();

	void expand(short stat);

	/** is arith greater */
	boolean isLT();

	/** is logical lower or equal */
	boolean isLE();

	/** is logical lower */
	boolean isL();

	/** is equal */
	boolean isEQ();

	/** is not equal */
	boolean isNE();

	/** is logical higher or equal */
	boolean isHE();

	/** is arith greater or equal */
	boolean isGT();

	/** is logical higher */
	boolean isH();

	/** is carry */
	boolean isC();

	int getIntMask();

}