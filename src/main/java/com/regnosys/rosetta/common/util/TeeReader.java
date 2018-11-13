package com.regnosys.rosetta.common.util;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.Phaser;

public class TeeReader {
	
	private Reader in;
	
	private char[] buffer = new char[1024];
	private int endPointer;
	private boolean ended=false;
	
	private Phaser barrier;
	
	private TeedReader[] tees;


	public TeeReader(Reader in) {
		this.in = in;
		readMoreUnderlying();
	}
	
	public void readerDeath() throws IOException {
		barrier.arriveAndDeregister();
		if (barrier.getRegisteredParties()==0) {
			in.close();
		}
	}
	
	public Reader[] splitInto(int count) {
		tees = new TeedReader[count];
		for (int i=0;i<count;i++) {
			tees[i]=new TeedReader();
		}
		barrier = new Phaser(count) {

			@Override
			protected boolean onAdvance(int phase, int registeredParties) {
				readMoreUnderlying();
				return super.onAdvance(phase, registeredParties);
			}
			
		};
		return tees;
	}
	
	private void readMoreUnderlying() {
		try {
			if (tees!=null) {
				for (int i = 0; i < tees.length; i++) {
					tees[i].resetPointer();
				}
			}
			int read = in.read(buffer);
			if (read==-1) {
				ended=true;
			}
			endPointer = read;
		} catch (IOException e) {
			ended=true;
			throw new RuntimeException("Error reading chars ", e);
		}
	}
	
	private class TeedReader extends Reader {

		private volatile int pointer;
		
		@Override
		public int read() throws IOException {
			if (pointer<endPointer) {
				return buffer[pointer++];
			}
			else {
				boolean sucess=readMore();
				if (sucess)
					return read();
				return -1;
			}
		}
		
		private boolean readMore() {
			barrier.arriveAndAwaitAdvance();
			return !ended;
		}

		private void resetPointer() {
			pointer=0;
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			int toRead = Math.min(len,  endPointer-pointer);
			if (toRead>0) {
				System.arraycopy(buffer, pointer, cbuf, off, toRead);
				pointer+=toRead;
				return toRead;
			}
			else {
				boolean sucess=readMore();
				if (sucess) {
					return read(cbuf, off, len);
				}
			}
			return -1;
		}

		@Override
		public void close() throws IOException {
			readerDeath();
		}
		
	}
}
