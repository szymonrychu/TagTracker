package org.opencv.android.local;

import java.util.ArrayList;

import android.os.Binder;


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
		long addToQueueTimestamp = 0;
		long getFromQueueTimestamp = 0;
		long processingTimestamp = 0;
		public Boolean reuse = true;
		public Task task;
		public void setTask(Task task){
			reuse = false;
			addToQueueTimestamp = System.currentTimeMillis();
			this.task = task;
		}
		@Override
		public void run() {
			if(task != null){
				getFromQueueTimestamp = System.currentTimeMillis();
				task.work();
				processingTimestamp = System.currentTimeMillis();
			}
			reuse = true;
			currentThreadsActive--;
			synchronized(flag) {
				flag.notifyAll();
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
	private Worker getNextWorker(){
		Worker worker = null;
		if(currentPoolSize < 1){
			return worker;
		}
		do{
			for(int c=0;c<maxPoolSize;c++){
				int pos = (c+currentPoolIndex)%maxPoolSize;
				Worker w = pool.get(pos);
				if(w.reuse){
					worker = w;
					currentPoolIndex = pos;
					break;
				}
			}
			if(worker == null){
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
		synchronized(flag) {
			flag.notifyAll();
		}
		for(int c=0;c<maxPoolSize;c++){
			int pos = (c+currentPoolIndex)%maxPoolSize;
			Worker w = pool.get(pos);
			if(w.reuse){
				w.setTask(task);
				currentPoolIndex = pos;
				return;
			}
		}
	}
	public void setNextEmptyTask(Task task) throws NullPointerException, IndexOutOfBoundsException{
		synchronized(flag) {
			flag.notifyAll();
		}
		Worker w = getNextWorker();
		if(w != null){
			w.setTask(task);
		}else{
			throw new NullPointerException();
		}
	}
	public void startWorking() throws InterruptedException{
		work = true;
		if(balancer != null){
			stopWorking();
		}
		balancer = new Thread(this);
		balancer.start();
	}
	public void stopWorking() throws InterruptedException{
		work = false;
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
		while(work){
			while(currentPoolSize < maxPoolSize){
				Worker task = new Worker();
				pool.add(task);
				currentPoolSize++;
			}
			Worker w = null;
			while(currentPoolSize > maxPoolSize){
				w = getNextWorker();
				pool.remove(w);
				currentPoolSize--;
			}
			while(currentThreadsActive < maxThreadsActive){
				currentThreadsActive++;
				w = getNextWorker();
				Thread t = new Thread(w);
				t.start();
			}
			try {
				synchronized(flag){
					flag.wait();
				}
			} catch (InterruptedException e) {}
		}
	}
}
