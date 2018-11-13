package com.regnosys.rosetta.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Phaser;

public class TeeInputStream {
	
	private InputStream in;
	
	byte[] buffer = new byte[1024];
	int endPointer;
	boolean ended=false;
	
	Phaser barrier;
	
	int brokenCount;
	
	
	TeedInputStream[] tees;


	public TeeInputStream(InputStream in) {
		this.in = in;
		readMoreUnderlying();
	}
	
	public void readerDeath() {
		barrier.arriveAndDeregister();
	}
	
	TeedInputStream[] splitInto(int count) {
		tees = new TeedInputStream[count];
		for (int i=0;i<count;i++) {
			tees[i]=new TeedInputStream();
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
		System.out.println("Attempting to read underlying");
		try {
			if (tees!=null) {
				for (int i = 0; i < tees.length; i++) {
					tees[i].resetPointer();
				}
			}
			int read = in.read(buffer);
			if (read==-1) ended=true;
			endPointer = read;
		} catch (IOException e) {
			ended=true;
			throw new RuntimeException("Error readign bytes ", e);
		}
	}
	
	
	class TeedInputStream extends InputStream {

		volatile int pointer;
		
		@Override
		public int read() throws IOException {
			if (pointer<endPointer) {
				return buffer[pointer++] & 0xFF;
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
		
	}
}
