package com.rychu.tagtracker.processing;

import java.io.Serializable;
import java.util.ArrayList;

import android.util.Log;






public class LoadBalancer implements Runnable{
	public class InvalidStateException extends Exception{
		private static final long serialVersionUID = 1L;
		public static final int TYPE_SET_SIZE_TOO_SMALL = 1;
		public static final int TYPE_SET_SIZE_TOO_BIG = 2;
		public int type;
		public InvalidStateException(int type) {
			this.type = type;
		}
	}
	public interface Task extends Serializable{
		void work();
	}
	private class Worker implements Runnable{
		final static int WORKER_STATE_ANY = 666;
		final static int WORKER_STATE_EMPTY = 0;
		final static int WORKER_STATE_FULL = 1;
		final static int WORKER_STATE_WORKING = 2;
		final static int WORKER_STATE_LOCKED = 2;
		public int state;
		public Task task;
		private long addToQueueTimestamp = 0;
		protected long retrieveFromQueueTime = 0;
		protected long processTime = 0;
		protected Thread t;
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
			if(task != null && work){
				retrieveFromQueueTime = System.currentTimeMillis() - addToQueueTimestamp;
				task.work();
				task = null;
				processTime = System.currentTimeMillis() - addToQueueTimestamp;
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
	private int currentPoolIndex = 0;
	private Boolean work = false;
	private ArrayList<Worker> pool;
	private int flag[] = {};
	private Boolean waiting = false;
	private Thread balancer;

	

	private long retrieveFromQueueTimeStatus = 0;
	private long processTimeStatus = 0;
	
	private Boolean monitorEnabled = false;
	public LoadBalancer() {
		balancer = null;
		pool = new ArrayList<LoadBalancer.Worker>();
		// TODO Auto-generated constructor stub
	}
	private Worker getNextWorker(int state){
		Worker worker = null;
		if(pool.size() < 1){
			return worker;
		}
		do{
			for(int c=0;c<maxPoolSize;c++){
				int pos = (c+currentPoolIndex)%maxPoolSize;
				try{
					Worker w = pool.get(pos);
					if(state == Worker.WORKER_STATE_ANY){
						currentPoolIndex = pos;
						return w;
					}else if(w.state == state){
						w.state = Worker.WORKER_STATE_LOCKED;
						if(state == Worker.WORKER_STATE_FULL && monitorEnabled){
							//monitor.setWaitingProcessingTime(processTimeStatus);
							//monitor.setWaitingTime(retrieveFromQueueTimeStatus);
						}
						processTimeStatus = w.processTime;
						currentPoolIndex = pos;
						worker = w;
						break;
					}
				}catch(IndexOutOfBoundsException e){
					
					e.printStackTrace();
					Log.w("getNextWorker()", "Unable to get the worker in pos="+pos);
				}
			}
			if(worker == null){
				try {
					synchronized(flag){
						if(work)flag.wait();
					}
				} catch (InterruptedException e) {}
			}
		}while(worker == null && work);
		return worker;
	}
	public void setNextTaskIfEmpty(Task task) throws NullPointerException, IndexOutOfBoundsException{
		Boolean changed = false;
		if(pool.size() > 0){
			for(int c=0;c<pool.size();c++){
				int pos = (c+currentPoolIndex)%maxPoolSize;
				try{
					Worker w = pool.get(pos);
					if(w.state == Worker.WORKER_STATE_EMPTY){
						currentPoolIndex = pos;
						w.setTask(task);
						changed = true;
						break;
					}
				}catch(IndexOutOfBoundsException e){
					throw new IndexOutOfBoundsException("Unable to get the worker in pos="+pos);
				}
			}
		}
		synchronized(flag) {
			flag.notify();
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
		maxPoolSize = 0;
		for(Worker work : pool){
			try {
				work.t.join();
			} catch (Exception e) {}
		}
		pool.clear();
		balancer.join(100);
		balancer = null;
	}
	public void setMaxPoolSize(int size) throws InvalidStateException{
		if(size < 0){
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
			while(work && pool.size() < maxPoolSize){
				w = new Worker();
				pool.add(w);
				try{
					flag.notify();
				}catch(Exception e){}
			}
			while(pool.size() > maxPoolSize){
				w = getNextWorker(Worker.WORKER_STATE_EMPTY);
				pool.remove(w);
				
			}
			while(work && maxPoolSize > 0 && currentThreadsActive < maxThreadsActive){
				currentThreadsActive++;
				w = getNextWorker(Worker.WORKER_STATE_FULL);
				if(w!=null){
					w.t = new Thread(w);
					w.t.setDaemon(true);
					w.t.setPriority(Thread.MAX_PRIORITY-2);
					w.t.start();
					//Log.v("run()", "worker started!");
				}
			}
			try {
				synchronized(flag){
					if(work){
						flag.wait();
					}
				}
			} catch (InterruptedException e) {}
			if(!work){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
		}
		
		
	}
}
