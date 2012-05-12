/**
 * 
 */
package v9t9.server.demo;

import java.io.IOException;

import v9t9.common.events.NotifyException;
import v9t9.server.demo.DemoFormat.BufferType;

public class DemoReadBuffer {
	/**
	 * 
	 */
	private final BaseReader reader;
	private byte[] buffer;
	private int length;
	private int index;

	private int startPos;
	private final String label;
	private int myType;
	
	public DemoReadBuffer(BaseReader reader, String label, BufferType myType, int size) {
		this.reader = reader;
		this.label = label;
		this.buffer = new byte[size];
		this.index = 0;
		this.length = size;
		this.myType = myType.getCode();
	}
	public int read() throws IOException, NotifyException {
		if (index >= length) {
			refill();
		}
		return buffer[index++] & 0xff; 
	}
	public void refill() throws IOException, NotifyException {
		readHeader();
		if (length > buffer.length)
			throw new NotifyException(null, "corrupt demo in " + label + " buffer at " + 
					Integer.toHexString(getEffectivePos()) + " expecting " + length);
		int read = this.reader.is.read(buffer, 0, length);
		if (read != length)
			throw new NotifyException(null, "corrupt demo in " + label + " buffer at " + 
					Integer.toHexString(getEffectivePos()));
		this.reader.isPos += read;
		length = read;
		index = 0;
	}
	protected void readHeader() throws IOException {
		startPos = this.reader.isPos;
		//System.err.println(Integer.toHexString(isPos)+": " + label + " header");
		length = (this.reader.is.read() & 0xff) | ((this.reader.is.read() & 0xff) << 8);
		this.reader.isPos += 2;
	}
	
	public int getEffectivePos() {
		return startPos + index;
	}

	public byte[] readRest() {
		byte[] data = new byte[length - index];
		System.arraycopy(buffer, index, data, 0, length - index);
		index = length;
		return data;
	}

	public int readWord() throws IOException, NotifyException {
		int word = (read() & 0xff) | ((read() & 0xff) << 8);
		return word;
	}
	/**
	 * @return
	 */
	public boolean isAvailable() {
		return index < length;
	}
	/**
	 * @param chunkLength
	 * @return
	 * @throws IOException 
	 * @throws NotifyException 
	 */
	public byte[] readData(int chunkLength) throws IOException, NotifyException {
		// all chunk should be buffered, or not
		if (index >= length) {
			int typ = this.reader.is.read();
			if (typ != myType) {
				throw new NotifyException(null, "corrupt demo buffering for " + label + " at " + Integer.toHexString(getEffectivePos()));
			}
			refill();
		}
		byte[] data = new byte[chunkLength];
		System.arraycopy(buffer, index, data, 0, chunkLength);
		index += chunkLength;
		return data;
	}
	

	/**
	 * Push a variable-length signed integer (UTF-8)
	 * 
	 * @param i
	 * @throws IOException 
	 * @throws NotifyException 
	 */
	public int readVar() throws IOException, NotifyException {
		boolean neg = false;
		
		int byt = read();
		if (byt == 0xff) {
			neg = true;
			byt = read();
		}
		
		int val;
		if (byt < 0x80) {
			// 7 bits
			val = byt;
		}
		else if ((byt & 0xe0) == 0xc0) {
			// 11 bits
			val = ((byt & 0x1f) << 6) 
					| (ensureUtf8(read()) << 0);
		}
		else if ((byt & 0xf0) == 0xe0) {
			// 16 bits
			val = ((byt & 0xf) << 12) 
					| (ensureUtf8(read()) << 6)
					| (ensureUtf8(read()) << 0);
		}
		else if ((byt & 0xf8) == 0xf0) {
			// 21 bits
			val = ((byt & 0x7) << 18) 
					| (ensureUtf8(read()) << 12)
					| (ensureUtf8(read()) << 6)
					| (ensureUtf8(read()) << 0);
		}
		else if ((byt & 0xfc) == 0xf8) {
			// 26 bits
			val = ((byt & 0x3) << 24) 
					| (ensureUtf8(read()) << 18)
					| (ensureUtf8(read()) << 12)
					| (ensureUtf8(read()) << 6)
					| (ensureUtf8(read()) << 0);
		}
		else if ((byt & 0xfe) == 0xfc) {
			// 31 bits
			val = ((byt & 0x1) << 30) 
					| (ensureUtf8(read()) << 24)
					| (ensureUtf8(read()) << 18)
					| (ensureUtf8(read()) << 12)
					| (ensureUtf8(read()) << 6)
					| (ensureUtf8(read()) << 0);
		}
		else {
			throw new NotifyException(null, "corrupt demo encoding for " + label + " at " + Integer.toHexString(getEffectivePos()));
		}
		
		return neg ? -val : val;
	}
	
	/**
	 * @param read
	 * @return
	 * @throws NotifyException 
	 */
	private int ensureUtf8(int read) throws NotifyException {
		if ((read & 0xc0) != 0x80)
			throw new NotifyException(null, "corrupt demo encoding for " + label + " at " + Integer.toHexString(getEffectivePos()));
		return read & 0x3f;
	}
}