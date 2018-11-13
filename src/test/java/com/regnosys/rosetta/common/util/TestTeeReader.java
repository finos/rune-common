package com.regnosys.rosetta.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.common.util.TeeInputStream.TeedInputStream;
import com.regnosys.rosetta.common.util.TeeReader.TeedReader;

class TestTeeReader {


	@Test
	void test() throws IOException, InterruptedException, ExecutionException {
		String s = "The quick etc. ££";
		byte[] b = s.getBytes();
		ByteArrayInputStream bin = new ByteArrayInputStream(b);
		TeeReader tin = new TeeReader(new InputStreamReader(bin));
		TeedReader[] splitInto = tin.splitInto(2);
		BufferedReader reader1 = new BufferedReader(splitInto[0]);
		BufferedReader reader2 = new BufferedReader(splitInto[1]);
		
		ExecutorService service = Executors.newFixedThreadPool(2);
		Future<String> submit1 = service.submit(()->reader1.readLine());
		Future<String> submit2 = service.submit(()->reader2.readLine());
		
		assertEquals(s,  submit1.get());
		assertEquals(s,  submit2.get());
	}
	
	@Test
	void test2() throws IOException, InterruptedException, ExecutionException {
		String s = "The quick etc. ££adhfglkhdbljkahsdghfkhsdlkjhvkljashgdfkbmgbzlhlksalkdrljkajhghlkhjhxzkfgtkjhLAZLSUIHJKLYRTHJHJK ASDLFGT ;DXCJK.";
		StringBuilder builder = new StringBuilder();
		for (int i=0;i<100;i++) {
			builder.append(s);
		}
		String s2=builder.toString();
		byte[] b = s2.getBytes();
		ByteArrayInputStream bin = new ByteArrayInputStream(b);
		TeeReader tin = new TeeReader(new InputStreamReader(bin));
		TeedReader[] splitInto = tin.splitInto(2);
		BufferedReader reader1 = new BufferedReader(splitInto[0]);
		BufferedReader reader2 = new BufferedReader(splitInto[1]);
		
		ExecutorService service = Executors.newFixedThreadPool(2);
		Future<String> submit1 = service.submit(()->reader1.readLine());
		Future<String> submit2 = service.submit(()->reader2.readLine());
		
		assertEquals(submit1.get(), submit2.get());
		assertEquals(100*s.length(), submit2.get().length());
	}

}
