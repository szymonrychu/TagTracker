package com.richert.tagtracker.processing;

import java.util.ArrayList;

import android.os.Binder;
import android.util.Log;


public class LoadBalancer implements Runnable{
	public class InvalidStateException extends Exception{
		private static final long serialVersionUID = 1L;
		public static final int TYPE_SET_SIZE_TOO_SMALL = 1;
		public static final int TYPE_SET_SIZE_TOO_BIG = 2;
		int type;
		public InvalidStateException(int type) {
			this.type = type;
		}
	}
	public interface Task{
		void work();
	}
	private class Worker implements Runnable{
		final static int WORKER_STATE_EMPTY = 0;
		final static int WORKER_STATE_FULL = 1;
		final static int WORKER_STATE_WORKING = 2;
		final static int WORKER_STATE_LOCKED = 2;
		long addToQueueTimestamp = 0;
		long getFromQueueTimestamp = 0;
		long processingTimestamp = 0;
		public int state;
		public Task task;
		public Worker() {
			state = WORKER_STATE_EMPTY;
		}
		public void setTask(Task task){
			state = WORKER_STATE_FULL;
			addToQueueTimestamp = System.currentTimeMillis();
			this.task = task;
		}
		@Override
		public void run() {
			state = WORKER_STATE_WORKING;
			if(task != null){
				getFromQueueTimestamp = System.currentTimeMillis();
				task.work();
				task = null;
				processingTimestamp = System.currentTimeMillis();
			}
			currentThreadsActive--;
			state = WORKER_STATE_EMPTY;
			synchronized(flag) {
				flag.notify();
			}
		}
	}
	private int maxThreadsActive = 1;
	private int currentThreadsActive = 0;
	
	private int maxPoolSize = 0;
	private int currentPoolSize = 0;
	private int currentPoolIndex = 0;
	private Boolean work = false;
	private ArrayList<Worker> pool;
	private int flag[] = {};
	private Thread balancer;
	public LoadBalancer() {
		balancer = null;
		pool = new ArrayList<LoadBalancer.Worker>();
		// TODO Auto-generated constructor stub
	}
	private Worker getNextWorker(int state){
		Worker worker = null;
		if(currentPoolSize < 1){
			return worker;
		}
		do{
			for(int c=0;c<maxPoolSize;c++){
				int pos = (c+currentPoolIndex)%maxPoolSize;
				Worker w = pool.get(pos);
				if(w.state == state){
					w.state = Worker.WORKER_STATE_LOCKED;
					currentPoolIndex = pos;
					worker = w;
					break;
				}
			}
			if(!work){
				return null;
			}else if(worker == null){
				try {
					synchronized(flag){
						flag.wait();
					}
				} catch (InterruptedException e) {}
			}
		}while(worker == null);
		return worker;
	}
	public void setNextTaskIfEmpty(Task task) throws NullPointerException, IndexOutOfBoundsException{
		Boolean changed = false;
		for(int c=0;c<maxPoolSize;c++){
			int pos = (c+currentPoolIndex)%maxPoolSize;
			Worker w = pool.get(pos);
			if(w.state == Worker.WORKER_STATE_EMPTY){
				currentPoolIndex = pos;
				w.setTask(task);
				changed = true;
				break;
			}
		}
		if(changed){
			synchronized(flag) {
				flag.notify();
			}
		}
	}
	public void startWorking() throws InterruptedException{
		if(balancer != null){
			stopWorking();
		}
		work = true;
		balancer = new Thread(this);
		balancer.setPriority(Thread.MAX_PRIORITY);
		balancer.start();
	}
	public void stopWorking() throws InterruptedException{
		work = false;
		try{
			flag.notifyAll();
		}catch(Exception e){}
		balancer.join();
		balancer = null;
	}
	public void setMaxPoolSize(int size) throws InvalidStateException{
		if(size < 1){
			throw new InvalidStateException(InvalidStateException.TYPE_SET_SIZE_TOO_SMALL);
		}
		maxPoolSize = size;
	}
	public void setMaxThreadsNum(int size) throws InvalidStateException{
		if(size < 1){
			throw new InvalidStateException(InvalidStateException.TYPE_SET_SIZE_TOO_SMALL);
		}
		if(size > maxPoolSize){
			throw new InvalidStateException(InvalidStateException.TYPE_SET_SIZE_TOO_BIG);
		}
		maxThreadsActive = size;
	}
	@Override
	public void run() {
		Worker w = null;
		while(work){
			while(currentPoolSize < maxPoolSize){
				Worker task = new Worker();
				pool.add(task);
				currentPoolSize++;
				try{
					flag.notify();
				}catch(Exception e){}
			}
			while(currentPoolSize > maxPoolSize){
				w = getNextWorker(Worker.WORKER_STATE_EMPTY);
				pool.remove(w);
				currentPoolSize--;
			}
			while(currentThreadsActive < maxThreadsActive){
				currentThreadsActive++;
				w = getNextWorker(Worker.WORKER_STATE_FULL);
				Thread t = new Thread(w);
				t.setPriority(Thread.MAX_PRIORITY-2);
				t.start();
			}if(work)try {
				synchronized(flag){
					flag.wait();
				}
			} catch (InterruptedException e) {}
		}
		pool.clear();
	}
}
