package com.richert.tagtracker.processing;

import java.util.ArrayList;

import com.richert.tagtracker.monitor.MonitoringService.MonitoringBinder;

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
	public void setMonitoringCallback(MonitoringBinder monitor){
		this.monitor = monitor;
		this.monitorEnabled = true;
	}
	public void unsetMonitoringCallback(){
		this.monitor = null;
		this.monitorEnabled = false;
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
			if(task != null){
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
	private int currentPoolSize = 0;
	private int currentPoolIndex = 0;
	private Boolean work = false;
	private ArrayList<Worker> pool;
	private int flag[] = {};
	private Thread balancer;
	private MonitoringBinder monitor;

	

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
		if(currentPoolSize < 1){
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
							monitor.setWaitingProcessingTime(processTimeStatus);
							monitor.setWaitingTime(retrieveFromQueueTimeStatus);
						}
						processTimeStatus = w.processTime;
						currentPoolIndex = pos;
						worker = w;
						break;
					}
				}catch(IndexOutOfBoundsException e){
					Log.w("getNextWorker()", "Unable to get the worker in pos="+pos);
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
			while(work && currentPoolSize < maxPoolSize){
				Worker task = new Worker();
				pool.add(task);
				currentPoolSize++;
				try{
					flag.notify();
				}catch(Exception e){}
			}
			while(work && currentPoolSize > maxPoolSize){
				w = getNextWorker(Worker.WORKER_STATE_EMPTY);
				pool.remove(w);
				currentPoolSize--;
			}
			while(work && currentThreadsActive < maxThreadsActive){
				currentThreadsActive++;
				w = getNextWorker(Worker.WORKER_STATE_FULL);
				if(w!=null){
					w.t = new Thread(w);
					w.t.setPriority(Thread.MAX_PRIORITY-2);
					w.t.start();
				}
			}if(work)try {
				synchronized(flag){
					flag.wait();
				}
			} catch (InterruptedException e) {}
		}
		for(Worker work : pool){
			try {
				work.t.join();
			} catch (Exception e) {}
		}
		pool.clear();
	}
}
